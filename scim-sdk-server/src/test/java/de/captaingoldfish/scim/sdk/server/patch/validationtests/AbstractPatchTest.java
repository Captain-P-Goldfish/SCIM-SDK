package de.captaingoldfish.scim.sdk.server.patch.validationtests;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.AllTypesHandlerImpl;
import de.captaingoldfish.scim.sdk.server.patch.DefaultPatchOperationHandler;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;


/**
 * @author Pascal Knueppel
 * @since 19.01.2024
 */
public abstract class AbstractPatchTest implements FileReferences
{

  /**
   * the resource type for all types definition. Contains data types of any possible scim representation
   */
  protected ResourceType allTypesResourceType;

  /**
   * the service provider with the current endpoints configuration
   */
  protected ServiceProvider serviceProvider;

  /**
   * used to access the default
   * {@link de.captaingoldfish.scim.sdk.server.patch.workarounds.PatchWorkaround}-handlers
   */
  protected ResourceEndpoint resourceEndpoint;

  /**
   * the resource-handler used for the patch-tests
   */
  protected AllTypesHandlerImpl allTypesHandler;

  protected DefaultPatchOperationHandler defaultPatchOperationHandler;

  /**
   * initializes a new {@link ResourceTypeFactory} for the following tests
   */
  @BeforeEach
  public void initialize()
  {
    this.serviceProvider = ServiceProvider.builder().patchConfig(PatchConfig.builder().supported(true).build()).build();
    JsonNode allTypesResourceType = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    resourceEndpoint = new ResourceEndpoint(serviceProvider);
    this.allTypesHandler = Mockito.spy(new AllTypesHandlerImpl());
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceType,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));

    this.defaultPatchOperationHandler = //
      (DefaultPatchOperationHandler)allTypesHandler.getPatchOpResourceHandler(new Context(null));
    this.defaultPatchOperationHandler = Mockito.spy(this.defaultPatchOperationHandler);
    Mockito.doReturn(this.defaultPatchOperationHandler).when(allTypesHandler).getPatchOpResourceHandler(Mockito.any());
  }

  /**
   * adds some custom attributes to the enterprise user schema for testing.
   */
  public void addCustomAttributesToEnterpriseUserSchema()
  {
    Schema mainSchema = allTypesResourceType.getMainSchema();
    Schema enterpriseUserSchema = allTypesResourceType.getSchemaByUri(SchemaUris.ENTERPRISE_USER_URI);

    ObjectNode numberAttribute = mainSchema.getSchemaAttribute("number").deepCopy();
    ObjectNode numberArrayAttribute = mainSchema.getSchemaAttribute("numberArray").deepCopy();
    ObjectNode complexAttribute = mainSchema.getSchemaAttribute("complex").deepCopy();
    ObjectNode multiComplexAttribute = mainSchema.getSchemaAttribute("multiComplex").deepCopy();

    enterpriseUserSchema.addAttribute(numberAttribute);
    enterpriseUserSchema.addAttribute(numberArrayAttribute);
    enterpriseUserSchema.addAttribute(complexAttribute);
    enterpriseUserSchema.addAttribute(multiComplexAttribute);
  }

  /**
   * adds an allTypes object to the {@link #allTypesHandler}
   */
  public void addAllTypesToProvider(AllTypes allTypes)
  {
    if (!allTypes.getId().isPresent())
    {
      allTypes.setId(UUID.randomUUID().toString());
    }
    allTypesHandler.getInMemoryMap().put(allTypes.getId().get(), allTypes);
  }
}
