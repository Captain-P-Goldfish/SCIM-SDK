package de.captaingoldfish.scim.sdk.server.patch;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidFilterException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimArrayNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.RequestContextException;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.ValidationContext;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexMultivaluedSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexSimpleSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedSimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.RemoveComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.RemoveExtensionRefOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.SimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.PatchWorkaround;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.exceptions.AttributeValidationException;
import de.captaingoldfish.scim.sdk.server.schemas.validation.RequestAttributeValidator;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import de.captaingoldfish.scim.sdk.server.utils.ScimAttributeHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 06.01.2024
 */
@Slf4j
@Getter
public class PatchRequestHandler<T extends ResourceNode> implements ScimAttributeHelper
{

  /**
   * the validation object that needs access to some attributes of this class
   */
  private final PatchValidations patchValidations = new PatchValidations();

  /**
   * the id of the resource that is being patched
   */
  private final String resourceId;

  /**
   * required for attribute validation
   */
  private final ServiceProvider serviceProvider;

  /**
   * the current patch configuration
   */
  private final PatchConfig patchConfig;

  /**
   * the definition of the resource that is being patched
   */
  private final ResourceType resourceType;

  /**
   * the patch-workarounds that might be applied to the different patch-operations
   */
  private final List<Supplier<PatchWorkaround>> patchWorkarounds;

  /**
   * the main-schema from the {@link #resourceType}
   */
  private final Schema mainSchema;

  /**
   * the schema-extensions from the {@link #resourceType}
   */
  private final List<Schema> extensionSchemas;

  /**
   * the validation context is used to return appropriate error messages the validation on an attribute did fail
   */
  private final ValidationContext validationContext;

  /**
   * the current request context
   */
  private final Context requestContext;

  /**
   * an object on which we will gather the attributes that were present within the patch-operations. This is
   * necessary for the eventual resource-validation to determine which attributes should be returned in the
   * response and which should not. (Handling of the
   * {@link de.captaingoldfish.scim.sdk.common.constants.enums.Returned} value)
   */
  private final ScimObjectNode requestedAttributes = new ScimObjectNode();

  /**
   * if the resource was changed or not
   */
  private boolean resourceChanged = false;

  /**
   * this is the implementation that handles a single patch-operation on a resource
   */
  private PatchOperationHandler<T> patchOperationHandler;

  public PatchRequestHandler(String resourceId,
                             ResourceHandler resourceHandler,
                             List<Supplier<PatchWorkaround>> patchWorkarounds,
                             Context context)
  {
    this.resourceId = resourceId;
    this.serviceProvider = resourceHandler.getServiceProvider();
    this.patchConfig = resourceHandler.getServiceProvider().getPatchConfig();
    this.resourceType = resourceHandler.getResourceType();
    this.patchWorkarounds = patchWorkarounds;
    this.mainSchema = resourceType.getMainSchema();
    this.extensionSchemas = resourceType.getAllSchemaExtensions();
    this.patchOperationHandler = resourceHandler.getPatchOpResourceHandler(resourceId, context);
    this.validationContext = new ValidationContext(resourceType);
    this.requestContext = context;
  }

  public PatchRequestHandler(String resourceId,
                             ServiceProvider serviceProviderConfig,
                             ResourceType resourceType,
                             PatchOperationHandler<T> patchOperationHandler)
  {
    this(resourceId, serviceProviderConfig, resourceType, patchOperationHandler, null);
  }

  public PatchRequestHandler(String resourceId,
                             ServiceProvider serviceProviderConfig,
                             ResourceType resourceType,
                             PatchOperationHandler<T> patchOperationHandler,
                             Context context)
  {
    this.resourceId = resourceId;
    this.serviceProvider = serviceProviderConfig;
    this.patchConfig = serviceProviderConfig.getPatchConfig();
    this.resourceType = resourceType;
    this.patchWorkarounds = Collections.emptyList();
    this.mainSchema = resourceType.getMainSchema();
    this.extensionSchemas = resourceType.getAllSchemaExtensions();
    this.patchOperationHandler = patchOperationHandler;
    this.validationContext = new ValidationContext(resourceType);
    this.requestContext = context;
  }

  /**
   * delegate method to {@link PatchOperationHandler#getOldResourceSupplier(String, List, List)}
   */
  public Supplier<T> getOldResourceSupplier(String id,
                                            List<SchemaAttribute> attributes,
                                            List<SchemaAttribute> excludedAttributes)
  {
    return patchOperationHandler.getOldResourceSupplier(id, attributes, excludedAttributes, requestContext);
  }

