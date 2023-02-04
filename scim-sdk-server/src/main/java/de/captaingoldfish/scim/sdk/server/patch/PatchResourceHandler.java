package de.captaingoldfish.scim.sdk.server.patch;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.patch.msazure.MsAzurePatchResourceWorkaroundHandler;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;


/**
 * author Pascal Knueppel <br>
 * created at: 30.10.2019 - 08:49 <br>
 * <br>
 * this class will handle the in which the patch-add operation does not define a target and the value is
 * represented by the resource itself:<br>
 * <br>
 *
 * <pre>
 *    The result of the add operation depends upon what the target location
 *    indicated by "path" references:
 *
 *    o  If omitted, the target location is assumed to be the resource
 *       itself.  The "value" parameter contains a set of attributes to be
 *       added to the resource.
 * </pre>
 */
public class PatchResourceHandler extends AbstractPatch
{

  /**
   * the patch configuration of the current request. Used to check if workarounds are activated
   */
  private final PatchConfig patchConfig;

  /**
   * tells us if the current operation is an add or a replace operation.
   */
  private PatchOp patchOp;

  public PatchResourceHandler(PatchConfig patchConfig, ResourceType resourceType, PatchOp op)
  {
    super(resourceType);
    this.patchConfig = patchConfig;
    this.patchOp = op;
  }

  /**
   * adds the values of the patch operation into the given resource node
   *
   * @param resource the resource node into which the values should be added
   * @param patchJsonDocument the patch operation resource from which the values should be added into the
   *          resource node
   * @param extensionUri this extensionUri is used for resolving extensions in the resource if name conflicts do
   *          exist we need the fully qualified name for verifying the attributes.
   */
  public boolean addResourceValues(ObjectNode resource, JsonNode patchJsonDocument, String extensionUri)
  {
    if (patchJsonDocument == null || patchJsonDocument.size() == 0)
    {
      throw new BadRequestException("no attributes present in value-resource in patch operation", null,
                                    ScimType.RFC7644.INVALID_VALUE);
    }
    AtomicBoolean changeWasMade = new AtomicBoolean(false);
    JsonHelper.removeAttribute(patchJsonDocument, AttributeNames.RFC7643.SCHEMAS);
    patchJsonDocument.fields().forEachRemaining(stringJsonNodeEntry -> {
      String key = stringJsonNodeEntry.getKey();
      JsonNode value = stringJsonNodeEntry.getValue();
      ResourceType.SchemaExtension extensionRef = resourceType.getSchemaExtensions()
                                                              .stream()
                                                              .filter(ext -> key.equals(ext.getSchema())
                                                                             // ms azure workaround
                                                                             // https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/193
                                                                             || key.startsWith(ext.getSchema()))
                                                              .findAny()
                                                              .orElse(null);
      if (extensionRef != null)
      {
        JsonNode extensionResource = resource.get(extensionRef.getSchema());
        if (extensionResource == null)
        {
          extensionResource = new ScimObjectNode();
          resource.set(extensionRef.getSchema(), extensionResource);
          ((ResourceNode)resource).addSchema(key);
        }
        // ms azure workaround
        // https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/193
        JsonNode effectiveValue = value;
        // if the attribute is not identical to the extension-schema-reference but starts with the
        // extension-schema-reference
        boolean executeMsAzureWorkaround = !key.equals(extensionRef.getSchema())
                                           && key.startsWith(extensionRef.getSchema());
        if (executeMsAzureWorkaround)
        {
          MsAzurePatchResourceWorkaroundHandler workaroundHandler = new MsAzurePatchResourceWorkaroundHandler(resourceType);
          effectiveValue = workaroundHandler.rebuildResource(extensionRef, key, value);
        }
        if (effectiveValue.isEmpty())
        {
          resource.remove(extensionRef.getSchema());
          changeWasMade.compareAndSet(false, extensionResource.size() > 0);
        }
        else
        {
          final boolean changeMade = addResourceValues((ObjectNode)extensionResource,
                                                       effectiveValue,
                                                       extensionRef.getSchema());
          changeWasMade.compareAndSet(false, changeMade);
        }
      }
      else
      {
        SchemaAttribute schemaAttribute = getSchemaAttribute((extensionUri == null ? resourceType.getSchema()
          : extensionUri) + ":" + key);
        boolean resourceChanged;
        boolean isReadOnlyAndSet = isReadOnlyAndSet(resource, schemaAttribute);
        if (Type.COMPLEX.equals(schemaAttribute.getType()))
        {
          resourceChanged = addComplexAttribute(resource, schemaAttribute, value, extensionUri);
        }
        else if (schemaAttribute.isMultiValued())
        {
          resourceChanged = addMultivaluedAttribute(resource, schemaAttribute, value);
        }
        else
        {
          resourceChanged = addSimpleAttribute(resource, schemaAttribute, value);
        }
        verifyReadOnlyNotModified(schemaAttribute, resourceChanged, isReadOnlyAndSet);
        changeWasMade.weakCompareAndSet(false, resourceChanged);
      }
    });
    return changeWasMade.get();
  }

