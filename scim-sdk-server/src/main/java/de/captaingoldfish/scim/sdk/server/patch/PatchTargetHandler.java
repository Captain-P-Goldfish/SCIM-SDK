package de.captaingoldfish.scim.sdk.server.patch;

import java.util.ArrayList;
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
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.filter.resources.PatchFilterResolver;
import de.captaingoldfish.scim.sdk.server.patch.operations.PatchOperation;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import de.captaingoldfish.scim.sdk.server.utils.ScimAttributeHelper;
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
public class PatchTargetHandler extends AbstractPatch implements ScimAttributeHelper
{

  /**
   * used to check if the ms-azure workaround for patch operations with filters is active.
   */
  private final PatchConfig patchConfig;

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


  public PatchTargetHandler(PatchConfig patchConfig, ResourceType resourceType, PatchOperation patchOperation)
  {
    super(resourceType);
    this.patchConfig = patchConfig;
    this.path = patchOperation.getAttributePath();
    this.patchOp = patchOperation.getPatchOp();
    if (!(this.path instanceof PatchExtensionAttributePath))
    {
      this.schemaAttribute = getSchemaAttribute();
    }
  }

  /**
   * will add, replace or remove the specified values based on the given path-attribute
   *
   * @param resource the resource to which the values should be added
   * @param values the values that should be added into the resource
   * @return true if an effective change was made, false else
   */
  public boolean handleOperationValues(ResourceNode resource, List<String> values)
  {
    if (path instanceof PatchExtensionAttributePath)
    {
      return handleExtensionOperation(resource, values);
    }
    return handlePathAttributeOperation(resource, values);
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
    if (!PatchOp.REMOVE.equals(patchOp) && Mutability.IMMUTABLE.equals(schemaAttribute.getMutability())
        && attribute != null)
    {
      String errorMessage = String.format("The attribute '%s' is 'IMMUTABLE' and is not unassigned. Current value is: %s",
                                          schemaAttribute.getScimNodeName(),
                                          attribute.asText());
      throw new BadRequestException(errorMessage, null, ScimType.RFC7644.MUTABILITY);
    }
  }

  /**
   * handles path references that will directly address an extension of a resource
   *
   * @param resource the resource to which the values should be handled
   * @param values the values that should contain a single element that contains the referenced extension, or
   *          should be empty.
   * @return true if an effective change was made, false else
   */
  private boolean handleExtensionOperation(ResourceNode resource, List<String> values)
  {
    boolean changeMade;
    final boolean addOrReplaceValue = !PatchOp.REMOVE.equals(patchOp);
    ObjectNode currentNode = (ObjectNode)resource.get(path.getFullName());
    if (addOrReplaceValue)
    {
      ObjectNode extensionNode = (ObjectNode)JsonHelper.readJsonDocument(values.get(0));
      changeMade = !extensionNode.equals(currentNode);
      if (extensionNode.isEmpty())
      {
        resource.remove(path.getFullName());
      }
      else
      {
        resource.set(path.getFullName(), extensionNode);
      }
    }
    else
    {
      changeMade = currentNode != null && !currentNode.isEmpty();
      resource.remove(path.getFullName());
    }
    return changeMade;
  }