  /**
   * delegate method to {@link PatchOperationHandler#getUpdatedResource(String, ResourceNode, boolean)}
   */
  public ResourceNode getUpdatedResource(T validatedPatchedResource,
                                         List<SchemaAttribute> attributes,
                                         List<SchemaAttribute> excludedAttributes)
  {
    return patchOperationHandler.getUpdatedResource(resourceId,
                                                    validatedPatchedResource,
                                                    resourceChanged,
                                                    attributes,
                                                    excludedAttributes,
                                                    requestContext);
  }

  /**
   * the main-method of this handler that will handle a complete patch-request
   *
   * @param patchOpRequest the patch-request that should be handled
   * @return the patched and updated resource
   */
  public T handlePatchRequest(PatchOpRequest patchOpRequest)
  {
    for ( PatchRequestOperation operation : patchOpRequest.getOperations() )
    {
      handleSinglePatchOperation(operation);
    }

    if (validationContext.hasErrors())
    {
      validationContext.logErrors();
      throw new RequestContextException(validationContext);
    }

    T resource = patchOperationHandler.getPatchedResource(resourceId);
    if (resourceChanged)
    {
      Optional<Meta> metaOptional = resource.getMeta();
      if (metaOptional.isPresent())
      {
        metaOptional.get().setLastModified(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
      }
      else
      {
        Meta meta = Meta.builder().lastModified(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)).build();
        resource.setMeta(meta);
      }
    }
    return resource;
  }

  /**
   * handles a single patch operation from the patch-request that might consist of several patch-operations
   *
   * @param patchRequestOperation the patch-operation to handle
   */
  private void handleSinglePatchOperation(PatchRequestOperation patchRequestOperation)
  {
    final boolean isPathPresent = patchRequestOperation.getPath().isPresent();
    try
    {
      if (isPathPresent)
      {
        PatchRequestOperation fixedOperation = applyWorkaroundsToPatchOperation(patchRequestOperation);
        patchValidations.validateRemoveOperation(true, fixedOperation);

        String path = fixedOperation.getPath().orElse(null);
        Optional<Schema> extensionSchema = resourceType.getExtensionById(path);

        if (extensionSchema.isPresent())
        {
          resourceChanged = handlePatchOnExtension(extensionSchema.get(), fixedOperation) || resourceChanged;
        }
        else
        {

          PatchPathHandler patchPathHandler = new PatchPathHandler();
          resourceChanged = patchPathHandler.handlePathOperation(fixedOperation) || resourceChanged;
        }
      }
      else
      {
        patchValidations.validateRemoveOperation(false, patchRequestOperation);
        PatchResourceHandler patchResourceHandler = new PatchResourceHandler();
        resourceChanged = patchResourceHandler.handleMainResource(mainSchema, patchRequestOperation) || resourceChanged;
      }
    }
    catch (AttributeValidationException ex)
    {
      validationContext.addExceptionMessages(ex);
    }
    catch (IgnoreWholeOperationException | IgnoreSingleAttributeException ex)
    {
      // do nothing
      log.trace(ex.getMessage(), ex);
    }
  }

  /**
   * this method will apply the configured workarounds to the given patch-operation if the patch-operation
   * qualifies for one of the workarounds
   *
   * @param patchRequestOperation the patch operation to which a workaround might be applied
   * @return the fixed patch-operation
   */
  private PatchRequestOperation applyWorkaroundsToPatchOperation(PatchRequestOperation patchRequestOperation)
  {
    PatchRequestOperation fixedOperation = patchRequestOperation;
    for ( Supplier<PatchWorkaround> patchWorkaroundSupplier : patchWorkarounds )
    {
      PatchWorkaround patchWorkaround = patchWorkaroundSupplier.get();
      if (patchWorkaround.shouldBeHandled(patchConfig, resourceType, fixedOperation))
      {
        fixedOperation = patchWorkaround.fixPatchRequestOperaton(resourceType, fixedOperation);
        if (!patchWorkaround.executeOtherHandlers())
        {
          break;
        }
      }
    }
    return fixedOperation;
  }

  /**
   * translates the path-attribute from the patch-request into an attribute-path that may contain a
   * filter-expression if a multivalued complex attribute is referenced
   *
   * @throws IgnoreWholeOperationException if the path cannot be resolved and if unknown paths should not cause
   *           errors
   * @throws InvalidFilterException if the path cannot be resolved and unknown paths should cause errors
   */
  private AttributePathRoot getAttributePath(PatchRequestOperation fixedOperation)
  {
    try
    {
      return RequestUtils.parsePatchPath(resourceType, fixedOperation.getPath().orElse(null));
    }
    catch (InvalidFilterException ex)
    {
      if (patchConfig.isIgnoreUnknownAttribute())
      {
        log.debug("Ignoring invalid path '{}'", fixedOperation.getPath());
        throw new IgnoreWholeOperationException();
      }
      else
      {
        throw new BadRequestException(ex.getMessage(), ex);
      }
    }
  }