  /**
   * determines if the given json node is read-only or immutable and previously set, if so returns true
   *
   * @param jsonNode the node that should be verified
   * @param schemaAttribute the schema definition of the node
   */
  private boolean isReadOnlyAndSet(JsonNode jsonNode, SchemaAttribute schemaAttribute)
  {
    return Mutability.READ_ONLY.equals(schemaAttribute.getMutability())
           || Mutability.IMMUTABLE.equals(schemaAttribute.getMutability())
              && jsonNode.get(schemaAttribute.getName()) != null;
  }

  /**
   * verifies that the given json node is neither readOnly nor immutable and already set
   *
   * @param schemaAttribute the schema definition of the node
   * @param resourceChanged true if the resource was modified
   * @param isReadOnlyAndSet true if the field is read-only OR immutable and previously set
   */
  private void verifyReadOnlyNotModified(SchemaAttribute schemaAttribute,
                                         boolean resourceChanged,
                                         boolean isReadOnlyAndSet)
  {
    if (resourceChanged && isReadOnlyAndSet)
    {
      throw new BadRequestException("attribute with name '" + schemaAttribute.getFullResourceName()
                                    + "' cannot be written it has a mutability of '" + schemaAttribute.getMutability()
                                    + "'", null, ScimType.RFC7644.MUTABILITY);
    }
  }

  /**
   * verifies that the given json node is neither readOnly nor immutable and already set
   *
   * @param jsonNode the node that should be verified
   * @param schemaAttribute the schema definition of the node
   */
  private void verifyImmutableAndReadOnly(JsonNode jsonNode, SchemaAttribute schemaAttribute)
  {
    if (Mutability.READ_ONLY.equals(schemaAttribute.getMutability())
        || jsonNode.get(schemaAttribute.getName()) != null
           && Mutability.IMMUTABLE.equals(schemaAttribute.getMutability()))
    {
      throw new BadRequestException("attribute with name '" + schemaAttribute.getFullResourceName()
                                    + "' cannot be written it has a mutability of '" + schemaAttribute.getMutability()
                                    + "'", null, ScimType.RFC7644.MUTABILITY);
    }
  }

  /**
   * adds a simple attribute to the given resource. If the attribute does already exist it is replaced if the
   * value is different from before
   *
   * @param resource the resource in which the attribute should be added or replaced
   * @param schemaAttribute the schema attribute definition
   * @param value the value that should be added to the resource
   * @return true if a change was made, false else
   */
  private boolean addSimpleAttribute(ObjectNode resource, SchemaAttribute schemaAttribute, JsonNode value)
  {
    if (!value.equals(resource.get(schemaAttribute.getName())))
    {
      resource.set(schemaAttribute.getName(), value);
      return true;
    }
    else
    {
      return false;
    }
  }

  /**
   * adds a complex attribute to the given resource
   *
   * @param resource the resource to which the attribute should be added
   * @param schemaAttribute the schema attribute definition
   * @param value the value that should be added to the resource
   * @param extensionUri
   * @return true if a change was made, false else
   */
  private boolean addComplexAttribute(ObjectNode resource,
                                      SchemaAttribute schemaAttribute,
                                      JsonNode value,
                                      String extensionUri)
  {
    if (schemaAttribute.isMultiValued())
    {
      return addMultiValuedComplexNode(resource, schemaAttribute, value, extensionUri);
    }
    else
    {
      if (value.equals(resource.get(schemaAttribute.getName())))
      {
        // If the target location already contains the value specified, no
        // changes SHOULD be made to the resource, and a success response
        // SHOULD be returned. Unless other operations change the resource,
        // this operation SHALL NOT change the modify timestamp of the
        // resource.
        return false;
      }

      PatchOp effectivePatchOp = patchOp;
      if (patchConfig.isActivateSailsPointWorkaround() && PatchOp.REPLACE.equals(patchOp))
      {
        effectivePatchOp = PatchOp.ADD;
      }
      if (PatchOp.ADD.equals(effectivePatchOp))
      {
        ObjectNode complexNode;
        if (resource.get(schemaAttribute.getName()) == null)
        {
          complexNode = new ScimObjectNode(schemaAttribute);
        }
        else
        {
          complexNode = (ObjectNode)resource.get(schemaAttribute.getName());
        }
        AtomicBoolean changeWasMade = new AtomicBoolean(false);
        value.fields().forEachRemaining(stringJsonNodeEntry -> {
          final String key = stringJsonNodeEntry.getKey();
          final JsonNode newValue = stringJsonNodeEntry.getValue();
          final String uri = extensionUri == null ? resourceType.getSchema() : extensionUri;
          final String fullName = uri + ":" + schemaAttribute.getName() + "." + key;
          SchemaAttribute subAttribute = RequestUtils.getSchemaAttributeByAttributeName(resourceType, fullName);
          verifyImmutableAndReadOnly(complexNode, subAttribute);
          if (!newValue.equals(complexNode.get(key)))
          {
            complexNode.set(key, newValue);
            changeWasMade.set(true);
          }
        });
        if (changeWasMade.get())
        {
          resource.set(schemaAttribute.getName(), complexNode);
        }
        return changeWasMade.get();
      }
      else
      {
        resource.set(schemaAttribute.getName(), value);
        return true;
      }
    }
  }