  /**
   * handles patch requests whose path will directly address an attribute of either the main resource or an
   * extension
   *
   * @param resource the resource to which the values should be handled
   * @param values the values that should be handled
   * @return true if an effective change was made, false else
   */
  private boolean handlePathAttributeOperation(ResourceNode resource, List<String> values)
  {
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
    else if (schemaAttribute.isComplexAttribute())
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
        if (patchConfig.isDoNotFailOnNoTarget())
        {
          return false;
        }
        else
        {
          throw new BadRequestException(String.format("No target found for path-filter '%s'",
                                                      schemaAttribute.getFullResourceName()),
                                        ScimType.RFC7644.NO_TARGET);
        }
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
   * handles a single patch operation on a multivalued type
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
    if (PatchOp.REMOVE.equals(patchOp) && fullAttributeNames.length == 1 && path.getSubAttributeName() == null)
    {
      evaluatePatchPathOperation(schemaAttribute, firstAttribute.size() == 0 ? null : firstAttribute);
      int sizeBefore = currentParent.size();
      boolean effectiveChangeMade = false;
      if (path.getChild() == null)
      {
        JsonNode removedNode = currentParent.remove(schemaAttribute.getName());
        if (sizeBefore > currentParent.size() && removedNode.size() != 0)
        {
          effectiveChangeMade = true;
        }
        else
        {
          if (patchConfig.isDoNotFailOnNoTarget())
          {
            return false;
          }
          else
          {
            throw new BadRequestException(String.format("No target found for path-filter '%s'",
                                                        schemaAttribute.getFullResourceName()),
                                          ScimType.RFC7644.NO_TARGET);
          }
        }
        removeExtensionIfEmpty(resource, schemaAttribute, isExtension, currentParent);
        return effectiveChangeMade;
      }
      else if (schemaAttribute.getParent() == null && !Type.COMPLEX.equals(schemaAttribute.getType()))
      {
        JsonNode jsonNode = currentParent.get(schemaAttribute.getName());
        if (jsonNode != null && jsonNode.isArray())
        {
          ArrayNode arrayNode = (ArrayNode)jsonNode;
          List<Integer> indexesToRemove = new ArrayList<>();
          for ( int i = 0 ; i < arrayNode.size() ; i++ )
          {
            JsonNode simpleNode = arrayNode.get(i);
            PatchFilterResolver patchFilterResolver = new PatchFilterResolver();
            boolean doesEntryMatch = patchFilterResolver.isSimpleNodeMatchingFilter(simpleNode, path.getChild());
            if (doesEntryMatch)
            {
              indexesToRemove.add(i);
            }
          }
          for ( int i = indexesToRemove.size() - 1 ; i >= 0 ; i-- )
          {
            arrayNode.remove(indexesToRemove.get(i));
            effectiveChangeMade = true;
          }
          if (arrayNode.isEmpty())
          {
            currentParent.remove(schemaAttribute.getName());
          }
        }
        if (!effectiveChangeMade && !patchConfig.isDoNotFailOnNoTarget())
        {
          throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                        ScimType.RFC7644.NO_TARGET);
        }
        return effectiveChangeMade;
      }
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
    JsonNode oldNode = objectNode.get(schemaAttribute.getName());
    if (!PatchOp.REMOVE.equals(patchOp)
        && values.get(0).equals(Optional.ofNullable(oldNode).map(JsonNode::textValue).orElse(null)))
    {
      return false;
    }
    evaluatePatchPathOperation(schemaAttribute, oldNode);
    if (PatchOp.REMOVE.equals(patchOp))
    {
      boolean isSimpleNode = schemaAttribute.getParent() == null;
      boolean isSimpleNodeInMultivaluedComplex = schemaAttribute.getParent() != null
                                                 && schemaAttribute.getParent().isMultiValued();
      if ((oldNode == null && isSimpleNode) || (oldNode == null && !isSimpleNodeInMultivaluedComplex))
      {
        if (patchConfig.isDoNotFailOnNoTarget())
        {
          return false;
        }
        else
        {
          throw new BadRequestException(String.format("No target found for path-filter '%s'",
                                                      schemaAttribute.getFullResourceName()),
                                        ScimType.RFC7644.NO_TARGET);
        }
      }
      else
      {
        objectNode.remove(schemaAttribute.getName());
        return true;
      }
    }

    JsonNode newNode = parseToJsonNode(schemaAttribute, values.get(0));
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
    JsonNode newNode = JsonHelper.readJsonDocument(values.get(0));
    PatchFilterResolver filterResolver = new PatchFilterResolver();
    boolean hasFilterExpression = path.getChild() != null;
    if (complexNode != null && hasFilterExpression
        && !filterResolver.isNodeMatchingFilter(complexNode, path, patchOp).isPresent())
    {
      if (patchConfig.isDoNotFailOnNoTarget())
      {
        return false;
      }
      else
      {
        throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                      ScimType.RFC7644.NO_TARGET);
      }
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
    Optional<ObjectNode> matchingNode = new PatchFilterResolver().isNodeMatchingFilter(complexNode, path, patchOp);
    if (!matchingNode.isPresent())
    {
      if (patchConfig.isDoNotFailOnNoTarget())
      {
        return false;
      }
      else
      {
        throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                      ScimType.RFC7644.NO_TARGET);
      }
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
    return false;
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
        boolean isParentMultivalued = subAttribute.getParent().isMultiValued();
        if (!effectiveChange && !isParentMultivalued)
        {
          if (patchConfig.isDoNotFailOnNoTarget())
          {
            return false;
          }
          else
          {
            throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                          ScimType.RFC7644.NO_TARGET);
          }
        }
        else if (!effectiveChange && isParentMultivalued)
        {
          return false;
        }
        return true;
      }
      else
      {
        for ( String value : values )
        {
          JsonNode jsonValue = parseToJsonNode(subAttribute, value);
          arrayNode.add(jsonValue);
        }
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
   * @param values the values that should be added to the multivalued complex type
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
        multiValued.add(parseToJsonNode(schemaAttribute, value));
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
      if (patchConfig.isDoNotFailOnNoTarget())
      {
        return false;
      }
      else
      {
        throw new BadRequestException(String.format("Cannot '%s' value on path '%s' for no matching object was found",
                                                    patchOp,
                                                    path),
                                      null, ScimType.RFC7644.NO_TARGET);
      }
    }
    if (PatchOp.REPLACE.equals(patchOp))
    {
      for ( int i = matchingComplexNodes.size() - 1 ; i >= 0 ; i-- )
      {
        IndexNode indexNode = matchingComplexNodes.get(i);
        boolean isEquals = indexNode.getObjectNode().equals(JsonHelper.readJsonDocument(values.get(0)));
        if (isEquals)
        {
          return false;
        }
        multiValued.remove(indexNode.getIndex());
      }
    }
    for ( String value : values )
    {
      ObjectNode complexNode;
      try
      {
        complexNode = (ObjectNode)JsonHelper.readJsonDocument(value);
      }
      catch (IOException ex)
      {
        throw new BadRequestException("The value must be a whole complex type json structure but was: '" + value + "'",
                                      ex, ScimType.RFC7644.INVALID_VALUE);
      }
      JsonNode primary = complexNode.get(AttributeNames.RFC7643.PRIMARY);
      checkForPrimary(multiValued, primary != null && primary.booleanValue());

      if (path.isWithFilter() && !matchingComplexNodes.isEmpty())
      {
        complexNode.fields().forEachRemaining(entry -> {
          for ( IndexNode matchingComplexIndexNode : matchingComplexNodes )
          {
            ObjectNode matchingNode = matchingComplexIndexNode.getObjectNode();
            if (PatchOp.REPLACE.equals(patchOp))
            {
              matchingNode.set(entry.getKey(), entry.getValue());
            }
            else // patchOp == ADD
            {
              JsonNode originalValue = matchingNode.get(entry.getKey());
              if (originalValue == null || originalValue.isNull() || !originalValue.isArray())
              {
                matchingNode.set(entry.getKey(), entry.getValue());
              }
              else // we are operating on a complex type and complex types may never have other complex types
              {
                ArrayNode originalArray = (ArrayNode)originalValue;
                ArrayNode newValue = (ArrayNode)entry.getValue();
                originalArray.addAll(newValue);
              }
            }
          }
        });
        if (PatchOp.REPLACE.equals(patchOp))
        {
          for ( IndexNode matchingComplexNode : matchingComplexNodes )
          {
            multiValued.add(matchingComplexNode.getObjectNode());
          }
        }
      }
      else if (!path.isWithFilter() || matchingComplexNodes.isEmpty())
      {
        multiValued.add(complexNode);
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
   * handles a multivalued complex type with a path reference that points to a sub attribute of the multivalued
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
    SchemaAttribute subAttribute = RequestUtils.getSchemaAttributeByAttributeName(resourceType, fullAttributeName);
    List<IndexNode> matchingComplexNodes = resolveFilter(multiValued, path);
    AtomicBoolean changeWasMade = new AtomicBoolean(false);
    if (AttributeNames.RFC7643.PRIMARY.equals(subAttribute.getName()))
    {
      checkForPrimary(multiValued, Boolean.parseBoolean(values.get(0)));
    }
    if (path.getChild() != null && matchingComplexNodes.isEmpty())
    {
      // a filter expression was present and no matches were found e.g. (emails[type eq "work"].type)
      if (patchConfig.isDoNotFailOnNoTarget())
      {
        return false;
      }
      else
      {
        throw new BadRequestException(String.format("No target found for path-filter '%s'", path),
                                      ScimType.RFC7644.NO_TARGET);
      }
    }
    if (path.getChild() == null && matchingComplexNodes.isEmpty() && PatchOp.REMOVE.equals(patchOp))
    {
      // no filter expression was present e.g. (emails.type)
      return false;
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
      Optional<ObjectNode> filteredNode = patchFilterResolver.isNodeMatchingFilter((ObjectNode)complex, path, patchOp);
      if (filteredNode.isPresent())
      {
        matchingComplexNodes.add(new IndexNode(i, filteredNode.get()));
      }
    }
    if (path.getChild() != null && matchingComplexNodes.size() == 0)
    {
      return new ArrayList<>();
    }
    return matchingComplexNodes;
  }

  /**
   * will get the fully qualified attribute names
   */
  private String[] getAttributeNames()
  {
    // TODO somethings wrong here. See test:
    // PatchRequestValidationTests.ComplexAttributeTests.ResourcePatchTests#testAddSubNumberArrayWithMsAzureStyleRef
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