  /**
   * this method will add an attribute from a patch operation to the requested attributes object
   * {@link #requestedAttributes}. This is used in the
   * {@link de.captaingoldfish.scim.sdk.server.schemas.validation.ResponseResourceValidator} to identify the
   * attributes that should be returned to the client and which attributes should be removed from the response
   *
   * @param schemaAttribute the definition of the attribute that should be added to the object
   */
  private void addAttributeToRequestedAttributes(SchemaAttribute schemaAttribute)
  {
    if (schemaAttribute == null)
    {
      return;
    }
    if (schemaAttribute.getParent() == null)
    {
      if (Type.COMPLEX.equals(schemaAttribute.getType()))
      {
        requestedAttributes.set(schemaAttribute.getName(), new ObjectNode(JsonNodeFactory.instance));
      }
      else
      {
        requestedAttributes.set(schemaAttribute.getName(), new TextNode(""));
      }
    }
    else
    {
      final String parentName = schemaAttribute.getParent().getName();
      ObjectNode complexNode = (ObjectNode)Optional.ofNullable(requestedAttributes.get(parentName)).orElseGet(() -> {
        ObjectNode objectNode = new ScimObjectNode();
        requestedAttributes.set(parentName, objectNode);
        return objectNode;
      });
      JsonNode attribute = complexNode.get(schemaAttribute.getName());
      if (attribute == null)
      {
        // the value does not matter here. We only need the key in the document
        complexNode.set(schemaAttribute.getName(), new TextNode(""));
      }
    }
  }

  /**
   * executes a patch operation on a direct extension reference. Meaning the path-attribute in the patch
   * operation identifies the extension itself instead of an attribute within the extension
   *
   * @param schema the extension schema that is referenced
   * @param fixedOperation the patch operation to apply to the extension
   */
  private boolean handlePatchOnExtension(Schema schema, PatchRequestOperation fixedOperation)
  {
    // remove the whole extension
    if (PatchOp.REMOVE.equals(fixedOperation.getOp()))
    {
      return patchOperationHandler.handleOperation(resourceId,
                                                   new RemoveExtensionRefOperation(schema, fixedOperation.getOp()));
    }

    if (fixedOperation.getValues().size() > 1)
    {
      throw new BadRequestException(String.format("Patch request contains too many values. Expected a "
                                                  + "single value representing an extension but got several. '%s'",
                                                  fixedOperation.getValues()));
    }
    String value = fixedOperation.getValues().get(0);
    ObjectNode extension = (ObjectNode)JsonHelper.readJsonDocument(value);
    if (extension == null)
    {
      String errorMessage = String.format("Received invalid data on patch values. Expected an extension "
                                          + "resource but got: '%s'",
                                          value);
      throw new BadRequestException(errorMessage);
    }
    PatchResourceHandler patchResourceHandler = new PatchResourceHandler();
    boolean changeMade = patchResourceHandler.handleExtensionResource(schema, extension, fixedOperation.getOp());
    final String path = schema.getNonNullId();
    requestedAttributes.set(path, extension);
    return changeMade;
  }

  private static class IgnoreWholeOperationException extends RuntimeException
  {}

  private static class IgnoreSingleAttributeException extends RuntimeException
  {}

  private abstract class AbstractPatchOperationHandler
  {

