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
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.patch.PatchHandler;
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
   * needed to extract the {@link ResourceType}s which are necessary to check if the given attribute-names are
   * valid or not
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the resource type for all types definition. Contains data types of any possible scim representation
   */
  private ResourceType allTypesResourceType;

  /**
   * initializes a new {@link ResourceTypeFactory} for the following tests
   */
  @BeforeEach
  public void initialize()
  {
    this.serviceProvider = ServiceProvider.builder().patchConfig(PatchConfig.builder().supported(true).build()).build();
    this.resourceTypeFactory = new ResourceTypeFactory();
    JsonNode allTypesResourceType = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    this.allTypesResourceType = resourceTypeFactory.registerResourceType(null,
                                                                         allTypesResourceType,
                                                                         allTypesSchema,
                                                                         enterpriseUserSchema);
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
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    AllTypes patchedResource = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(patchedResource.getEnterpriseUser().isPresent());
    Assertions.assertEquals(employeeNumber, patchedResource.getEnterpriseUser().get().getEmployeeNumber().get());
    Assertions.assertEquals(costCenter, patchedResource.getEnterpriseUser().get().getCostCenter().get());
  }

  /**
   * verifies that a complete complex type can be replaced if the value is set as object
   */
  @Test
  public void testAddExtensionComplexValueInMsAzureAdStyle()
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
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    AllTypes patchedResource = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(patchedResource.getEnterpriseUser().isPresent());
    Assertions.assertEquals(managerId, patchedResource.getEnterpriseUser().get().getManager().get().getValue().get());
    Assertions.assertEquals("User", patchedResource.getEnterpriseUser().get().getManager().get().getRef().get());
    Assertions.assertEquals("Chuck Norris",
                            patchedResource.getEnterpriseUser().get().getManager().get().getDisplayName().get());
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
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    AllTypes patchedResource = patchHandler.patchResource(allTypes, patchOpRequest);
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
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
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
   * verifies that a simiple type does not accept an array as value and throws a {@link BadRequestException}
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
    String value = "[\"1111\"]";
    String valueNode = String.format("{\"%s\": %s}", key, value);
    // @formatter:on

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      String expectedMessage = String.format("Invalid value '%s' found for attribute '%s'", value, key);
      Assertions.assertEquals(expectedMessage, ex.getMessage());
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
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      String expectedMessage = String.format("Value for attribute '%s' must be an object but was '%s'", key, value);
      Assertions.assertEquals(expectedMessage, ex.getMessage());
    }
  }

  /**
   * verifies that multivalued attributes are not supported for the ms azure notation
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
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (BadRequestException ex)
    {
      String expectedMessage = String.format("Unsupported patch operation with key-reference: %s", key);
      Assertions.assertEquals(expectedMessage, ex.getMessage());
    }
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

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setEnterpriseUser(EnterpriseUser.builder()
                                                   .employeeNumber(employeeNumber)
                                                   .costCenter(costCenter)
                                                   .build());

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
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    AllTypes newAllTypes = patchHandler.patchResource(allTypeChanges, patchOpRequest);
    Assertions.assertEquals(employeeNumberChanged, newAllTypes.getEnterpriseUser().get().getEmployeeNumber().get());
    Assertions.assertTrue(patchHandler.isChangedResource());
  }
}