  /**
   * adds a new value to the specified multi valued complex type or creates the node if the node does not exist
   * yet
   *
   * @param resource the resource that should be modified
   * @param schemaAttribute the schema attribute definition
   * @param value the array node whose values should be added to the resource
   * @param extensionUri
   * @return always true
   */
  private boolean addMultiValuedComplexNode(ObjectNode resource,
                                            SchemaAttribute schemaAttribute,
                                            JsonNode value,
                                            String extensionUri)
  {
    JsonNode multiValuedComplexNode = resource.get(schemaAttribute.getName());
    ArrayNode arrayNode;
    boolean changeWasMade = true;
    if (multiValuedComplexNode == null)
    {
      arrayNode = new ScimArrayNode(schemaAttribute);
    }
    else
    {
      arrayNode = (ArrayNode)multiValuedComplexNode;
      if (PatchOp.REPLACE.equals(patchOp))
      {
        if (arrayNode.equals(value))
        {
          changeWasMade = false;
        }
        arrayNode.removeAll();
      }
    }
    Optional<JsonNode> primaryNode = getPrimaryFromMultiComplex(arrayNode);
    AtomicBoolean newPrimaryNodeDetected = new AtomicBoolean(false);
    value.forEach(complex -> {
      complex.fields().forEachRemaining(stringJsonNodeEntry -> {
        String fullName = (extensionUri == null ? resourceType.getSchema() : extensionUri) + ":"
                          + schemaAttribute.getName() + "." + stringJsonNodeEntry.getKey();
        SchemaAttribute subAttribute = RequestUtils.getSchemaAttributeByAttributeName(resourceType, fullName);
        verifyImmutableAndReadOnly(complex, subAttribute);
      });
      JsonNode prime = complex.get(AttributeNames.RFC7643.PRIMARY);
      boolean primaryFound = prime != null && prime.booleanValue();
      if (newPrimaryNodeDetected.get())
      {
        throw new BadRequestException("Found 2 primary values in the new dataset of node: "
                                      + schemaAttribute.getFullResourceName(), null, ScimType.RFC7644.INVALID_VALUE);
      }
      newPrimaryNodeDetected.weakCompareAndSet(false, primaryFound);
      arrayNode.add(complex);
    });
    if (newPrimaryNodeDetected.get())
    {
      primaryNode.ifPresent(complex -> JsonHelper.removeAttribute(complex, AttributeNames.RFC7643.PRIMARY));
    }
    resource.set(schemaAttribute.getName(), arrayNode);
    return changeWasMade;
  }

  /**
   * searches for the complex type that holds the primary=true value and returns it
   *
   * @param arrayNode the multi valued complex node
   * @return the primary node or an empty
   */
  private Optional<JsonNode> getPrimaryFromMultiComplex(ArrayNode arrayNode)
  {
    for ( JsonNode complex : arrayNode )
    {
      JsonNode primaryNode = complex.get(AttributeNames.RFC7643.PRIMARY);
      if (primaryNode != null && primaryNode.booleanValue())
      {
        return Optional.of(complex);
      }
    }
    return Optional.empty();
  }

  /**
   * adds a simple multi valued attribute to the resource
   *
   * @param resource the resource to which the simple multi valued attribute should be added
   * @param schemaAttribute the schema attribute definition
   * @param value the value(s) that should be added
   * @return always true
   */
  private boolean addMultivaluedAttribute(ObjectNode resource, SchemaAttribute schemaAttribute, JsonNode value)
  {
    ArrayNode arrayNode;
    if (resource.get(schemaAttribute.getName()) == null)
    {
      arrayNode = new ScimArrayNode(schemaAttribute);
    }
    else
    {
      arrayNode = (ArrayNode)resource.get(schemaAttribute.getName());
    }
    for ( JsonNode jsonNode : value )
    {
      arrayNode.add(jsonNode);
    }
    resource.set(schemaAttribute.getName(), arrayNode);
    return true;
  }

}