    /**
     * handles a resource-field from the currently processed resource. The processed resource is either a
     * non-multivalued complex node, an extension or the main-resource
     *
     * @param schemaAttribute the currently handled attribute
     * @param patchOp the patch operation to handle
     * @param resourceField the current resource-field that is represented by the schemaAttribute
     * @return true if the resource was effectively changed, false else
     */
    protected boolean handleSingleResourceField(SchemaAttribute schemaAttribute,
                                                PatchOp patchOp,
                                                Map.Entry<String, JsonNode> resourceField)
    {
      final String fieldName = resourceField.getKey();
      if (schemaAttribute == null)
      {
        if (patchConfig.isIgnoreUnknownAttribute())
        {
          log.debug("Ignoring unknown attribute '{}'", fieldName);
          return false;
        }
        else
        {
          throw new BadRequestException(String.format("Attribute '%s' is unknown to resource type '%s'",
                                                      fieldName,
                                                      resourceType.getName()),
                                        ScimType.RFC7644.INVALID_PATH);
        }
      }

      JsonNode attributeValue = resourceField.getValue();

      if (schemaAttribute.isMultivaluedComplexAttribute())
      {
        attributeValue = patchValidations.validateCurrentAttribute(schemaAttribute, resourceField);
        return handleMultivaluedComplexAttribute(schemaAttribute, patchOp, attributeValue);
      }
      else if (schemaAttribute.isMultiValued())
      {
        attributeValue = patchValidations.validateCurrentAttribute(schemaAttribute, resourceField);
        return handleSimpleMultivaluedAttribute(schemaAttribute, patchOp, attributeValue);
      }
      else if (schemaAttribute.isComplexAttribute())
      {
        // the handleComplexAttribute method is recalling this method with its sub-attributes so the sub-attributes
        // will be validated implicitly
        return handleComplexAttribute(schemaAttribute, patchOp, attributeValue);
      }
      else
      {
        if (schemaAttribute.isChildOfMultivaluedComplexAttribute())
        {
          // this happens in case that the msAzure notation was used. e.g. { "emails.value": "test@test.de" }
          ObjectNode objectNode = new ScimObjectNode(schemaAttribute.getParent());
          objectNode.set(schemaAttribute.getName(), attributeValue);
          return handleMultivaluedComplexAttribute(schemaAttribute.getParent(), patchOp, objectNode);
        }
        else
        {
          attributeValue = patchValidations.validateCurrentAttribute(schemaAttribute, resourceField);
          return handleSimpleAttribute(schemaAttribute, patchOp, attributeValue);
        }
      }
    }

    /**
     * handles a multivalued complex sub-attribute
     *
     * @return true if the resource was effectively changed, false else
     */
    protected boolean handleMultivaluedComplexSubAttribute(SchemaAttribute multiComplexAttribute,
                                                           PatchOp patchOp,
                                                           JsonNode attributeValue)
    {
      AttributePathRoot attributePath = new AttributePathRoot(multiComplexAttribute);
      if (attributeValue == null || attributeValue.isNull())
      {
        // null values are considered implicit remove operations
        MultivaluedComplexMultivaluedSubAttributeOperation multiComplexOperation = //
          new MultivaluedComplexMultivaluedSubAttributeOperation(attributePath, attributePath.getSubAttribute(),
                                                                 PatchOp.REMOVE);
        return patchOperationHandler.handleOperation(resourceId, multiComplexOperation);
      }
      SchemaAttribute schemaAttribute = attributePath.getDirectlyReferencedAttribute();
      if (schemaAttribute.isMultiValued())
      {
        ArrayNode arrayNode;
        if (attributeValue instanceof ArrayNode)
        {
          arrayNode = (ArrayNode)attributeValue;
        }
        else
        {
          arrayNode = new ScimArrayNode(multiComplexAttribute);
          arrayNode.add(attributeValue);
        }
        arrayNode = (ArrayNode)patchValidations.validateCurrentAttribute(multiComplexAttribute,
                                                                         new AbstractMap.SimpleEntry<>(schemaAttribute.getName(),
                                                                                                       arrayNode));
        addAttributeToRequestedAttributes(schemaAttribute);
        MultivaluedComplexMultivaluedSubAttributeOperation multiComplexOperation = //
          new MultivaluedComplexMultivaluedSubAttributeOperation(attributePath, schemaAttribute, patchOp, arrayNode);
        return patchOperationHandler.handleOperation(resourceId, multiComplexOperation);
      }
      else
      {
        JsonNode value = patchValidations.validateCurrentAttribute(multiComplexAttribute,
                                                                   new AbstractMap.SimpleEntry<>(schemaAttribute.getName(),
                                                                                                 attributeValue));
        addAttributeToRequestedAttributes(schemaAttribute);
        MultivaluedComplexSimpleSubAttributeOperation multiComplexOperation = //
          new MultivaluedComplexSimpleSubAttributeOperation(attributePath, schemaAttribute, patchOp, value);
        return patchOperationHandler.handleOperation(resourceId, multiComplexOperation);
      }
    }

