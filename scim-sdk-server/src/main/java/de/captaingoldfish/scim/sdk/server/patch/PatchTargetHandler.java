package de.captaingoldfish.scim.sdk.server.patch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.IOException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimBooleanNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimDoubleNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimIntNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimLongNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.filter.resources.PatchFilterResolver;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 30.10.2019 - 09:07 <br>
 * <br>
 * this class will handle the patch-add operation if a target is specified <br>
 *
 * <pre>
 *    The result of the add operation depends upon what the target location
 *    indicated by "path" references:
 *
 *    o  If the target location does not exist, the attribute and value are
 *       added.
 *
 *    o  If the target location specifies a complex attribute, a set of
 *       sub-attributes SHALL be specified in the "value" parameter.
 *
 *    o  If the target location specifies a multi-valued attribute, a new
 *       value is added to the attribute.
 *
 *    o  If the target location specifies a single-valued attribute, the
 *       existing value is replaced.
 *
 *    o  If the target location specifies an attribute that does not exist
 *       (has no value), the attribute is added with the new value.
 *
 *    o  If the target location exists, the value is replaced.
 *
 *    o  If the target location already contains the value specified, no
 *       changes SHOULD be made to the resource, and a success response
 *       SHOULD be returned.  Unless other operations change the resource,
 *       this operation SHALL NOT change the modify timestamp of the
 *       resource.
 * </pre>
 */
@Slf4j
public class PatchTargetHandler extends AbstractPatch
{

  /**
   * the specified path where the value should be added
   */
  @Getter(AccessLevel.PROTECTED)
  private final AttributePathRoot path;

  /**
   * the patch operation to handle
   */
  private final PatchOp patchOp;

  /**
   * the attribute definition of the target
   */
  private SchemaAttribute schemaAttribute;

  public PatchTargetHandler(ResourceType resourceType, PatchOp patchOp, String path)
  {
    super(resourceType);
    try
    {
      this.path = RequestUtils.parsePatchPath(resourceType, path);
    }
    catch (ScimException ex)
    {
      ex.setScimType(ScimType.RFC7644.INVALID_PATH);
      throw ex;
    }
    this.patchOp = patchOp;
    this.schemaAttribute = getSchemaAttribute();
  }

  /**
   * this method will check that the referenced attribute is not a readOnly or immutable node. If the client
   * tries to process a patch operation on such an object the current value of the resource must meet the
   * requirements of RFC7644 <br>
   * <br>
   *
   * <pre>
   * If the attribute path is "readOnly" an exception should be thrown
   * If the attributes mutability is "immutable" an add operation is only allowed if the value is unassigned
   * </pre>
   */
  private void evaluatePatchPathOperation(SchemaAttribute schemaAttribute, JsonNode attribute)
  {
    if (Mutability.READ_ONLY.equals(schemaAttribute.getMutability()))
    {
      throw new BadRequestException("the attribute '" + schemaAttribute.getScimNodeName() + "' is a '"
                                    + Mutability.READ_ONLY + "' attribute and cannot be changed", null,
                                    ScimType.RFC7644.INVALID_PATH);
    }
    if (!PatchOp.REMOVE.equals(patchOp) && Mutability.IMMUTABLE.equals(schemaAttribute.getMutability())
        && attribute != null)
    {
      throw new BadRequestException("the attribute '" + schemaAttribute.getScimNodeName() + "' is '"
                                    + Mutability.IMMUTABLE + "' and is not unassigned. Current value is: "
                                    + attribute.asText(), null, ScimType.RFC7644.INVALID_PATH);
    }
  }

