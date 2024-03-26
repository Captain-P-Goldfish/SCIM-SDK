package de.captaingoldfish.scim.sdk.server.patch.msazure;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpointBridge;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.AllTypesHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.RequestContextException;
import de.captaingoldfish.scim.sdk.server.patch.PatchRequestHandler;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 24.09.2021
 */
@Slf4j
public class MsAzurePatchExtensionResourceRebuilderTest implements FileReferences
{

  /**
   * contains the current patch configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * the base object that is used for registering resourceTypes
   */
  private ResourceEndpoint resourceEndpoint;

  /**
   * needed to extract the {@link ResourceType}s which are necessary to check if the given attribute-names are
   * valid or not
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the resource type for all types definition. Contains data types of any possible scim representation
   */
  private ResourceType allTypesResourceType;

  /**
   * the resourcehandler that is able to handle requests with the {@link AllTypes} object
   */
  private AllTypesHandlerImpl allTypesResourceHandler;

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
    this.resourceEndpoint = new ResourceEndpoint(serviceProvider);
    this.allTypesResourceHandler = new AllTypesHandlerImpl();
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceType,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         this.allTypesResourceHandler));
    this.resourceTypeFactory = ResourceEndpointBridge.getResourceTypeFactory(resourceEndpoint);
  }

  /**
   * adds an allTypes object to the {@link #allTypesHandler}
   */
  private void addAllTypesToProvider(AllTypes allTypes)
  {
    if (!allTypes.getId().isPresent())
    {
      allTypes.setId(UUID.randomUUID().toString());
    }
    allTypesResourceHandler.getInMemoryMap().put(allTypes.getId().get(), allTypes);
  }

  /**
   * this test will reproduce the problem caused by MS Azure AD in which an attribute is tried to be set in the
   * following style:
   *
   * <pre>
   *   {
   *     "Operations": [
   *         {
   *             "op": "replace",
   *             "value": {
   *                 ...
   *                 "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber": "1111"
   *             }
   *         }
   *     ],
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ]
   *   }
   * </pre>
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/193
   */
  @Test
  public void testAddExtensionValueInMsAzureAdStyle()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    // @formatter:off
    String employeeNumber = "1111";
    String costCenter = "2222";
    String valueNode = "{" +
      "  \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber\": \"" +
      employeeNumber+ "\"," +
      "  \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter\": \"" +
      costCenter+ "\"" +
      "}";
    // @formatter:on

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedResource = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchedResource.getEnterpriseUser().isPresent());
    Assertions.assertEquals(employeeNumber, patchedResource.getEnterpriseUser().get().getEmployeeNumber().get());
    Assertions.assertEquals(costCenter, patchedResource.getEnterpriseUser().get().getCostCenter().get());
  }

  /**
   * verifies that a complete complex type can be replaced if the value is set as object
   */
  @Test
  public void testAddExtensionComplexValueInMsAzureAddStyle()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    // @formatter:off
    String managerId = UUID.randomUUID().toString();
    String valueNode = "{" +
      "\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager\": {" +
                                                                                  "\"value\": \"" + managerId + "\",\n" +
                                                                                  "\"$ref\": \"User\"\n" +
                                                                                 "}" +
      "}";
    // @formatter:on

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedResource = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchedResource.getEnterpriseUser().isPresent());
    Assertions.assertEquals(managerId, patchedResource.getEnterpriseUser().get().getManager().get().getValue().get());
    Assertions.assertEquals("User", patchedResource.getEnterpriseUser().get().getManager().get().getRef().get());
  }

  /**
   * verifies that the readOnly operation 'manager.displayName' on the resource will be ignored
   */
  @Test
  public void testAddExtensionComplexValueInMsAzureAddStyleWithReadOnlyAttribute()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    // @formatter:off
    String managerId = UUID.randomUUID().toString();
    String valueNode = "{" +
      "\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager\": {" +
                                                                                  "\"value\": \"" + managerId + "\",\n" +
                                                                                  "\"$ref\": \"User\",\n" +
                                                                                  "\"displayName\": \"Chuck Norris\"" +
                                                                                 "}" +
      "}";
    // @formatter:on

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    Assertions.assertEquals(managerId,
                            patchedAllTypes.getEnterpriseUser()
                                           .flatMap(EnterpriseUser::getManager)
                                           .flatMap(Manager::getValue)
                                           .orElse(null));
    Assertions.assertEquals("User",
                            patchedAllTypes.getEnterpriseUser()
                                           .flatMap(EnterpriseUser::getManager)
                                           .flatMap(Manager::getRef)
                                           .orElse(null));
    Assertions.assertNull(patchedAllTypes.getEnterpriseUser()
                                         .flatMap(EnterpriseUser::getManager)
                                         .flatMap(Manager::getDisplayName)
                                         .orElse(null));
  }

  /**
   * verifies that a reference to a complex sub-attribute can be successfully replaced
   */
  @Test
  public void testAddExtensionComplexValueInMsAzureAdStyle2()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    String key = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value";
    String managerId = UUID.randomUUID().toString();
    String valueNode = String.format("{\"%s\": \"%s\"}", key, managerId);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedResource = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchedResource.getEnterpriseUser().isPresent());
    Assertions.assertEquals(managerId, patchedResource.getEnterpriseUser().get().getManager().get().getValue().get());
  }

  /**
   * verifies that unknown attributes to the resource type will be rejected
   */
  @Test
  public void testReferenceUnknownAttribute()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    String key = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:unknown";
    String value = "1111";
    String valueNode = String.format("{\"%s\": \"%s\"}", key, value);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    try
    {
      addAllTypesToProvider(allTypes);
      PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                    allTypesResourceHandler,
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
      patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      String expectedMessage = String.format("Attribute '%s' is unknown to resource type '%s'",
                                             key,
                                             allTypesResourceType.getName());
      Assertions.assertEquals(expectedMessage, ex.getMessage());
    }
  }

  /**
   * verifies that a simple type does not accept an array as value and throws a {@link BadRequestException}
   */
  @Test
  public void testSimpleTypeDoesNotAcceptArrays()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    // @formatter:off
    String key = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber";
    String value = "[\"1111\", \"2222\"]";
    String valueNode = String.format("{\"%s\": %s}", key, value);
    // @formatter:on

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    try
    {
      patchRequestHandler.handlePatchRequest(patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (RequestContextException ex)
    {
      String expectedMessage = String.format("Attribute '%s' is expected to be a simple attribute of type 'STRING' "
                                             + "but is '[\"1111\",\"2222\"]'",
                                             key);
      ErrorResponse errorResponse = new ErrorResponse(ex);
      ex.getValidationContext().writeToErrorResponse(errorResponse);
      Assertions.assertEquals(expectedMessage, errorResponse.getDetail().get());
    }
  }

  /**
   * verifies that values for complex type references that are not objects will be rejected
   */
  @ParameterizedTest
  @ValueSource(strings = {"\"1111\"", "[\"1111\"]"})
  public void testComplexAttributeRefMustBeAnObject(String value)
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    String key = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager";
    String valueNode = String.format("{\"%s\": %s}", key, value);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    try
    {
      log.warn(patchRequestHandler.handlePatchRequest(patchOpRequest).toPrettyString());
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      String expectedMessage = String.format("Value for attribute '%s' must be an object but was '%s'",
                                             key,
                                             value.replaceAll("\\[\"", "").replaceAll("\"]", ""));
      Assertions.assertEquals(expectedMessage, ex.getMessage());
    }
  }

  /**
   * verifies that multivalued attributes are also supported for the msAzure notation
   */
  @Test
  public void testMsAzureNotationIsNotSupportedForMultivaluedAttributes()
  {
    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    // make costCenter attribute of enterpriseUser multivalued
    {
      JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
      TestHelper.modifyAttributeMetaData(enterpriseUserSchema,
                                         "costCenter",
                                         null,
                                         null,
                                         null,
                                         null,
                                         true,
                                         null,
                                         null,
                                         null);
      resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);
    }

    String key = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter";
    String value = "[\"1111\"]";
    String valueNode = String.format("{\"%s\": %s}", key, value);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedResource = patchRequestHandler.handlePatchRequest(patchOpRequest);
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
    Assertions.assertTrue(patchedResource.getEnterpriseUser().get().get("costCenter").isArray());
    Assertions.assertEquals(1, patchedResource.getEnterpriseUser().get().get("costCenter").size());
    Assertions.assertEquals("1111", patchedResource.getEnterpriseUser().get().get("costCenter").get(0).textValue());
  }

  /**
   * this will test that PatchHandler.isChangedResource is true if any attribute is changed, and not just if the
   * last attributes is changed.
   */
  @Test
  public void testIsChangedResourceForExtensionValueInMsAzureAdStyle()
  {
    String employeeNumber = "1111";
    String employeeNumberChanged = "2222";
    String costCenter = "2222";

    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().employeeNumber(employeeNumber).costCenter(costCenter).build());

    // @formatter:off
    String valueNode = "{" +
      "  \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber\": \"" +
      employeeNumberChanged+ "\"," +
      "  \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter\": \"" +
      costCenter+ "\"" +
      "}";
    // @formatter:on

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    addAllTypesToProvider(allTypes);
    PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler<>(allTypes.getId().get(),
                                                                                  allTypesResourceHandler,
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
    AllTypes patchedResource = patchRequestHandler.handlePatchRequest(patchOpRequest);

    Assertions.assertEquals(employeeNumberChanged, patchedResource.getEnterpriseUser().get().getEmployeeNumber().get());
    Assertions.assertTrue(patchRequestHandler.isResourceChanged());
  }
}