    /**
     * handles a multivalued complex attribute
     *
     * @return true if the resource was effectively changed, false else
     */
    private boolean handleMultivaluedComplexAttribute(SchemaAttribute multiComplexAttribute,
                                                      PatchOp patchOp,
                                                      JsonNode attributeValue)
    {
      AttributePathRoot attributePath = new AttributePathRoot(multiComplexAttribute);
      patchValidations.validateMutability(attributePath.getDirectlyReferencedAttribute());

      if (attributeValue == null || attributeValue.isNull())
      {
        // null values are considered implicit remove operations
        MultivaluedComplexAttributeOperation multiComplexOperation = //
          new MultivaluedComplexAttributeOperation(attributePath, PatchOp.REMOVE);
        return patchOperationHandler.handleOperation(resourceId, multiComplexOperation);
      }
      ArrayNode arrayNode;
      if (attributeValue instanceof ArrayNode)
      {
        arrayNode = (ArrayNode)attributeValue;
      }
      else
      {
        arrayNode = new ScimArrayNode(multiComplexAttribute);
        arrayNode.add(attributeValue);
      }
      arrayNode = (ArrayNode)patchValidations.validateCurrentAttribute(multiComplexAttribute,
                                                                       new AbstractMap.SimpleEntry<>(multiComplexAttribute.getName(),
                                                                                                     arrayNode));
      addAttributeToRequestedAttributes(multiComplexAttribute);
      MultivaluedComplexAttributeOperation multiComplexOperation = //
        new MultivaluedComplexAttributeOperation(attributePath, patchOp, arrayNode);
      return patchOperationHandler.handleOperation(resourceId, multiComplexOperation);
    }

    /**
     * handles a simple multivalued attribute.
     *
     * @return true if the resource was effectively changed, false else
     */
    private boolean handleSimpleMultivaluedAttribute(SchemaAttribute schemaAttribute,
                                                     PatchOp patchOp,
                                                     JsonNode attributeValue)
    {
      patchValidations.validateMutability(schemaAttribute);
      AttributePathRoot attributePath = new AttributePathRoot(schemaAttribute);
      if (attributeValue == null || attributeValue.isNull())
      {
        // null values are considered implicit remove operations
        return patchOperationHandler.handleOperation(resourceId,
                                                     new MultivaluedSimpleAttributeOperation(attributePath,
                                                                                             PatchOp.REMOVE));
      }
      addAttributeToRequestedAttributes(schemaAttribute);
      return patchOperationHandler.handleOperation(resourceId,
                                                   new MultivaluedSimpleAttributeOperation(attributePath, patchOp,
                                                                                           (ArrayNode)attributeValue));
    }

    /**
     * handles a complex attribute
     *
     * @return true if the resource was effectively changed, false else
     */
    private boolean handleComplexAttribute(SchemaAttribute complexAttribute, PatchOp patchOp, JsonNode jsonNode)
    {
      JsonNode attributeValue = jsonNode;
      if (PatchOp.REMOVE.equals(patchOp) || attributeValue == null || attributeValue.isNull())
      {
        patchValidations.validateMutability(complexAttribute);
        AttributePathRoot attributePath = new AttributePathRoot(complexAttribute);
        // null values are considered implicit remove operations
        return patchOperationHandler.handleOperation(resourceId,
                                                     new RemoveComplexAttributeOperation(attributePath,
                                                                                         PatchOp.REMOVE));
      }
      errorIfBlock: if (!attributeValue.isObject())
      {
        if (attributeValue.isArray() && attributeValue.size() == 1)
        {
          if (attributeValue.get(0).isObject())
          {
            attributeValue = attributeValue.get(0);
            break errorIfBlock;
          }
          else if (attributeValue.get(0).isTextual())
          {
            attributeValue = JsonHelper.readJsonDocument(attributeValue.get(0).textValue());
            if (attributeValue != null && attributeValue.isObject())
            {
              break errorIfBlock;
            }
          }
        }
        throw new BadRequestException(String.format("Value for attribute '%s' must be an object but was '%s'",
                                                    complexAttribute.getFullResourceName(),
                                                    attributeValue));
      }
      ObjectNode complexNode = (ObjectNode)attributeValue;
      AtomicReference<Boolean> wasChanged = new AtomicReference<>(false);

      Schema schema = complexAttribute.getSchema();
      complexNode.fields().forEachRemaining(complexField -> {
        try
        {
          final String attributeName = String.format("%s.%s",
                                                     complexAttribute.getFullResourceName(),
                                                     complexField.getKey());
          SchemaAttribute subAttribute = schema.getSchemaAttribute(attributeName);
          if (subAttribute == null)
          {
            if (patchConfig.isIgnoreUnknownAttribute())
            {
              log.debug("Ignoring unknown attribute '{}'", attributeName);
              throw new IgnoreSingleAttributeException();
            }
            else
            {
              throw new BadRequestException(String.format("Attribute '%s' is unknown to resource type '%s'",
                                                          attributeName,
                                                          resourceType.getName()),
                                            ScimType.RFC7644.INVALID_PATH);
            }
          }
          boolean wasValueChanged = handleSingleResourceField(subAttribute, patchOp, complexField) || wasChanged.get();
          wasChanged.compareAndSet(false, wasValueChanged);
        }
        catch (IgnoreSingleAttributeException ex)
        {
          log.debug("Ignoring attribute '{}' with value '{}'", complexField.getKey(), complexField.getValue());
          log.trace(ex.getMessage(), ex);
        }
      });
      return wasChanged.get();
    }