  /**
   * will add the specified values into the specified path
   *
   * @param resource the resource to which the values should be added
   * @param values the values that should be added into the resource
   * @return true if an effective change was made, false else
   */
  public boolean addOperationValues(ResourceNode resource, List<String> values)
  {
    validateRequest(values);
    String[] fullAttributeNames = getAttributeNames();

    String firstAttributeName = fullAttributeNames[0];
    SchemaAttribute schemaAttribute = getSchemaAttribute(firstAttributeName);
    boolean isExtension = resourceType.getSchemaExtensions()
                                      .stream()
                                      .anyMatch(ext -> ext.getSchema().equals(schemaAttribute.getResourceUri()));
    ObjectNode currentParent = resource;
    if (isExtension)
    {
      addExtensionToSchemas(resource, patchOp, schemaAttribute);
      currentParent = (ObjectNode)currentParent.get(schemaAttribute.getResourceUri());
      if (currentParent == null)
      {
        currentParent = new ScimObjectNode();
        resource.set(schemaAttribute.getResourceUri(), currentParent);
      }
    }
    JsonNode firstAttribute = getAttributeFromObject(currentParent, firstAttributeName);

    // if the attribute is null we know that this is a simple attribute
    if (firstAttribute == null && !Type.COMPLEX.equals(schemaAttribute.getType())
        || (firstAttribute != null && !firstAttribute.isArray() && !firstAttribute.isObject()))
    {
      boolean changeWasMade = handleSimpleNode(schemaAttribute, currentParent, values);
      removeExtensionIfEmpty(resource, schemaAttribute, isExtension, currentParent);
      return changeWasMade;
    }
    else if (firstAttribute != null && firstAttribute.isArray())
    {
      return handlePatchOperationOnMultiValued(resource,
                                               values,
                                               fullAttributeNames,
                                               schemaAttribute,
                                               isExtension,
                                               currentParent,
                                               firstAttribute);
    }
    else if (Type.COMPLEX.equals(schemaAttribute.getType()))
    {
      return handlePatchOperationOnComplex(resource,
                                           values,
                                           fullAttributeNames,
                                           schemaAttribute,
                                           isExtension,
                                           currentParent,
                                           firstAttribute);
    }

    return false;
  }

