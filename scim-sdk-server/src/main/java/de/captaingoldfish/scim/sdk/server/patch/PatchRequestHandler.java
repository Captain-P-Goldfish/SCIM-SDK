package de.captaingoldfish.scim.sdk.server.patch;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidFilterException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.AttributePathRoot;
import de.captaingoldfish.scim.sdk.server.patch.operations.ComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.ComplexSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.ExtensionRefOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedSimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.SimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.PatchWorkaround;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.validation.SimpleAttributeValidator;
import de.captaingoldfish.scim.sdk.server.schemas.validation.SimpleMultivaluedAttributeValidator;
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
   * the current patch configuration
   */
  private final PatchConfig patchConfig;

  /**
   * this resource-handler will accept the handled patch-operations
   */
  private final ResourceHandler<T> resourceHandler;

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
    this.resourceHandler = resourceHandler;
    this.patchConfig = resourceHandler.getServiceProvider().getPatchConfig();
    this.resourceType = resourceHandler.getResourceType();
    this.patchWorkarounds = patchWorkarounds;
    this.mainSchema = resourceType.getMainSchema();
    this.extensionSchemas = resourceType.getAllSchemaExtensions();
    this.patchOperationHandler = resourceHandler.getPatchOpResourceHandler(context);
  }

  /**
   * delegate method to {@link PatchOperationHandler#getOldResourceSupplier(String, List, List)}
   */
  public Supplier<T> getOldResourceSupplier(String id,
                                            List<SchemaAttribute> attributes,
                                            List<SchemaAttribute> excludedAttributes)
  {
    return patchOperationHandler.getOldResourceSupplier(id, attributes, excludedAttributes);
  }

  /**
   * delegate method to {@link PatchOperationHandler#getUpdatedResource(String, ResourceNode, boolean)}
   */
  public ResourceNode getUpdatedResource(T validatedPatchedResource)
  {
    return patchOperationHandler.getUpdatedResource(resourceId, validatedPatchedResource, resourceChanged);
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
    if (PatchOp.REMOVE.equals(patchRequestOperation.getOp()) && !isPathPresent)
    {
      throw new BadRequestException("Missing target for remove operation", ScimType.RFC7644.NO_TARGET);
    }
    try
    {
      if (isPathPresent)
      {
        PatchRequestOperation fixedOperation = applyWorkaroundsToPatchOperation(patchRequestOperation);

        String path = fixedOperation.getPath().orElse(null);
        Optional<Schema> extensionSchema = resourceType.getExtensionById(path);

        if (extensionSchema.isPresent())
        {
          handlePatchOnExtension(extensionSchema.get(), fixedOperation);
        }
        else
        {
          PatchPathHandler patchPathHandler = new PatchPathHandler();
          patchPathHandler.handlePatchWithPath(fixedOperation);
        }
      }
      else
      {
        PatchResourceHandler patchResourceHandler = new PatchResourceHandler();
        resourceChanged = patchResourceHandler.handlePatchResourceOperations(patchRequestOperation) || resourceChanged;
      }
    }
    catch (IgnoreOperationException ex)
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
   * @throws IgnoreOperationException if the path cannot be resolved and if unknown paths should not cause
   *           errors
   * @throws InvalidFilterException if the path cannot be resolved and unknown paths should cause errors
   */
  private AttributePathRoot getAttributePath(PatchRequestOperation fixedOperation)
  {
    try
    {
      return RequestUtils.parsePatchPath(resourceHandler.getResourceType(), fixedOperation.getPath().orElse(null));
    }
    catch (InvalidFilterException ex)
    {
      if (patchConfig.isIgnoreUnknownAttribute())
      {
        log.debug("Ignoring invalid path '{}'", fixedOperation.getPath());
        throw new IgnoreOperationException();
      }
      else
      {
        throw ex;
      }
    }
  }

  /**
   * translates the path-attribute from the patch-request into an attribute-path that may contain a
   * filter-expression if a multivalued complex attribute is referenced
   *
   * @throws IgnoreOperationException if the path cannot be resolved and if unknown paths should not cause
   *           errors
   * @throws InvalidFilterException if the path cannot be resolved and unknown paths should cause errors
   */
  private SchemaAttribute getAttributePathByScimNodeName(String scimNodeName)
  {
    try
    {
      return RequestUtils.getSchemaAttributeByAttributeName(resourceType, scimNodeName);
    }
    catch (BadRequestException ex)
    {
      if (patchConfig.isIgnoreUnknownAttribute())
      {
        log.debug("Ignoring invalid attribute path '{}'", scimNodeName);
        throw new IgnoreOperationException();
      }
      else
      {
        throw new InvalidFilterException(String.format("Unknown attribute '%s'", scimNodeName), ex);
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
  private void handlePatchOnExtension(Schema schema, PatchRequestOperation fixedOperation)
  {
    final String path = schema.getNonNullId();
    // remove the whole extension
    if (PatchOp.REMOVE.equals(fixedOperation.getOp()))
    {
      resourceChanged = patchOperationHandler.handleOperation(resourceId,
                                                              new ExtensionRefOperation(schema, path,
                                                                                        fixedOperation.getOp(), null))
                        || resourceChanged;
      return;
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

    resourceChanged = patchOperationHandler.handleOperation(resourceId,
                                                            new ExtensionRefOperation(schema, path,
                                                                                      fixedOperation.getOp(),
                                                                                      extension))
                      || resourceChanged;
    requestedAttributes.set(path, extension);
  }

  private static class IgnoreOperationException extends RuntimeException
  {}

  private class PatchPathHandler
  {

    /**
     * will handle any patch-operation with a path-attribute
     *
     * @param fixedOperation the current patch-operation to handle
     */
    private void handlePatchWithPath(PatchRequestOperation fixedOperation)
    {
      final AttributePathRoot attributePath = getAttributePath(fixedOperation);
      final SchemaAttribute schemaAttribute = attributePath.getSchemaAttribute();
      final PatchOp patchOp = fixedOperation.getOp();
      final ArrayNode valueNode = fixedOperation.getValueNode().orElse(null);
      final List<String> values = fixedOperation.getValues();

      patchValidations.validatePath(attributePath, patchOp, values);

      final boolean changeMade;
      // a direct complex path is "name" or "emails" or "manager".
      final boolean isDirectComplexPath = attributePath.getSchemaAttribute().isComplexAttribute();
      if (isDirectComplexPath)
      {
        // an extended nodePath applies only if a filter-expression is present: emails[type eq "work"].value
        // the ".value" is the extended nodePath
        final boolean isExtendedNodePath = attributePath.getSubAttributeName() != null;
        if (isExtendedNodePath)
        {
          final String subNodeName = String.format("%s.%s",
                                                   schemaAttribute.getName(),
                                                   attributePath.getSubAttributeName());
          SchemaAttribute subAttribute = getAttributePathByScimNodeName(subNodeName);
          if (schemaAttribute.isMultiValued())
          {
            patchValidations.validatePath(attributePath, patchOp, values);
            changeMade = patchOperationHandler.handleOperation(resourceId,
                                                               new MultivaluedComplexSubAttributeOperation(attributePath,
                                                                                                           subAttribute,
                                                                                                           patchOp,
                                                                                                           valueNode,
                                                                                                           values));
          }
          else
          {
            if (subAttribute.isMultiValued())
            {
              changeMade = patchOperationHandler.handleOperation(resourceId,
                                                                 new MultivaluedComplexSubAttributeOperation(attributePath,
                                                                                                             subAttribute,
                                                                                                             patchOp,
                                                                                                             valueNode,
                                                                                                             values));
            }
            else
            {
              changeMade = patchOperationHandler.handleOperation(resourceId,
                                                                 new ComplexSubAttributeOperation(attributePath,
                                                                                                  patchOp, valueNode,
                                                                                                  values));
            }
          }
        }
        else
        {
          if (schemaAttribute.isMultiValued())
          {
            final ArrayNode arrayNode;
            if (PatchOp.REMOVE.equals(patchOp))
            {
              arrayNode = null;
            }
            else
            {
              arrayNode = fixedOperation.getValueNode().get();
            }
            changeMade = patchOperationHandler.handleOperation(resourceId,
                                                               new MultivaluedSimpleAttributeOperation(attributePath,
                                                                                                       patchOp,
                                                                                                       arrayNode,
                                                                                                       values));
          }
          else
          {
            final ObjectNode complexNode = patchValidations.validateValueOfDirectComplexAttributePath(schemaAttribute,
                                                                                                      patchOp,
                                                                                                      valueNode);
            changeMade = patchOperationHandler.handleOperation(resourceId,
                                                               new ComplexAttributeOperation(attributePath, patchOp,
                                                                                             complexNode, values));
          }
        }
      }
      else if (attributePath.getSchemaAttribute().getParent() != null)
      {
        if (attributePath.getSchemaAttribute().isMultiValued())
        {
          ArrayNode valuesNode = patchValidations.validateSimpleMultivaluedAttribute(attributePath, fixedOperation);
          changeMade = patchOperationHandler.handleOperation(resourceId,
                                                             new MultivaluedComplexSubAttributeOperation(attributePath,
                                                                                                         attributePath.getSchemaAttribute(),
                                                                                                         patchOp,
                                                                                                         valuesNode,
                                                                                                         values));
        }
        else
        {
          JsonNode value = patchValidations.validateSimpleAttribute(attributePath, fixedOperation);
          changeMade = patchOperationHandler.handleOperation(resourceId,
                                                             new ComplexSubAttributeOperation(attributePath, patchOp,
                                                                                              value, values));
        }
      }
      else
      {
        if (attributePath.getSchemaAttribute().isMultiValued())
        {
          ArrayNode valuesNode = patchValidations.validateSimpleMultivaluedAttribute(attributePath, fixedOperation);
          changeMade = patchOperationHandler.handleOperation(resourceId,
                                                             new MultivaluedSimpleAttributeOperation(attributePath,
                                                                                                     patchOp,
                                                                                                     valuesNode,
                                                                                                     values));
        }
        else
        {
          final JsonNode value;
          if (PatchOp.REMOVE.equals(patchOp))
          {
            value = null;
          }
          else
          {
            value = fixedOperation.getValueNode().get().get(0);
          }
          changeMade = patchOperationHandler.handleOperation(resourceId,
                                                             new SimpleAttributeOperation(attributePath, patchOp, value,
                                                                                          values));
        }
      }
      resourceChanged = changeMade || resourceChanged;
      addAttributeToRequestedAttributes(attributePath.getSchemaAttribute());
    }

  }

  private class PatchResourceHandler
  {

    /**
     * this handler is used to translate a resource-patch-operations into several operations having the
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
    private boolean handlePatchResourceOperations(PatchRequestOperation patchRequestOperation)
    {
      List<String> values = patchRequestOperation.getValues();
      if (values.size() != 1)
      {
        throw new BadRequestException("Patch operation without a path must contain only a single value that represents "
                                      + "the resource itself", ScimType.RFC7644.INVALID_VALUE);
      }
      ObjectNode resourceNode = (ObjectNode)JsonHelper.readJsonDocument(values.get(0));
      AtomicReference<Boolean> wasChanged = new AtomicReference<>(false);
      // iterate over each field in the resourceNode
      resourceNode.remove(AttributeNames.RFC7643.SCHEMAS);
      resourceNode.fields().forEachRemaining(resourceField -> {
        // if the field is referencing an extension
        Optional<Schema> matchingExtension = extensionSchemas.stream().filter(schema -> {
          return resourceField.getKey().startsWith(schema.getNonNullId());
        }).findAny();
        boolean isMainResourceAttribute = !matchingExtension.isPresent();

        final PatchOp patchOp = patchRequestOperation.getOp();
        final String attributeName = resourceField.getKey();

        if (isMainResourceAttribute)
        {
          SchemaAttribute schemaAttribute = mainSchema.getSchemaAttribute(attributeName);
          if (schemaAttribute == null)
          {
            if (patchConfig.isIgnoreUnknownAttribute())
            {
              log.debug("Ignoring unknown attribute '{}'", attributeName);
              return;
            }
            else
            {
              throw new BadRequestException(String.format("Unknown attribute found '%s'", attributeName),
                                            ScimType.RFC7644.INVALID_PATH);
            }
          }
          wasChanged.compareAndSet(false,
                                   handleResourceAttribute(schemaAttribute, resourceNode, resourceField, patchOp));
        }
        else
        // handle attribute of extension
        {
          ObjectNode extensionNode = (ObjectNode)resourceNode.get(matchingExtension.get().getNonNullId());
          if (extensionNode == null)
          // attribute-reference is in MsAzure style ${schemaId}:${scimNodeName}
          {
            SchemaAttribute extensionAttribute = matchingExtension.get().getSchemaAttribute(resourceField.getKey());
            if (extensionAttribute == null)
            {
              if (patchConfig.isIgnoreUnknownAttribute())
              {
                log.debug("Ignoring unknown attribute '{}'", attributeName);
                return;
              }
              else
              {
                throw new BadRequestException(String.format("Attribute '%s' is unknown to resource type '%s'",
                                                            attributeName,
                                                            resourceType.getName()),
                                              ScimType.RFC7644.INVALID_PATH);
              }
            }
            wasChanged.compareAndSet(false,
                                     handleResourceAttribute(extensionAttribute, resourceNode, resourceField, patchOp));
          }
          else
          // seems to be a normal attribute-reference: ${scimNodeName}
          {
            if (extensionNode.isEmpty())
            // if the extension node is empty we expect it to be an implicit remove-operation
            {
              boolean changed = patchOperationHandler.handleOperation(resourceId,
                                                                      new ExtensionRefOperation(matchingExtension.get(),
                                                                                                attributeName,
                                                                                                PatchOp.REMOVE, null));
              wasChanged.compareAndSet(false, changed);
            }
            else
            {
              extensionNode.fields().forEachRemaining(extensionField -> {
                SchemaAttribute extensionAttribute = matchingExtension.get()
                                                                      .getSchemaAttribute(extensionField.getKey());
                wasChanged.compareAndSet(false,
                                         handleResourceAttribute(extensionAttribute,
                                                                 resourceNode,
                                                                 extensionField,
                                                                 patchOp));
              });
            }
          }
        }
      });
      return wasChanged.get();
    }

    /**
     * possible representations that must be handled <br />
     * <b>SCIM RFC references</b>
     *
     * <pre>
     *   {
     *     "nickName": "goldfish"
     *   }
     * </pre>
     *
     * <pre>
     *   {
     *     "name": {
     *       "givenName": "mario"
     *     }
     *   }
     * </pre>
     *
     * <pre>
     *   {
     *     "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
     *       "manager": {
     *         "value": "123456789",
     *         "display": "John"
     *       }
     *     }
     *   }
     * </pre>
     *
     * <pre>
     *   {
     *     "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
     *       "costCenter": "costCenter"
     *     }
     *   }
     * </pre>
     *
     * <b>MsAzure style references</b>
     *
     * <pre>
     *   {
     *     "name.givenName": "mario"
     *   }
     * </pre>
     *
     * <pre>
     *   {
     *     "urn:ietf:params:scim:schemas:core:2.0:User:name.givenName": "mario"
     *   }
     * </pre>
     *
     * <pre>
     *   {
     *     "urn:ietf:params:scim:schemas:core:2.0:User:name": {
     *        "givenName": "mario"
     *     }
     *   }
     * </pre>
     *
     * <pre>
     *   {
     *     "urn:ietf:params:scim:schemas:core:2.0:User:nickName": "goldfish"
     *   }
     * </pre>
     *
     * <pre>
     *   {
     *     "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value": "123456"
     *     "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.display": "John"
     *   }
     * </pre>
     *
     * <pre>
     *   {
     *     "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter": "costCenter"
     *   }
     * </pre>
     *
     * @param schemaAttribute the attribute determined by the key of the current value
     * @param resourceNode the resourceNode that directly envelopes node of the schemaAttribute e.g.: <br />
     *
     *          <pre>
     *            {
     *              "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
     *                 "costCenter": "costCenter"
     *              }
     *            }
     *            schemaAttribute = "costCenter"
     *            resourceNode = the inner extension node
     *          </pre>
     *
     * @param attributeEntry the attribute entry with its name and value as it is present in the resource
     * @param patchOp the patchOperation that should be applied to the attribute (only ADD or REPLACE)
     */
    private boolean handleResourceAttribute(SchemaAttribute schemaAttribute,
                                            ObjectNode resourceNode,
                                            Map.Entry<String, JsonNode> attributeEntry,
                                            PatchOp patchOp)
    {
      boolean isShallowNode = schemaAttribute.getParent() == null && !Type.COMPLEX.equals(schemaAttribute.getType());
      if (isShallowNode)
      // a shallow-node has never children, so it will be a node like userName, nickName or costCenter
      {
        return handleShallowNode(schemaAttribute, attributeEntry, patchOp);
      }
      else
      // complex node
      {
        boolean isSubNodeReference = schemaAttribute.getParent() != null;
        if (isSubNodeReference)
        // direct subNode references are used by MsAzure and are not defined by SCIM for resource-patch-operations
        {
          if (schemaAttribute.getParent().isMultiValued())
          {
            // this is only a partial workaround. If Someone uses something like this:
            // {
            // "emails.value", "max@mustermann.de",
            // "emails.primary", true,
            // }
            // it will not work, because these cannot be put into relation with each other. This would create two
            // email-objects that would cause the eventual schema-validation to fail
            ScimObjectNode objectNode = new ScimObjectNode();
            objectNode.set(schemaAttribute.getName(), attributeEntry.getValue());
            List<String> valueStringList = Collections.singletonList(objectNode.toString());
            final boolean wasChanged;
            ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
            arrayNode.add(objectNode);
            AttributePathRoot attributePath = new AttributePathRoot(schemaAttribute.getParent());
            MultivaluedComplexAttributeOperation multivaluedComplexAttributeOperation = //
              new MultivaluedComplexAttributeOperation(attributePath, patchOp, arrayNode, valueStringList);

            patchValidations.validatePath(attributePath, patchOp, valueStringList);
            wasChanged = patchOperationHandler.handleOperation(resourceId, multivaluedComplexAttributeOperation);
            addAttributeToRequestedAttributes(schemaAttribute);
            return wasChanged;
          }
          return handleShallowNode(schemaAttribute, attributeEntry, patchOp);
        }
        else
        {
          JsonNode complexNode = resourceNode.get(schemaAttribute.getName());
          if (complexNode == null)
          // MsAzure style reference ${schemaId}:${complexAttributeName}
          {
            boolean isMsAzureStyleReference = attributeEntry.getKey()
                                                            .startsWith(schemaAttribute.getSchema().getNonNullId());
            if (isMsAzureStyleReference)
            {
              complexNode = resourceNode.get(attributeEntry.getKey());
              return handleComplexNodeReference(schemaAttribute, patchOp, complexNode);
            }
            else
            {
              return handleShallowNode(schemaAttribute, attributeEntry, patchOp);
            }
          }
          else
          {
            return handleComplexNodeReference(schemaAttribute, patchOp, complexNode);
          }
        }
      }
    }

    /**
     * handles complex-attributes (also multivalued) and translates them into patch-path-operations.
     * Non-multivalued attributes will get a single operation for each sub-attribute but
     * multivalued-complex-attributes will get just a single patch operation that targets the root of the
     * multivalued-complex-attribute e.g. <em>emails</em>
     *
     * @param schemaAttribute the attribute that should be patched
     * @param patchOp the operation to apply to the resource
     * @param complexNode the complex representation of the node from the patch-operation
     * @return true if an effective change of the resource did occur
     */
    private boolean handleComplexNodeReference(SchemaAttribute schemaAttribute, PatchOp patchOp, JsonNode complexNode)
    {
      if (schemaAttribute.isMultiValued())
      {
        List<String> valuesStringList = new ArrayList<>();
        ArrayNode patchNode;
        if (complexNode instanceof ArrayNode)
        {
          patchNode = (ArrayNode)complexNode;
          for ( JsonNode jsonNode : patchNode )
          {
            JsonNode sanitizedNode = removeUnknownAttributesFromValues(schemaAttribute, jsonNode);
            valuesStringList.add(sanitizedNode.toString());
          }
        }
        else
        {
          patchNode = new ArrayNode(JsonNodeFactory.instance);
          patchNode.add(complexNode);
          valuesStringList.add(complexNode.toString());
        }
        AttributePathRoot attributePath = new AttributePathRoot(schemaAttribute);
        MultivaluedComplexAttributeOperation operation = new MultivaluedComplexAttributeOperation(attributePath,
                                                                                                  patchOp, patchNode,
                                                                                                  valuesStringList);
        patchValidations.validatePath(attributePath, patchOp, valuesStringList);
        boolean wasChanged = patchOperationHandler.handleOperation(resourceId, operation);
        addAttributeToRequestedAttributes(schemaAttribute);
        return wasChanged;
      }
      else
      {
        AtomicReference<Boolean> wasChanged = new AtomicReference<>(false);
        if (!complexNode.isObject())
        {
          throw new BadRequestException(String.format("Value for attribute '%s' must be an object but was '%s'",
                                                      schemaAttribute.getFullResourceName(),
                                                      complexNode),
                                        ScimType.RFC7644.INVALID_VALUE);
        }
        complexNode.fields().forEachRemaining(complexNodeField -> {
          final String fullNodeName = String.format("%s.%s", schemaAttribute.getName(), complexNodeField.getKey());
          SchemaAttribute childNodeDefinition = schemaAttribute.getSchema().getSchemaAttribute(fullNodeName);
          if (childNodeDefinition == null)
          {
            log.trace("Found unknown attribute with name '{}' in request. Attribute will be ignored.",
                      complexNodeField.getKey());
            return;
          }
          try
          {
            wasChanged.compareAndSet(false, handleShallowNode(childNodeDefinition, complexNodeField, patchOp));
          }
          catch (IgnoreOperationException ex)
          {
            log.trace(ex.getMessage(), ex);
            // simply ignore the attributes operation and do nothing
          }
        });
        return wasChanged.get();
      }
    }

    /**
     * this method will make sure that no unknown attributes are passed to the patch-handler by creating a new
     * object that holds only the known attributes and ignores undefined attributes
     *
     * @param schemaAttribute the complex attributes definition
     * @param complexNode the representation from the patch-request
     * @return the representation that will only have known attributes
     */
    private ObjectNode removeUnknownAttributesFromValues(SchemaAttribute schemaAttribute, JsonNode complexNode)
    {
      ObjectNode sanitizedNode = new ScimObjectNode(schemaAttribute);
      for ( SchemaAttribute subAttribute : schemaAttribute.getSubAttributes() )
      {
        if (complexNode.has(subAttribute.getName()))
        {
          sanitizedNode.set(subAttribute.getName(), complexNode.get(subAttribute.getName()));
        }
      }
      return sanitizedNode;
    }

    /**
     * handles a shallow-node that has never children, so it will be a node like userName, nickName or costCenter
     *
     * @param schemaAttribute the attributes definition
     * @param attributeEntry the attribute from the resource
     * @param patchOp the patch operation to apply
     */
    private boolean handleShallowNode(SchemaAttribute schemaAttribute,
                                      Map.Entry<String, JsonNode> attributeEntry,
                                      PatchOp patchOp)
    {
      List<Object> values = new ArrayList<>();
      JsonNode value = attributeEntry.getValue();
      if (schemaAttribute.isMultiValued())
      // multivalued shallow nodes are allowed to be simple values and array values.
      {
        if (value instanceof ArrayNode)
        {
          ArrayNode arrayNode = (ArrayNode)value;
          arrayNode.forEach(val -> {
            Object javaValue = getValueOfJsonNode(schemaAttribute, val);
            if (javaValue == null)
            {
              throw new BadRequestException(String.format("Illegal type for attribute '%s'. Type must be '%s' "
                                                          + "but was of type '%s'",
                                                          schemaAttribute.getFullResourceName(),
                                                          schemaAttribute.getType(),
                                                          val.getNodeType()),
                                            ScimType.RFC7644.INVALID_VALUE);
            }
            values.add(javaValue);
          });
        }
        else
        {
          Object javaValue = getValueOfJsonNode(schemaAttribute, value);
          if (javaValue == null)
          {
            throw new BadRequestException(String.format("Illegal type for attribute '%s'. Type must be '%s' "
                                                        + "but was of type '%s'",
                                                        schemaAttribute.getFullResourceName(),
                                                        schemaAttribute.getType(),
                                                        value.getNodeType()),
                                          ScimType.RFC7644.INVALID_VALUE);
          }
          values.add(javaValue);
        }
      }
      else
      {
        Object javaValue = getValueOfJsonNode(schemaAttribute, attributeEntry.getValue());
        if (javaValue == null)
        {
          throw new BadRequestException(String.format("Illegal type for attribute '%s'. Type must be '%s' "
                                                      + "but was of type '%s'",
                                                      schemaAttribute.getFullResourceName(),
                                                      schemaAttribute.getType(),
                                                      attributeEntry.getValue().getNodeType()),
                                        ScimType.RFC7644.INVALID_VALUE);
        }
        values.add(javaValue);
      }
      AttributePathRoot attributePath = new AttributePathRoot(schemaAttribute);
      List<String> valuesStringList = new ArrayList<>();
      if (value.isObject())
      {
        valuesStringList.add(value.toString());
      }
      else if (value.isArray())
      {
        ArrayNode arrayNode = (ArrayNode)value;
        for ( JsonNode jsonNode : arrayNode )
        {
          valuesStringList.add(jsonNode.asText());
        }
      }
      else
      {
        valuesStringList.add(value.asText());
      }
      patchValidations.validatePath(attributePath, patchOp, valuesStringList);
      boolean wasChanged = patchOperationHandler.handleOperation(resourceId,
                                                                 new SimpleAttributeOperation(attributePath, patchOp,
                                                                                              value, valuesStringList));
      addAttributeToRequestedAttributes(schemaAttribute);
      return wasChanged;
    }

  }

  /**
   * this class contains utility-methods to validate the patch operations content
   */
  private class PatchValidations
  {

    /**
     * validates the content of a patch operation that directly references a complex attribute like <em>name</em>
     * or <em>manager</em>
     *
     * @param schemaAttribute the attributes definition
     * @param patchOp the patch operation that is being applied
     * @param values the values from the patch request
     * @return the objectNode that is applied to the complex attribute from the values of the patch-operation
     */
    private ObjectNode validateValueOfDirectComplexAttributePath(SchemaAttribute schemaAttribute,
                                                                 PatchOp patchOp,
                                                                 ArrayNode values)
    {
      if (PatchOp.REMOVE.equals(patchOp))
      {
        // remove was validated already before with #validateRemoveOperation(...)
        return null;
      }

      if (values == null || values.isEmpty())
      {
        throw new BadRequestException(String.format("No value set for complex attribute path '%s'",
                                                    schemaAttribute.getFullResourceName()),
                                      null, ScimType.RFC7644.INVALID_VALUE);
      }
      if (values.size() > 1)
      {
        throw new BadRequestException(String.format("Found too many values for non multivalued complex attribute path "
                                                    + "'%s': %s",
                                                    schemaAttribute.getFullResourceName(),
                                                    values),
                                      null, ScimType.RFC7644.INVALID_VALUE);
      }
      return verifyIsObject(schemaAttribute, values.get(0));
    }

    /**
     * verifies if the given jsonNode is an object and tries to parse it into such
     *
     * @param schemaAttribute the attributes definition is only needed in case of an error
     * @param value the objectNode that might be a string-representation of the object
     * @return the parsed object-node
     */
    private ObjectNode verifyIsObject(SchemaAttribute schemaAttribute, JsonNode value)
    {
      if (value.isObject())
      {
        return (ObjectNode)value;
      }
      try
      {
        return (ObjectNode)JsonHelper.readJsonDocument(value.asText());
      }
      catch (Exception ex)
      {
        throw new BadRequestException(String.format("Value for path '%s' must be a complex-node representation but "
                                                    + "was: %s",
                                                    schemaAttribute.getFullResourceName(),
                                                    value),
                                      null, ScimType.RFC7644.INVALID_VALUE);
      }
    }

    /**
     * this method will check the expression send by the client does follow its syntax rules based on the used
     * operation
     *
     * @param attributePath the target expression
     * @param patchOp the operation
     * @param values the values (should be empty on delete)
     */
    private void validatePath(AttributePathRoot attributePath, PatchOp patchOp, List<String> values)
    {
      SchemaAttribute schemaAttribute = attributePath.getDirectlyReferencedAttribute();
      if (Mutability.READ_ONLY.equals(schemaAttribute.getMutability()))
      {
        throw new IgnoreOperationException();
      }
      if (schemaAttribute.getParent() != null
          && Mutability.READ_ONLY.equals(schemaAttribute.getParent().getMutability()))
      {
        throw new IgnoreOperationException();
      }

      switch (patchOp)
      {
        case ADD:
          checkForComplexJson(attributePath, values);
          validateValueCount(attributePath.getDirectlyReferencedAttribute(), values);
          break;
        case REPLACE:
          validateReplaceOperation(attributePath, values);
          validateValueCount(attributePath.getDirectlyReferencedAttribute(), values);
          break;
        case REMOVE:
          validateRemoveOperation(values);
          break;
      }
    }

    /**
     * this method validates that the patch operation does not have too many or not enough values when patching a
     * resource
     *
     * @param attributePath the attribute path that contains the definition of the attribute
     * @param patchOp the operation that determines if values may be present
     * @param values the values from the patch operation
     */
    private void validateValueCount(SchemaAttribute schemaAttribute, List<String> values)
    {
      if (values.isEmpty())
      {
        throw new BadRequestException(String.format("Missing value for patch-operation on attribute '%s'."
                                                    + " ADD and REPLACE operations require at least one value.",
                                                    schemaAttribute.getScimNodeName()),
                                      ScimType.RFC7644.INVALID_VALUE);
      }

      if (!schemaAttribute.isMultiValued() && values.size() > 1)
      {
        throw new BadRequestException(String.format("Too many values found for '%s'-type attribute '%s': %s",
                                                    schemaAttribute.getType(),
                                                    schemaAttribute.getScimNodeName(),
                                                    values),
                                      ScimType.RFC7644.INVALID_VALUE);
      }
    }

    /**
     * will validate that no values are present in the values list all other path representations should be valid
     * except for an empty representation
     *
     * @param path the attribute path expression
     * @param values in remove operation no values should be present
     */
    private void validateRemoveOperation(List<String> values)
    {
      if (values != null && !values.isEmpty())
      {
        throw new BadRequestException(String.format("Values must not be set for remove operation but was: %s",
                                                    String.join(",", values)),
                                      null, ScimType.RFC7644.INVALID_VALUE);
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
      if (!path.isWithSubAttributeRef() && path.isWithFilter() && !values.stream().allMatch(JsonHelper::isValidJson))
      {
        throw new BadRequestException("the values are expected to be valid json representations for an expression as "
                                      + "'" + path + "' but was: " + String.join(",\n", values), null,
                                      ScimType.RFC7644.INVALID_PATH);
      }
      checkForComplexJson(path, values);
    }

    /**
     * verifies that the values are valid json representations if we have an injection into a complex type without
     * a sub-attribute
     *
     * @param path the target of the expression
     * @param values the values should be added or replaced
     */
    private void checkForComplexJson(AttributePathRoot path, List<String> values)
    {
      // emails or name
      if (!path.isWithFilter() && path.getSchemaAttribute().isComplexAttribute() && !path.isWithSubAttributeRef()
          && !values.stream().allMatch(JsonHelper::isValidJson))
      {
        throw new BadRequestException("the value parameters must be valid json representations but was '"
                                      + String.join(",", values) + "'", null, ScimType.RFC7644.INVALID_VALUE);

      }
    }

    /**
     * validates a simple attribute just like in the normal request-resource-validation
     *
     * @param attributePath the attribute path
     * @param patchRequestOperation the request operation that is being applied
     * @return the validated node-representation
     */
    private JsonNode validateSimpleAttribute(AttributePathRoot attributePath,
                                             PatchRequestOperation patchRequestOperation)
    {
      if (PatchOp.REMOVE.equals(patchRequestOperation.getOp()))
      {
        return null;
      }
      SchemaAttribute schemaAttribute = attributePath.getSchemaAttribute();
      JsonNode value = patchRequestOperation.getValueNode().map(arrayNode -> arrayNode.get(0)).orElse(null);
      return SimpleAttributeValidator.parseNodeTypeAndValidate(schemaAttribute, value);
    }

    /**
     * validates a simple multivalued attribute just like in the normal request-resource-validation
     *
     * @param attributePath the attribute path
     * @param patchRequestOperation the request operation that is being applied
     * @return the validated node-representation
     */
    private ArrayNode validateSimpleMultivaluedAttribute(AttributePathRoot attributePath,
                                                         PatchRequestOperation patchRequestOperation)
    {
      if (PatchOp.REMOVE.equals(patchRequestOperation.getOp()))
      {
        return null;
      }
      SchemaAttribute schemaAttribute = attributePath.getSchemaAttribute();
      return SimpleMultivaluedAttributeValidator.parseNodeTypeAndValidate(schemaAttribute,
                                                                          patchRequestOperation.getValueNode().get());
    }
  }

}