    /**
     * handles a simple attribute that is neither complex nor multivalued
     *
     * @return true if the resource was effectively changed, false else
     */
    private boolean handleSimpleAttribute(SchemaAttribute schemaAttribute, PatchOp patchOp, JsonNode attributeValue)
    {
      patchValidations.validateMutability(schemaAttribute);
      AttributePathRoot attributePath = new AttributePathRoot(schemaAttribute);
      if (attributeValue == null || attributeValue.isNull())
      {
        // null values are considered implicit remove operations
        return patchOperationHandler.handleOperation(resourceId,
                                                     new SimpleAttributeOperation(attributePath, PatchOp.REMOVE));
      }
      addAttributeToRequestedAttributes(schemaAttribute);
      return patchOperationHandler.handleOperation(resourceId,
                                                   new SimpleAttributeOperation(attributePath, patchOp,
                                                                                attributeValue));
    }
  }

  private class PatchPathHandler extends AbstractPatchOperationHandler
  {

    private boolean handlePathOperation(PatchRequestOperation patchOperation)
    {
      final AttributePathRoot attributePath = getAttributePath(patchOperation);
      final SchemaAttribute schemaAttribute = attributePath.getDirectlyReferencedAttribute();
      final PatchOp patchOp = patchOperation.getOp();
      JsonNode valueNode = patchOperation.getValue().orElse(null);

      if (attributePath.isWithFilter())
      {
        valueNode = patchValidations.validateCurrentAttribute(schemaAttribute,
                                                              new AbstractMap.SimpleEntry<>(schemaAttribute.getName(),
                                                                                            valueNode));
        addAttributeToRequestedAttributes(schemaAttribute);
        if (attributePath.isWithSubAttributeRef() || schemaAttribute.isChildOfComplexAttribute())
        {
          if (schemaAttribute.isMultiValued())
          {
            return patchOperationHandler.handleOperation(resourceId,
                                                         new MultivaluedComplexMultivaluedSubAttributeOperation(attributePath,
                                                                                                                schemaAttribute,
                                                                                                                patchOp,
                                                                                                                valueNode));
          }
          else
          {
            return patchOperationHandler.handleOperation(resourceId,
                                                         new MultivaluedComplexSimpleSubAttributeOperation(attributePath,
                                                                                                           schemaAttribute,
                                                                                                           patchOp,
                                                                                                           valueNode));
          }
        }
        else
        {
          if (schemaAttribute.isComplexAttribute())
          {
            return patchOperationHandler.handleOperation(resourceId,
                                                         new MultivaluedComplexAttributeOperation(attributePath,
                                                                                                  patchOp, valueNode));
          }
          else
          {
            return patchOperationHandler.handleOperation(resourceId,
                                                         new MultivaluedSimpleAttributeOperation(attributePath, patchOp,
                                                                                                 (ArrayNode)valueNode));
          }
        }
      }

      if (schemaAttribute.isChildOfMultivaluedComplexAttribute())
      {
        valueNode = patchValidations.validateCurrentAttribute(schemaAttribute,
                                                              new AbstractMap.SimpleEntry<>(schemaAttribute.getName(),
                                                                                            valueNode));
        return handleMultivaluedComplexSubAttribute(schemaAttribute, patchOp, valueNode);
      }

      return handleSingleResourceField(schemaAttribute,
                                       patchOp,
                                       new AbstractMap.SimpleEntry<>(schemaAttribute.getName(), valueNode));
    }

  }

  private class PatchResourceHandler extends AbstractPatchOperationHandler
  {

