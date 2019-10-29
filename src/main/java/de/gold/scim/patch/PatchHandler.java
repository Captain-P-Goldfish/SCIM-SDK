package de.gold.scim.patch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.ScimType;
import de.gold.scim.constants.enums.Mutability;
import de.gold.scim.constants.enums.Type;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.exceptions.NotImplementedException;
import de.gold.scim.exceptions.ScimException;
import de.gold.scim.request.PatchOpRequest;
import de.gold.scim.request.PatchRequestOperation;
import de.gold.scim.resources.ResourceNode;
import de.gold.scim.resources.base.ScimArrayNode;
import de.gold.scim.resources.base.ScimObjectNode;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.SchemaAttribute;
import de.gold.scim.utils.JsonHelper;
import de.gold.scim.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 29.10.2019 - 09:40 <br>
 * <br>
 * this class is used to resolve patch operations on resources
 */
@Slf4j
public class PatchHandler
{

  /**
   * this resource type is used to get the attribute definitions of the values from the patch operations
   */
  private final ResourceType resourceType;

  public PatchHandler(ResourceType resourceType)
  {
    this.resourceType = Objects.requireNonNull(resourceType);
  }

  /**
   * this method will execute the patch operation on the given resource
   *
   * @param resource the resource representation that should be patched
   * @param patchOpRequest the patch operation that should be executed on the resource
   * @return the patched resource
   */
  public <T extends ResourceNode> T patchResource(T resource, PatchOpRequest patchOpRequest)
  {
    for ( PatchRequestOperation operation : patchOpRequest.getOperations() )
    {
      switch (operation.getOp())
      {
        case ADD:
          addValues(resource, operation);
          break;
        case REPLACE:
          replaceValues(resource, operation);
          break;
        case REMOVE:
          removeValues(resource, operation);
          break;
      }
    }
    return resource;
  }

  /**
   * will add the given attributes to the given resource
   *
   * @param resource the resource to which the attributes should be added
   * @param operation the operation request that contains the new attributes
   */
  private void addValues(ResourceNode resource, PatchRequestOperation operation)
  {
    Optional<String> target = operation.getPath();
    List<String> values = operation.getValues();
    if (values == null || values.isEmpty())
    {
      throw new BadRequestException("no value attributes present in patch operation", null,
                                    ScimType.RFC7644.INVALID_VALUE);
    }

    if (target.isPresent())
    {
      addOperationValues(resource, operation.getValues());
    }
    else
    {
      if (values.size() > 1)
      {
        throw new BadRequestException("too many resources set in patch operation. If the target is not specified only"
                                      + " a single value must be present in the values list which represents the "
                                      + "resource itself", null, ScimType.RFC7644.INVALID_VALUE);
      }
      addResourceValues(resource, JsonHelper.readJsonDocument(values.get(0)), null);
    }
  }

  /**
   * adds the values of the patch operation into the given resource node
   *
   * @param resource the resource node into which the values should be added
   * @param readJsonDocument the patch operation resource from which the values should be added into the
   *          resource node
   * @param extensionUri this extensionUri is used for resolving extensions in the resource if name conflicts do
   *          exist we need the fully qualified name for verifying the attributes.
   */
  private void addResourceValues(ObjectNode resource, JsonNode readJsonDocument, String extensionUri)
  {
    if (readJsonDocument == null || readJsonDocument.isEmpty())
    {
      throw new BadRequestException("no attributes present in value-resource in patch operation", null,
                                    ScimType.RFC7644.INVALID_VALUE);
    }
    AtomicBoolean changeWasMade = new AtomicBoolean(false);
    readJsonDocument.fields().forEachRemaining(stringJsonNodeEntry -> {
      String key = stringJsonNodeEntry.getKey();
      JsonNode value = stringJsonNodeEntry.getValue();
      boolean isExtension = resourceType.getSchemaExtensions().stream().anyMatch(ext -> ext.getSchema().equals(key));
      if (isExtension)
      {
        JsonNode complex = resource.get(key);
        if (complex == null)
        {
          complex = new ScimObjectNode();
          resource.set(key, complex);
        }
        addResourceValues((ObjectNode)complex, value, key);
      }
      else
      {
        SchemaAttribute schemaAttribute = getSchemaAttribute((extensionUri == null ? "" : extensionUri + ":") + key);
        verifyImmutableAndReadOnly(resource, schemaAttribute);
        if (Type.COMPLEX.equals(schemaAttribute.getType()))
        {
          changeWasMade.weakCompareAndSet(false, addComplexAttribute(resource, schemaAttribute, value, extensionUri));
        }
        else if (schemaAttribute.isMultiValued())
        {
          changeWasMade.weakCompareAndSet(false, addMultivaluedAttribute(resource, schemaAttribute, value));
        }
        else
        {
          changeWasMade.weakCompareAndSet(false, addSimpleAttribute(resource, schemaAttribute, value));
        }
      }
    });
    if (ResourceNode.class.isAssignableFrom(resource.getClass()))
    {
      setLastModified((ResourceNode)resource, changeWasMade);
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
      throw new BadRequestException("attribute cannot be written it has a mutability of '"
                                    + schemaAttribute.getMutability() + "'", null, ScimType.RFC7644.MUTABILITY);
    }
  }

  /**
   * overrides the lastModified value if a change was made
   *
   * @param resource the resource of which the lastModified value should be changed
   * @param changeWasMade if the lastModified value should be changed or not
   */
  private void setLastModified(ResourceNode resource, AtomicBoolean changeWasMade)
  {
    if (changeWasMade.get())
    {
      Optional<Meta> metaOptional = resource.getMeta();
      if (metaOptional.isPresent())
      {
        metaOptional.get().setLastModified(LocalDateTime.now());
      }
      else
      {
        Meta meta = Meta.builder().lastModified(LocalDateTime.now()).build();
        resource.setMeta(meta);
      }
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
      else
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
    if (multiValuedComplexNode == null)
    {
      arrayNode = new ScimArrayNode(schemaAttribute);
    }
    else
    {
      arrayNode = (ArrayNode)multiValuedComplexNode;
    }
    Optional<JsonNode> primaryNode = getPrimaryFromMultiComplex(arrayNode);
    AtomicBoolean newPrimaryNodeDetected = new AtomicBoolean(false);
    value.forEach(complex -> {
      complex.fields().forEachRemaining(stringJsonNodeEntry -> {
        String fullName = (extensionUri == null ? "" : extensionUri + ":") + schemaAttribute.getName() + "."
                          + stringJsonNodeEntry.getKey();
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
    return true;
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

  /**
   * tries to resolve that attribute path and gets the schema of the attribute
   *
   * @param key the attribute name of which the definition should be extracted
   * @return the schema attribute or an exception if the definition does not exist for the resourceType
   */
  private SchemaAttribute getSchemaAttribute(String key)
  {
    try
    {
      return RequestUtils.getSchemaAttributeByAttributeName(resourceType, key);
    }
    catch (ScimException ex)
    {
      ex.setScimType(ScimType.RFC7644.INVALID_PATH);
      ex.setStatus(HttpStatus.BAD_REQUEST);
      throw ex;
    }
  }

  private void addOperationValues(ResourceNode resource, List<String> values)
  {

  }

  private void removeValues(ResourceNode resource, PatchRequestOperation operation)
  {
    throw new NotImplementedException("not yet");
  }

  private void replaceValues(ResourceNode resource, PatchRequestOperation operation)
  {
    throw new NotImplementedException("not yet");
  }

}