  /**
   * handles a single patch operation on a complex type
   *
   * @param resource the resource that is currently processed
   * @param values the values that should be added or replaced
   * @param fullAttributeNames the full attribute names from the top level to leaf level e.g. ["emails",
   *          "emails.value"]
   * @param schemaAttribute the schema attribute definition of the value that should be replaced
   * @param isExtension if this operation is executed on an extension
   * @param currentParent if {@code isExtension} is true this is the extension node otherwise this node is
   *          equals to {@code resource}
   * @param firstAttribute the attribute extracted from the {@code currentParent}
   * @return true if an effective change was made, false else
   */
  private boolean handlePatchOperationOnComplex(ResourceNode resource,
                                                List<String> values,
                                                String[] fullAttributeNames,
                                                SchemaAttribute schemaAttribute,
                                                boolean isExtension,
                                                ObjectNode currentParent,
                                                JsonNode firstAttribute)
  {
    if (PatchOp.REMOVE.equals(patchOp) && fullAttributeNames.length == 1 && path.getSubAttributeName() == null)
    {
      evaluatePatchPathOperation(schemaAttribute, firstAttribute);
      if (firstAttribute == null)
      {
        throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                      ScimType.RFC7644.NO_TARGET);
      }
      else
      {
        currentParent.remove(schemaAttribute.getName());
        removeExtensionIfEmpty(resource, schemaAttribute, isExtension, currentParent);
        return true;
      }
    }
    boolean changeMade = handleComplexAttribute(schemaAttribute, currentParent, fullAttributeNames, values);
    removeExtensionIfEmpty(resource, schemaAttribute, isExtension, currentParent);
    return changeMade;
  }

  /**
   * handles a single patch operation on a multi valued type
   *
   * @param resource the resource that is currently processed
   * @param values the values that should be added or replaced
   * @param fullAttributeNames the full attribute names from the top level to leaf level e.g. ["emails",
   *          "emails.value"]
   * @param schemaAttribute the schema attribute definition of the value that should be replaced
   * @param isExtension if this operation is executed on an extension
   * @param currentParent if {@code isExtension} is true this is the extension node otherwise this node is
   *          equals to {@code resource}
   * @param firstAttribute the attribute extracted from the {@code currentParent}
   * @return true if an effective change was made, false else
   */
  private boolean handlePatchOperationOnMultiValued(ResourceNode resource,
                                                    List<String> values,
                                                    String[] fullAttributeNames,
                                                    SchemaAttribute schemaAttribute,
                                                    boolean isExtension,
                                                    ObjectNode currentParent,
                                                    JsonNode firstAttribute)
  {
    if (PatchOp.REMOVE.equals(patchOp) && fullAttributeNames.length == 1 && path.getSubAttributeName() == null
        && path.getChild() == null)
    {
      evaluatePatchPathOperation(schemaAttribute, firstAttribute.size() == 0 ? null : firstAttribute);
      int sizeBefore = currentParent.size();
      JsonNode removedNode = currentParent.remove(schemaAttribute.getName());
      boolean effectiveChangeMade = false;
      if (sizeBefore > currentParent.size() && removedNode.size() != 0)
      {
        effectiveChangeMade = true;
      }
      else
      {
        throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                      ScimType.RFC7644.NO_TARGET);
      }
      removeExtensionIfEmpty(resource, schemaAttribute, isExtension, currentParent);
      return effectiveChangeMade;
    }
    boolean changeWasMade = handleMultiValuedAttribute(schemaAttribute,
                                                       (ArrayNode)firstAttribute,
                                                       fullAttributeNames,
                                                       values);
    if (firstAttribute.size() == 0)
    {
      resource.remove(schemaAttribute.getName());
      removeExtensionIfEmpty(resource, schemaAttribute, isExtension, currentParent);
    }
    return changeWasMade;
  }

  /**
   * in case that an extension object is empty after a remove operation the extension will be removed from the
   * resource
   *
   * @param resource the resource that owns the extension attribute
   * @param schemaAttribute the attribute of the extension that was removed
   * @param isExtension if the current operation is executed on an extension or not
   * @param currentParent if the value {@code isExtension} is true this is the extension object otherwise it is
   *          equals to {@code resource}
   */
  private void removeExtensionIfEmpty(ResourceNode resource,
                                      SchemaAttribute schemaAttribute,
                                      boolean isExtension,
                                      ObjectNode currentParent)
  {
    if (isExtension && currentParent.size() == 0)
    {
      resource.remove(schemaAttribute.getResourceUri());
      resource.removeSchema(schemaAttribute.getResourceUri());
    }
  }

  /**
   * adds an extension uri to the schemas attribute under the condition that the operation is not a remove
   * operation and that the extension uri is not already present within the schemas
   *
   * @param resource the resource node to which the extension uri should be added
   * @param patchOp the current patch operation
   * @param schemaAttribute the schema attribute that is the target of operation
   */
  private void addExtensionToSchemas(ResourceNode resource, PatchOp patchOp, SchemaAttribute schemaAttribute)
  {
    if (patchOp.equals(PatchOp.REMOVE))
    {
      return;
    }
    resource.addSchema(schemaAttribute.getResourceUri());
  }

  /**
   * adds or replaces a simple node in the given object node
   *
   * @param schemaAttribute the attribute schema definition
   * @param objectNode the object node into which the new node should be added or replaced
   * @param values the values that should be added to the node. This list must not contain more than a single
   *          entry
   * @return true if an effective change was made, false else
   */
  protected boolean handleSimpleNode(SchemaAttribute schemaAttribute, ObjectNode objectNode, List<String> values)
  {
    if (!PatchOp.REMOVE.equals(patchOp) && values.size() > 1 && !schemaAttribute.isMultiValued())
    {
      throw new BadRequestException("found multiple values for simple attribute '"
                                    + schemaAttribute.getFullResourceName() + "': " + String.join(",", values), null,
                                    ScimType.RFC7644.INVALID_VALUE);
    }

    JsonNode oldNode = objectNode.get(schemaAttribute.getName());
    evaluatePatchPathOperation(schemaAttribute, oldNode);
    if (PatchOp.REMOVE.equals(patchOp))
    {
      if (oldNode == null)
      {
        throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                      ScimType.RFC7644.NO_TARGET);
      }
      else
      {
        objectNode.remove(schemaAttribute.getName());
        return true;
      }
    }

    JsonNode newNode = createNewNode(schemaAttribute, values.get(0));
    if (!newNode.equals(oldNode))
    {
      objectNode.set(schemaAttribute.getName(), newNode);
      return true;
    }
    else
    {
      return false;
    }
  }

  /**
   * adds or replaces complex attributes
   *
   * @param schemaAttribute the attribute schema definition
   * @param resource the resource into which the complex type should be added or replaced
   * @param fullAttributeNames contains all attribute names. It starts with the name of the complex attributes
   *          and might follow with the sub-attribute of the complex type if specified. the full name contains
   *          the full resource uri
   * @param values the values that should be added to the complex type
   * @return true if an effective change has been made, false else
   */
  private boolean handleComplexAttribute(SchemaAttribute schemaAttribute,
                                         ObjectNode resource,
                                         String[] fullAttributeNames,
                                         List<String> values)
  {
    if (fullAttributeNames.length > 1)
    {
      ObjectNode complexNode = (ObjectNode)resource.get(schemaAttribute.getName());
      evaluatePatchPathOperation(schemaAttribute, complexNode);
      return handleComplexSubAttributePathReference(schemaAttribute, resource, fullAttributeNames[1], values);
    }
    else
    {
      return handleDirectComplexPathReference(schemaAttribute, resource, values);
    }
  }

  /**
   * will handle a direct complex path reference e.g. "name"
   *
   * @param schemaAttribute the schema attribute definition of the complex attribute
   * @param resource the resource that is currently processed
   * @param values the values that will be added or replaced
   * @return true if an effective change has been made, false else
   */
  private boolean handleDirectComplexPathReference(SchemaAttribute schemaAttribute,
                                                   ObjectNode resource,
                                                   List<String> values)
  {
    ObjectNode complexNode = (ObjectNode)resource.get(schemaAttribute.getName());
    evaluatePatchPathOperation(schemaAttribute, complexNode);
    if (values.size() != 1 || StringUtils.isBlank(values.get(0)))
    {
      throw new BadRequestException("found multiple or no values for non multi valued complex type '"
                                    + schemaAttribute.getFullResourceName() + "': \n\t" + String.join(",", values),
                                    null, ScimType.RFC7644.INVALID_VALUE);
    }
    JsonNode newNode = JsonHelper.readJsonDocument(values.get(0));
    if (newNode == null || !newNode.isObject())
    {
      throw new BadRequestException("given value is not a complex json representation for attribute '"
                                    + schemaAttribute.getFullResourceName() + "': \n\t" + String.join(",", values),
                                    null, ScimType.RFC7644.INVALID_VALUE);
    }
    PatchFilterResolver filterResolver = new PatchFilterResolver();
    boolean hasFilterExpression = path.getChild() != null;
    if (complexNode != null && hasFilterExpression
        && !filterResolver.isNodeMatchingFilter(complexNode, path).isPresent())
    {
      throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                    ScimType.RFC7644.NO_TARGET);
    }
    boolean changeWasMade = false;
    if (PatchOp.ADD.equals(patchOp))
    {
      JsonNode oldNode = resource.get(schemaAttribute.getName());
      newNode = mergeObjectNodes((ObjectNode)newNode, oldNode);
      resource.set(schemaAttribute.getName(), newNode);
      changeWasMade = !newNode.equals(oldNode);
    }
    else if (PatchOp.REPLACE.equals(patchOp))
    {
      resource.set(schemaAttribute.getName(), newNode);
      changeWasMade = true;
    }
    return changeWasMade;
  }

  /**
   * handles a complex sub attribute path reference e.g. "name.givenName"
   *
   * @param schemaAttribute the schema attribute definition of the sub attribute
   * @param resource the resource that is currently processed
   * @param fullAttributeName the attribute name e.g. name.givenName
   * @param values the values that should be added or replaced
   * @return true if an effective change was made on this resource false else
   */
  private boolean handleComplexSubAttributePathReference(SchemaAttribute schemaAttribute,
                                                         ObjectNode resource,
                                                         String fullAttributeName,
                                                         List<String> values)
  {
    SchemaAttribute subAttribute = getSchemaAttribute(fullAttributeName);
    ObjectNode complexNode = (ObjectNode)resource.get(schemaAttribute.getName());
    if (complexNode == null)
    {
      complexNode = new ScimObjectNode(schemaAttribute);
      resource.set(schemaAttribute.getName(), complexNode);
    }
    Optional<ObjectNode> matchingNode = new PatchFilterResolver().isNodeMatchingFilter(complexNode, path);
    if (!matchingNode.isPresent())
    {
      throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                    ScimType.RFC7644.NO_TARGET);
    }
    if (handleInnerComplexAttribute(subAttribute, complexNode, values))
    {
      if (complexNode.size() == 0)
      {
        resource.remove(schemaAttribute.getName());
      }
      return true;
    }
    else if (complexNode.size() == 0)
    {
      resource.remove(schemaAttribute.getName());
      return false;
    }
    JsonNode firstAttribute = resource.get(fullAttributeName);
    return firstAttribute == null || !firstAttribute.asText().equals(values.get(0));
  }

  /**
   * extracts the relevant node from the given complex node and adds a value, replaces it or removes it
   *
   * @param subAttribute the attribute that should be added, replaced or removed
   * @param complexNode the parent node of the node that should be added, replaced or removed
   * @param values the value(s) that should be added, replaced or removed
   * @return true if an effective change was made, false else
   */
  private boolean handleInnerComplexAttribute(SchemaAttribute subAttribute, ObjectNode complexNode, List<String> values)
  {
    if (subAttribute.isMultiValued())
    {
      ArrayNode arrayNode = (ArrayNode)complexNode.get(subAttribute.getName());
      if (arrayNode == null)
      {
        arrayNode = new ScimArrayNode(subAttribute);
        complexNode.set(subAttribute.getName(), arrayNode);
      }
      if (PatchOp.REPLACE.equals(patchOp))
      {
        arrayNode.removeAll();
      }
      if (PatchOp.REMOVE.equals(patchOp))
      {
        boolean effectiveChange = complexNode.get(subAttribute.getName()).size() != 0;
        complexNode.remove(subAttribute.getName());
        if (!effectiveChange)
        {
          throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                        ScimType.RFC7644.NO_TARGET);
        }
        return true;
      }
      else
      {
        values.forEach(arrayNode::add);
      }
      return true;
    }
    else
    {
      return handleSimpleNode(subAttribute, complexNode, values);
    }
  }

  /**
   * merges two object nodes into a single node
   */
  private JsonNode mergeObjectNodes(ObjectNode newNode, JsonNode oldNode)
  {
    if (oldNode == null)
    {
      return newNode;
    }
    oldNode.fields().forEachRemaining(stringJsonNodeEntry -> {
      final String key = stringJsonNodeEntry.getKey();
      final JsonNode value = stringJsonNodeEntry.getValue();
      JsonNode newSubNode = newNode.get(key);
      if (newSubNode == null)
      {
        newNode.set(key, value);
      }
      else if (newSubNode.isArray())
      {
        // did it in this way to preserve the original array order and to append the new values
        newSubNode.forEach(((ArrayNode)value)::add);
        newNode.set(key, value);
      }
    });
    return newNode;
  }

  /**
   * handles multi valued complex nodes
   *
   * @param schemaAttribute the schema attribute definition of the top level node
   * @param multiValued the array node that is represented by the {@code schemaAttribute}
   * @param fullAttributeNames the array of full attribute names with their resourceUris e.g. <br>
   *
   *          <pre>
   *              urn:gold:params:scim:schemas:custom:2.0:AllTypes:name<br>
   *              urn:gold:params:scim:schemas:custom:2.0:AllTypes:name.givenName
   *          </pre>
   *
   * @param values the values that should be added to the multi valued complex type
   * @return true if an effective change has been made, false else
   */
  private boolean handleMultiValuedAttribute(SchemaAttribute schemaAttribute,
                                             ArrayNode multiValued,
                                             String[] fullAttributeNames,
                                             List<String> values)
  {
    if (Type.COMPLEX.equals(schemaAttribute.getType()))
    {
      if (fullAttributeNames.length > 1)
      {
        return handleMultiComplexSubAttributePath(multiValued, fullAttributeNames[1], values);
      }
      else
      {
        evaluatePatchPathOperation(schemaAttribute, multiValued.size() == 0 ? null : multiValued);
        return handleDirectMultiValuedComplexPathReference(multiValued, values);
      }
    }
    else
    {
      if (PatchOp.REPLACE.equals(patchOp))
      {
        multiValued.removeAll();
      }
      for ( String value : values )
      {
        multiValued.add(createNewNode(schemaAttribute, value));
      }
      return true;
    }
  }

  /**
   * handles a direct multivalued complex path reference e.g. "emails" or "emails[type eq "work"]"
   *
   * @param multiValued the multi values array node that holds the complex nodes
   * @param values the values that should be added or replaced on all matching nodes
   * @return true if an effective change has been made, false else
   */
  private boolean handleDirectMultiValuedComplexPathReference(ArrayNode multiValued, List<String> values)
  {
    List<IndexNode> matchingComplexNodes = resolveFilter(multiValued, path);
    if (PatchOp.REMOVE.equals(patchOp))
    {
      boolean changeWasMade = false;
      for ( int i = matchingComplexNodes.size() - 1 ; i >= 0 ; i-- )
      {
        multiValued.remove(matchingComplexNodes.get(i).getIndex());
        changeWasMade = true;
      }
      return changeWasMade;
    }
    if (matchingComplexNodes.isEmpty() && path.getChild() != null)
    {
      throw new BadRequestException(String.format("Cannot '%s' value on path '%s' for no matching object was found",
                                                  patchOp,
                                                  path),
                                    null, ScimType.RFC7644.NO_TARGET);
    }
    if (PatchOp.REPLACE.equals(patchOp))
    {
      for ( IndexNode indexNode : matchingComplexNodes )
      {
        multiValued.remove(indexNode.getIndex());
      }
    }
    for ( String value : values )
    {
      try
      {
        JsonNode jsonNode = JsonHelper.readJsonDocument(value);
        JsonNode primary = jsonNode.get(AttributeNames.RFC7643.PRIMARY);
        checkForPrimary(multiValued, primary != null && primary.booleanValue());
        multiValued.add(jsonNode);
      }
      catch (IOException ex)
      {
        throw new BadRequestException("the value must be a whole complex type json structure but was: '" + value + "'",
                                      ex, ScimType.RFC7644.INVALID_VALUE);
      }
    }
    return true;
  }

  /**
   * this method will check if the current operation adds a new primary value and will set the original primary
   * to false if such a value exists
   *
   * @param multiValued the multivalued complex array that might hold any primary values
   * @param primary if the new value is primary or not
   */
  private void checkForPrimary(ArrayNode multiValued, boolean primary)
  {
    if (!primary)
    {
      return;
    }
    multiValued.forEach(jsonNode -> {
      ((ObjectNode)jsonNode).remove(AttributeNames.RFC7643.PRIMARY);
    });
  }

  /**
   * handles a multivalued complex type with a path reference that points to a sub attribute of the multi-valued
   * complex type e.g. emails.value
   *
   * @param multiValued the array of the multivalued complex node
   * @param fullAttributeName the full name of the sub attribute
   * @param values the values that should be added or replaced
   * @return true if an effective change has been made, false else
   */
  private boolean handleMultiComplexSubAttributePath(ArrayNode multiValued,
                                                     String fullAttributeName,
                                                     List<String> values)
  {
    if (!PatchOp.REMOVE.equals(patchOp) && multiValued.size() == 0)
    {
      return false;
    }
    SchemaAttribute subAttribute = RequestUtils.getSchemaAttributeByAttributeName(resourceType, fullAttributeName);
    List<IndexNode> matchingComplexNodes = resolveFilter(multiValued, path);
    AtomicBoolean changeWasMade = new AtomicBoolean(false);
    if (AttributeNames.RFC7643.PRIMARY.equals(subAttribute.getName()))
    {
      checkForPrimary(multiValued, Boolean.parseBoolean(values.get(0)));
    }
    if ((path.getChild() != null && matchingComplexNodes.isEmpty())
        || (path.getChild() == null && matchingComplexNodes.isEmpty() && PatchOp.REMOVE.equals(patchOp)))
    {
      throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                    ScimType.RFC7644.NO_TARGET);
    }
    for ( int i = 0 ; i < matchingComplexNodes.size() ; i++ )
    {
      ObjectNode complexNode = matchingComplexNodes.get(i).getObjectNode();
      changeWasMade.weakCompareAndSet(false, handleInnerComplexAttribute(subAttribute, complexNode, values));
      if (complexNode.size() == 0)
      {
        multiValued.remove(matchingComplexNodes.get(i).getIndex());
      }
    }
    return changeWasMade.get();
  }

  /**
   * this method will extract all complex types from the given array node that do match the filter
   *
   * @param multiValuedComplex the multi valued complex node
   * @param path the filter expression that must be resolved to get the matching nodes
   * @return the list of nodes that should be modified
   */
  private List<IndexNode> resolveFilter(ArrayNode multiValuedComplex, AttributePathRoot path)
  {
    PatchFilterResolver patchFilterResolver = new PatchFilterResolver();
    List<IndexNode> matchingComplexNodes = new ArrayList<>();
    for ( int i = 0 ; i < multiValuedComplex.size() ; i++ )
    {
      JsonNode complex = multiValuedComplex.get(i);
      Optional<ObjectNode> filteredNode = patchFilterResolver.isNodeMatchingFilter((ObjectNode)complex, path);
      if (filteredNode.isPresent())
      {
        matchingComplexNodes.add(new IndexNode(i, filteredNode.get()));
      }
    }
    if (path.getChild() != null && matchingComplexNodes.size() == 0)
    {
      return Collections.emptyList();
    }
    return matchingComplexNodes;
  }

  /**
   * creates a new json node with the given value
   *
   * @param schemaAttribute the attribute schema definition
   * @param value the value that should be added into the node
   * @return the simple json node
   */
  private JsonNode createNewNode(SchemaAttribute schemaAttribute, String value)
  {
    switch (schemaAttribute.getType())
    {
      case STRING:
      case DATE_TIME:
      case REFERENCE:
        return new ScimTextNode(schemaAttribute, value);
      case BOOLEAN:
        return new ScimBooleanNode(schemaAttribute, Boolean.parseBoolean(value));
      case INTEGER:
        Long longVal = Long.parseLong(value);
        if (longVal == longVal.intValue())
        {
          return new ScimIntNode(schemaAttribute, longVal.intValue());
        }
        else
        {
          return new ScimLongNode(schemaAttribute, longVal);
        }
      default:
        return new ScimDoubleNode(schemaAttribute, Double.parseDouble(value));
    }
  }

  /**
   * will check that the expressions are correctly written for the defined patch operation
   *
   * @param values the values of the request
   */
  protected void validateRequest(List<String> values)
  {
    validateAttributeType(values);
    validatePath(path, patchOp, values);
  }

  /**
   * this method will check the the expression send by the client does follow its syntax rules based on the used
   * operation
   *
   * @param path the target expression
   * @param patchOp the operation
   * @param values the values (should be empty on delete)
   */
  private void validatePath(AttributePathRoot path, PatchOp patchOp, List<String> values)
  {
    switch (patchOp)
    {
      case ADD:
        checkIsValidComplexJson(path, values);
        break;
      case REPLACE:
        validateReplaceOperation(path, values);
        break;
      case REMOVE:
        validateRemoveOperation(path, values);
        break;
    }
  }

  /**
   * will validate that no values are present in the values list all other path representations should be valid
   * except for an empty representation
   *
   * @param path the attribute path expression
   * @param values in remove operation no values should be present
   */
  private void validateRemoveOperation(AttributePathRoot path, List<String> values)
  {
    if (values != null && !values.isEmpty())
    {
      throw new BadRequestException("values must not be set for remove operation but was: " + String.join(",", values),
                                    null, ScimType.RFC7644.INVALID_VALUE);
    }
    if (path == null)
    {
      throw new BadRequestException("no target present within the request", null, ScimType.RFC7644.INVALID_PATH);
    }
  }

  /**
   * will validate that the given attribute path expression is valid for a replace operation
   *
   * @param path the attribute path expression
   * @param values the values that should replace other values
   */
  private void validateReplaceOperation(AttributePathRoot path, List<String> values)
  {
    if (values == null || values.size() == 0)
    {
      throw new BadRequestException("values parameter must be set for replace operation but was empty", null,
                                    ScimType.RFC7644.INVALID_VALUE);
    }
    if (StringUtils.isBlank(path.getSubAttributeName()) && path.getChild() != null
        && !values.stream().allMatch(JsonHelper::isValidJson))
    {
      throw new BadRequestException("the values are expected to be valid json representations for an expression as "
                                    + "'" + path.toString() + "' but was: " + String.join(",\n", values), null,
                                    ScimType.RFC7644.INVALID_PATH);
    }
    checkIsValidComplexJson(path, values);
  }

  /**
   * verifies that the values are valid json representations if we have an injection into a complex type without
   * a sub-attribute
   *
   * @param path the target of the expression
   * @param values the values should be added or replaced
   */
  private void checkIsValidComplexJson(AttributePathRoot path, List<String> values)
  {
    String[] namePath = path.getShortName().split("\\.");
    // emails or name
    if (path.getChild() == null && Type.COMPLEX.equals(path.getSchemaAttribute().getType()) && namePath.length == 1
        && !values.stream().allMatch(JsonHelper::isValidJson))
    {
      throw new BadRequestException("the value parameters must be valid json representations but was '"
                                    + String.join(",", values) + "'", null, ScimType.RFC7644.INVALID_VALUE);

    }
  }

  /**
   * checks that if the attribute is a simple type and not multi valued that only a single attribute is allowed
   * in the values parameter of the patch request
   *
   * @param values the values parameter that is under test
   */
  private void validateAttributeType(List<String> values)
  {
    switch (schemaAttribute.getType())
    {
      case STRING:
      case DATE_TIME:
      case REFERENCE:
      case BOOLEAN:
      case INTEGER:
      case DECIMAL:
        if (!schemaAttribute.isMultiValued() && values.size() > 1)
        {
          throw new BadRequestException("several values found for non multivalued node of type '"
                                        + schemaAttribute.getType() + "'", null, ScimType.RFC7644.INVALID_VALUE);
        }
        break;
    }
  }

  /**
   * will get the fully qualified attribute names
   */
  private String[] getAttributeNames()
  {
    String attributeName = path.getShortName()
                           + (StringUtils.isBlank(path.getSubAttributeName()) ? "" : "." + path.getSubAttributeName());
    String[] attributeNames = attributeName.split("\\.");
    String resourceUri = path.getResourceUri() == null ? "" : path.getResourceUri() + ":";
    attributeNames[0] = resourceUri + attributeNames[0];
    for ( int i = 1 ; i < attributeNames.length ; i++ )
    {
      attributeNames[i] = attributeNames[i - 1] + "." + attributeNames[i];
    }
    return attributeNames;
  }

  /**
   * retrieves the schema attribute definition of the top loevel node of the patch attribute. The top level node
   * would be e.g. 'name' in the following representation "name.givenName"
   */
  private SchemaAttribute getSchemaAttribute()
  {
    if (this.schemaAttribute == null)
    {
      this.schemaAttribute = getSchemaAttribute(path.getFullName());
    }
    return this.schemaAttribute;
  }

  /**
   * a helper class that is used in case of filtering. We will also hold the index of the filtered nodes
   */
  @Getter
  @AllArgsConstructor
  private static class IndexNode
  {

    /**
     * the index of a filtered node
     */
    private int index;

    /**
     * a filtered node
     */
    private ObjectNode objectNode;
  }
}