    /**
     * this handler is used to translate a resource-patch-operation into several operations having the
     * path-attribute set. So a patch operation like this:
     *
     * <pre>
     * {
     *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
     *    "Operations" : [
     *        {
     *          "op" : "add/replace",
     *          "value": {
     *            "timezone": "Europe/Berlin",
     *            "preferredLanguage": "de",
     *            "name": {
     *              "givenName": "Chuck",
     *              "familyName": "Norris"
     *            },
     *            emails: [
     *              {
     *                "value": "chuck@norris.com"
     *              }
     *            ]
     *          }
     *        }
     *     ]
     *  }
     * </pre>
     *
     * will be translated to:
     *
     * <pre>
     * {
     *    "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
     *    "Operations" : [
     *        {
     *          "op" : "add/replace",
     *          "path": "timezone",
     *          "value": "Europe/Berlin"
     *        },
     *        {
     *          "op" : "add/replace",
     *          "path": "preferredLanguage"
     *          "value": "de"
     *        },
     *        {
     *          "op" : "add/replace",
     *          "path": "name.givenName"
     *          "value": "Chuck"
     *        },
     *        {
     *          "op" : "add/replace",
     *          "path": "name.familyName"
     *          "value": "Norris"
     *        },
     *        {
     *          "op" : "add/replace",
     *          "path": "emails"
     *          "value": [
     *            {
     *              "value": "chuck@norris.com"
     *            }
     *          ]
     *        },
     *     ]
     *  }
     * </pre>
     */
    private boolean handleMainResource(Schema schema, PatchRequestOperation patchRequestOperation)
    {
      validateRequest(patchRequestOperation);

      List<String> values = patchRequestOperation.getValues();
      ObjectNode resourceNode;
      try
      {
        JsonNode jsonNode = JsonHelper.readJsonDocument(values.get(0));
        if (jsonNode.isObject())
        {
          resourceNode = (ObjectNode)jsonNode;
        }
        else
        {
          throw new IllegalStateException();
        }
      }
      catch (Exception ex)
      {
        throw new BadRequestException("The resourceNode is not a valid JSON-object", ex);
      }
      // remove the schemas-attribute if present, it is just in the way
      resourceNode.remove(AttributeNames.RFC7643.SCHEMAS);
      final PatchOp patchOp = patchRequestOperation.getOp();

      AtomicReference<Boolean> wasChanged = new AtomicReference<>(false);
      resourceNode.fields().forEachRemaining(field -> {
        if (field.getKey().equals(AttributeNames.RFC7643.META))
        {
          return; // do not handle the meta attribute here
        }
        // if the field is referencing an extension
        // the pair here is used to determine if we have an unknown attribute or a direct-schema-reference:
        // urn:ietf:params:scim:schemas:extension:enterprise:2.0:User (direct schema-reference)
        // urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:blubb (unknown attribute)
        Optional<Pair<Schema, Boolean>> matchingExtension = extensionSchemas.stream().filter(s -> {
          // this might look like this:
          // urn:ietf:params:scim:schemas:extension:enterprise:2.0:User
          // or this
          // urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value
          return field.getKey().startsWith(s.getNonNullId());
        }).map(s -> Pair.of(s, s.getNonNullId().equals(field.getKey()))).findAny();

        boolean isMainResourceAttribute = !matchingExtension.isPresent();

        boolean wasValueChanged;
        try
        {
          if (isMainResourceAttribute)
          {
            SchemaAttribute schemaAttribute = schema.getSchemaAttribute(field.getKey());
            wasValueChanged = handleSingleResourceField(schemaAttribute, patchOp, field) || wasChanged.get();
            addAttributeToRequestedAttributes(schemaAttribute);
          }
          else
          {
            final Schema extensionSchema = matchingExtension.get().getKey();
            final boolean isDirectExtensionReference = matchingExtension.get().getValue();
            if (isDirectExtensionReference)
            {
              // this is an extension resource object within the resource
              // urn:ietf:params:scim:schemas:extension:enterprise:2.0:User: {...}
              JsonNode extensionNode = resourceNode.get(extensionSchema.getNonNullId());
              wasValueChanged = handleExtensionResource(extensionSchema, extensionNode, patchOp);
            }
            else
            {
              // this is an extension-attribute reference notation as used by msAzure:
              // urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter: "value"
              SchemaAttribute extensionAttribute = extensionSchema.getSchemaAttribute(field.getKey());
              wasValueChanged = handleSingleResourceField(extensionAttribute, patchOp, field);
            }
          }
          wasChanged.compareAndSet(false, wasValueChanged);
        }
        catch (IgnoreSingleAttributeException ex)
        {
          log.debug("Ignoring attribute '{}' with value '{}'", field.getKey(), field.getValue());
          log.trace(ex.getMessage(), ex);
        }
      });
      return wasChanged.get();
    }

    /**
     * handles an extension that is present within the main-resource
     *
     * @param extensionSchema the schema of the extension
     * @param jsonNode the representation of the extension from the main-resource. Should be an object.
     * @param patchOp the patch-operation to execute
     * @return true if the resource was effectively changed, false else
     */
    private boolean handleExtensionResource(Schema extensionSchema, JsonNode jsonNode, PatchOp patchOp)
    {
      if (jsonNode.isNull() || jsonNode.isEmpty())
      {
        // a null node is considered a remove-operation
        return patchOperationHandler.handleOperation(resourceId,
                                                     new RemoveExtensionRefOperation(extensionSchema, PatchOp.REMOVE));
      }
      ObjectNode extensionNode = (ObjectNode)jsonNode;

      ObjectNode requestedExtensionAttributes = new ObjectNode(JsonNodeFactory.instance);

      AtomicReference<Boolean> wasChanged = new AtomicReference<>(false);
      extensionNode.fields().forEachRemaining(extensionField -> {
        SchemaAttribute extensionAttribute = extensionSchema.getSchemaAttribute(extensionField.getKey());
        boolean wasValueChanged = handleSingleResourceField(extensionAttribute, patchOp, extensionField)
                                  || wasChanged.get();
        wasChanged.compareAndSet(false, wasValueChanged);
        if (extensionAttribute != null)
        {
          // the value is irrelevant. Just the name of the attribute must be present in the resource
          requestedExtensionAttributes.set(extensionAttribute.getName(), NullNode.getInstance());
        }
      });

      requestedAttributes.set(extensionSchema.getNonNullId(), requestedExtensionAttributes);
      return wasChanged.get();
    }

    /**
     * checks that the values-attribute contains only a single value. If no path is present in the patch-operation
     * the value must represent the resource itself
     */
    private void validateRequest(PatchRequestOperation patchRequestOperation)
    {
      List<String> values = patchRequestOperation.getValues();
      if (values.size() != 1)
      {
        throw new BadRequestException("Patch operation without a path must contain only a single value that represents "
                                      + "the resource itself", ScimType.RFC7644.INVALID_VALUE);
      }
    }
  }

  /**
   * this class contains utility-methods to validate the patch operations content
   */
  private class PatchValidations
  {

    /**
     * checks that the remove operation is correctly setup
     *
     * @param patchRequestOperation the current patch request operation
     */
    private void validateRemoveOperation(boolean isPathPresent, PatchRequestOperation patchRequestOperation)
    {
      boolean isRemoveOperation = PatchOp.REMOVE.equals(patchRequestOperation.getOp());
      if (!isRemoveOperation)
      {
        return;
      }
      if (!isPathPresent)
      {
        throw new BadRequestException("Missing target for remove operation", ScimType.RFC7644.NO_TARGET);
      }
      JsonNode valueNode = patchRequestOperation.getValueNode().orElse(null);
      if (valueNode != null && !valueNode.isNull())
      {
        throw new BadRequestException(String.format("Values must not be set for remove operation but was: %s",
                                                    valueNode),
                                      ScimType.RFC7644.INVALID_VALUE);
      }
    }

    /**
     * executes the schema-validation on the currently handled attribute
     *
     * @param schemaAttribute the attribute that is handled
     * @param resourceField the field representation from the JSON resource
     * @return the validated resourceNode (may have filtered and removed unknown attributes)
     */
    private JsonNode validateCurrentAttribute(SchemaAttribute schemaAttribute,
                                              Map.Entry<String, JsonNode> resourceField)
    {
      if (schemaAttribute.isReadOnly())
      {
        // we will ignore readOnly attributes
        throw new IgnoreSingleAttributeException();
      }
      JsonNode attributeValue = resourceField.getValue();
      if (attributeValue == null || attributeValue.isNull())
      {
        return attributeValue;
      }
      JsonNode validatedNode = RequestAttributeValidator.validateAttribute(serviceProvider,
                                                                           schemaAttribute,
                                                                           attributeValue,
                                                                           HttpMethod.PATCH)
                                                        .orElse(NullNode.getInstance());
      if (schemaAttribute.isMultivaluedComplexAttribute())
      {

        ArrayNode arrayNode;
        if (validatedNode.isArray())
        {
          arrayNode = (ArrayNode)validatedNode;
        }
        else
        {
          arrayNode = new ArrayNode(JsonNodeFactory.instance);
          arrayNode.add(validatedNode);
        }
        for ( JsonNode complexNode : arrayNode )
        {
          for ( SchemaAttribute subAttribute : schemaAttribute.getSubAttributes() )
          {
            RequestAttributeValidator.validateAttribute(serviceProvider,
                                                        subAttribute,
                                                        complexNode.get(subAttribute.getName()),
                                                        HttpMethod.PATCH);
          }
        }
      }
      return validatedNode;
    }

    /**
     * validates if the given attribute is modifiable
     */
    private void validateMutability(SchemaAttribute schemaAttribute)
    {
      if (Mutability.READ_ONLY.equals(schemaAttribute.getMutability()))
      {
        throw new IgnoreSingleAttributeException();
      }
      if (schemaAttribute.getParent() != null
          && Mutability.READ_ONLY.equals(schemaAttribute.getParent().getMutability()))
      {
        throw new IgnoreSingleAttributeException();
      }
    }

  }

}
