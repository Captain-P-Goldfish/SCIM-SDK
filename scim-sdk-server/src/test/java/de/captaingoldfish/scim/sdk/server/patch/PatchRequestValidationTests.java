package de.captaingoldfish.scim.sdk.server.patch;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.AllTypesHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.RequestContextException;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 14.01.2024
 */
@Slf4j
public class PatchRequestValidationTests implements FileReferences
{

  /**
   * the resource type for all types definition. Contains data types of any possible scim representation
   */
  private ResourceType allTypesResourceType;

  /**
   * the service provider with the current endpoints configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * used to access the default
   * {@link de.captaingoldfish.scim.sdk.server.patch.workarounds.PatchWorkaround}-handlers
   */
  private ResourceEndpoint resourceEndpoint;

  /**
   * the resource-handler used for the patch-tests
   */
  private AllTypesHandlerImpl allTypesHandler;

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
    this.allTypesHandler = new AllTypesHandlerImpl();
    this.allTypesResourceType = resourceEndpoint.registerEndpoint(new EndpointDefinition(allTypesResourceType,
                                                                                         allTypesSchema,
                                                                                         Arrays.asList(enterpriseUserSchema),
                                                                                         allTypesHandler));
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
    allTypesHandler.getInMemoryMap().put(allTypes.getId().get(), allTypes);
  }

  @DisplayName("Simple attribute validation tests")
  @Nested
  public class SimpleAttributesTests
  {

    @DisplayName("Resource-Patch Tests")
    @Nested
    class ResourcePatchTests
    {

      /**
       * adds a string into the number-attribute
       */
      @DisplayName("failure: Number with string value")
      @ParameterizedTest
      @ValueSource(strings = {"number", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:number"})
      public void testWrongTypeOnNumber(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);

        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        patchResource.set(attributeName, new TextNode("hello"));

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type "
                                              + "'string' with value '\"hello\"'",
                                              numberAttribute.getFullResourceName()),
                                errorResponse.getDetail().get());
      }

      /**
       * adds a number into the costCenter attribute as correct attribute reference
       */
      @DisplayName("failure: CostCenter with number value")
      @Test
      public void testWrongTypeOnCostCenter()
      {
        final String attributeName = "costCenter";
        AllTypes patchResource = new AllTypes(true);
        SchemaAttribute costCenterAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        EnterpriseUser enterpriseUser = new EnterpriseUser();
        enterpriseUser.set(attributeName, new IntNode(5));
        patchResource.setEnterpriseUser(enterpriseUser);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'string' but of type "
                                              + "'number' with value '5'",
                                              costCenterAttribute.getFullResourceName()),
                                errorResponse.getDetail().get());
      }

      /**
       * adds a number into the costCenter attribute in msAzure style attribute-reference
       */
      @DisplayName("failure: CostCenter with number value and full attributeName-ref")
      @Test
      public void testWrongTypeOnCostCenterWithFullName()
      {
        final String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter";
        AllTypes patchResource = new AllTypes(true);
        SchemaAttribute costCenterAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        patchResource.set(attributeName, new IntNode(5));

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'string' but of type "
                                              + "'number' with value '5'",
                                              costCenterAttribute.getFullResourceName()),
                                errorResponse.getDetail().get());
      }

    }


    @DisplayName("Path-Patch Tests")
    @Nested
    class PathPatchTests
    {

      /**
       * injects a string into a number-attribute
       */
      @DisplayName("failure: Number with string value")
      @ParameterizedTest
      @ValueSource(strings = {"number", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:number"})
      public void testWrongTypeOnNumber(String attributeName)
      {
        JsonNode valueNode = new TextNode("hello");

        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(valueNode)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type "
                                              + "'string' with value '\"hello\"'",
                                              numberAttribute.getFullResourceName()),
                                errorResponse.getDetail().get());
      }

      /**
       * injects a number into the costCenter attribute
       */
      @DisplayName("failure: CostCenter with number value")
      @ParameterizedTest
      @ValueSource(strings = {"costCenter", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter"})
      public void testWrongTypeOnCostCenter(String attributeName)
      {
        JsonNode valueNode = new IntNode(5);

        SchemaAttribute costCenterAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(valueNode)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'string' but of type "
                                              + "'number' with value '5'",
                                              costCenterAttribute.getFullResourceName()),
                                errorResponse.getDetail().get());
      }
    }

  }

  @DisplayName("Multivalued simple attribute validation tests")
  @Nested
  public class MultivaluedSimpleAttributesTests
  {

    @DisplayName("Resource-Patch Tests")
    @Nested
    class ResourcePatchTests
    {

      /**
       * Verifies that no exception is thrown if the numberArray is assigned a simple value that is not an array
       */
      @DisplayName("success: NumberArray with single non-array value")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testArrayAcceptsNonArrayAttribute(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);
        patchResource.set(attributeName, new IntNode(5));

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(1, patchedResource.getNumberArray().size());
        Assertions.assertEquals(5L, patchedResource.getNumberArray().get(0));
      }

      /**
       * adds a string into the numberArray attribute
       */
      @DisplayName("failure: NumberArray with string value")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testWrongTypeOnNumber(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);

        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        arrayNode.add(new TextNode("hello"));
        patchResource.set(attributeName, arrayNode);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);

        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\"]'",
                                errorResponse.getDetail().get());
        List<String> fieldErrors = errorResponse.getFieldErrors().get(numberAttribute.getScimNodeName());
        Assertions.assertEquals(2, fieldErrors.size());
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\"]'", fieldErrors.get(0));
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type "
                                              + "'string' with value '\"hello\"'",
                                              numberAttribute.getFullResourceName()),
                                fieldErrors.get(1));
      }
    }

    @DisplayName("Path-Patch Tests")
    @Nested
    class PathPatchTests
    {

      /**
       * Verifies that no exception is thrown if the numberArray is assigned a simple value that is not an array
       */
      @DisplayName("success: NumberArray with single non-array value")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testArrayAcceptsNonArrayAttribute(String attributeName)
      {
        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(new IntNode(5))
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(1, patchedResource.getNumberArray().size());
        Assertions.assertEquals(5L, patchedResource.getNumberArray().get(0));
      }

      /**
       * verifies that strings are not allowed in number-type arrays
       */
      @DisplayName("failure: NumberArray with string value")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testWrongTypeOnNumber(String attributeName)
      {
        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(5);
        numberArray.add("hello");
        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(numberArray)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[5,\"hello\"]'",
                                errorResponse.getDetail().get());
        List<String> fieldErrors = errorResponse.getFieldErrors().get(numberAttribute.getScimNodeName());
        Assertions.assertEquals(2, fieldErrors.size());
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[5,\"hello\"]'", fieldErrors.get(0));
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type "
                                              + "'string' with value '\"hello\"'",
                                              numberAttribute.getFullResourceName()),
                                fieldErrors.get(1));
      }
    }

  }

  @DisplayName("Complex attribute validation tests")
  @Nested
  public class ComplexAttributesTests
  {

    @DisplayName("Resource-Patch Tests")
    @Nested
    class ResourcePatchTests
    {

      /**
       * adds a string into the complex.numberArray attribute with the msAzure style attribute reference
       */
      @DisplayName("success: sub-attribute numberArray add values (msAzure style)")
      @ParameterizedTest
      @ValueSource(strings = {"complex.numberArray",
                              "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.numberArray"})
      public void testAddSubNumberArrayWithSimpleAttributeRef(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);

        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(new IntNode(5));
        numberArray.add(new IntNode(6));

        AllTypes complex = new AllTypes(false);
        complex.set(numberAttribute.getName(), numberArray);
        patchResource.setComplex(complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getComplex().get().getNumberArray().size());
        Assertions.assertEquals(5L, patchedResource.getComplex().get().getNumberArray().get(0));
        Assertions.assertEquals(6L, patchedResource.getComplex().get().getNumberArray().get(1));
      }

      /**
       * adds a string into the complex.numberArray attribute
       */
      @DisplayName("failure: sub-attribute numberArray with string value")
      @ParameterizedTest
      @ValueSource(strings = {"complex.numberArray",
                              "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.numberArray"})
      public void testWrongTypeOnNumberWithSimpleAttributeRef(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);

        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(new TextNode("hello"));

        AllTypes complex = new AllTypes(false);
        complex.set(numberAttribute.getName(), numberArray);
        patchResource.setComplex(complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\"]'",
                                errorResponse.getDetail().get());
        List<String> fieldErrors = errorResponse.getFieldErrors().get(numberAttribute.getScimNodeName());
        Assertions.assertEquals(2, fieldErrors.size());
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\"]'", fieldErrors.get(0));
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type "
                                              + "'string' with value '\"hello\"'",
                                              numberAttribute.getFullResourceName()),
                                fieldErrors.get(1));
      }

      /**
       * adds a string into the complex.numberArray attribute with the msAzure style attribute reference
       */
      @DisplayName("success: sub-attribute numberArray add values (msAzure style)")
      @ParameterizedTest
      @ValueSource(strings = {"complex.numberArray",
                              "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.numberArray"})
      public void testAddSubNumberArrayWithMsAzureStyleRef(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);

        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(new IntNode(5));
        numberArray.add(new IntNode(6));

        patchResource.set(attributeName, numberArray);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getComplex().get().getNumberArray().size());
        Assertions.assertEquals(5L, patchedResource.getComplex().get().getNumberArray().get(0));
        Assertions.assertEquals(6L, patchedResource.getComplex().get().getNumberArray().get(1));
      }

      /**
       * adds a string into the complex.numberArray attribute with the msAzure style attribute reference
       */
      @DisplayName("failure: sub-attribute numberArray with string value (msAzureStyle)")
      @ParameterizedTest
      @ValueSource(strings = {"complex.numberArray",
                              "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.numberArray"})
      public void testAddWrongSubNumberArrayWithMsAzureStyleRef(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);

        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(new TextNode("hello"));

        patchResource.set(attributeName, numberArray);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\"]'",
                                errorResponse.getDetail().get());
        List<String> fieldErrors = errorResponse.getFieldErrors().get(numberAttribute.getScimNodeName());
        Assertions.assertEquals(2, fieldErrors.size());
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\"]'", fieldErrors.get(0));
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type "
                                              + "'string' with value '\"hello\"'",
                                              numberAttribute.getFullResourceName()),
                                fieldErrors.get(1));
      }
    }

    @DisplayName("Path-Patch Tests")
    @Nested
    class PathPatchTests
    {

      /**
       * adds a number-array into its appropriate place
       */
      @DisplayName("success: sub-attribute numberArray add values")
      @ParameterizedTest
      @ValueSource(strings = {"complex.numberArray",
                              "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.numberArray"})
      public void testAddSubNumberArrayWithSimpleAttributeRef(String attributeName)
      {
        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        SchemaAttribute complexAttribute = numberAttribute.getParent();

        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(new IntNode(5));
        numberArray.add(new IntNode(6));

        AllTypes complex = new AllTypes(false);
        complex.set(numberAttribute.getName(), numberArray);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(complexAttribute.getFullResourceName())
                                                                                    .valueNode(complex)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getComplex().get().getNumberArray().size());
        Assertions.assertEquals(5L, patchedResource.getComplex().get().getNumberArray().get(0));
        Assertions.assertEquals(6L, patchedResource.getComplex().get().getNumberArray().get(1));
      }

      /**
       * adds a string into the complex.numberArray attribute
       */
      @DisplayName("failure: sub-attribute numberArray with string value")
      @ParameterizedTest
      @ValueSource(strings = {"complex.numberArray",
                              "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.numberArray"})
      public void testWrongTypeOnNumberWithSimpleAttributeRef(String attributeName)
      {
        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        SchemaAttribute complexAttribute = numberAttribute.getParent();

        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(new TextNode("hello"));

        AllTypes complex = new AllTypes(false);
        complex.set(numberAttribute.getName(), numberArray);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(complexAttribute.getFullResourceName())
                                                                                    .valueNode(complex)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));

        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\"]'",
                                errorResponse.getDetail().get());
        List<String> fieldErrors = errorResponse.getFieldErrors().get(numberAttribute.getScimNodeName());
        Assertions.assertEquals(2, fieldErrors.size());
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\"]'", fieldErrors.get(0));
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type "
                                              + "'string' with value '\"hello\"'",
                                              numberAttribute.getFullResourceName()),
                                fieldErrors.get(1));
      }

    }

  }

  @DisplayName("Complex simple-sub-attribute validation tests")
  @Nested
  public class ComplexSimpleSubAttributesTests
  {

    /**
     * no tests here because they would be exactly identical to: {@link ComplexAttributesTests.ResourcePatchTests}
     */
    @DisplayName("Resource-Patch Tests")
    @Nested
    class ResourcePatchTests
    {}

    @DisplayName("Path-Patch Tests")
    @Nested
    class PathPatchTests
    {

      /**
       * adds a number-array into its appropriate place
       */
      @DisplayName("success: sub-attribute number: add value")
      @ParameterizedTest
      @ValueSource(strings = {"complex.number", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.number"})
      public void testAddSubNumberArray(String attributeName)
      {
        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

        JsonNode numberNode = new IntNode(5);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(numberAttribute.getFullResourceName())
                                                                                    .valueNode(numberNode)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(5L, patchedResource.getComplex().get().getNumber().get());
      }

      /**
       * adds a number-field with a string value
       */
      @DisplayName("failure: sub-attribute number: add string values")
      @ParameterizedTest
      @ValueSource(strings = {"complex.number", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.number"})
      public void testAddSubNumberWithInvalidStringValue(String attributeName)
      {
        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

        JsonNode numberNode = new TextNode("hello");

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(numberAttribute.getFullResourceName())
                                                                                    .valueNode(numberNode)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type "
                                              + "'string' with value '\"hello\"'",
                                              numberAttribute.getFullResourceName()),
                                errorResponse.getDetail().get());
      }

      /**
       * adds a number-value to the field manager.value
       */
      @DisplayName("failure: sub-attribute manager.value: add number value")
      @ParameterizedTest
      @ValueSource(strings = {"manager.value",
                              "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value"})
      public void testAddManagerValueWithInvalidNumberValue(String attributeName)
      {
        SchemaAttribute managerValueAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

        JsonNode managerValue = new IntNode(564);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(managerValue)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));

        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'string' but of type "
                                              + "'number' with value '564'",
                                              managerValueAttribute.getFullResourceName()),
                                errorResponse.getDetail().get());
      }

    }

  }

  @DisplayName("Complex multivalued-sub-attribute validation tests")
  @Nested
  public class ComplexMultivaluedSubAttributesTests
  {

    /**
     * no tests here because they would be exactly identical to: {@link ComplexAttributesTests.ResourcePatchTests}
     */
    @DisplayName("Resource-Patch Tests")
    @Nested
    class ResourcePatchTests
    {}

    @DisplayName("Path-Patch Tests")
    @Nested
    class PathPatchTests
    {

      /**
       * adds a number-array into its appropriate place
       */
      @DisplayName("success: sub-attribute numberArray add values")
      @ParameterizedTest
      @ValueSource(strings = {"complex.numberArray",
                              "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.numberArray"})
      public void testAddSubNumberArray(String attributeName)
      {
        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(new IntNode(5));
        numberArray.add(new IntNode(6));

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(numberAttribute.getFullResourceName())
                                                                                    .valueNode(numberArray)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getComplex().get().getNumberArray().size());
        Assertions.assertEquals(5L, patchedResource.getComplex().get().getNumberArray().get(0));
        Assertions.assertEquals(6L, patchedResource.getComplex().get().getNumberArray().get(1));
      }

      /**
       * adds a number-array with string values
       */
      @DisplayName("failure: sub-attribute numberArray add string values")
      @ParameterizedTest
      @ValueSource(strings = {"complex.numberArray",
                              "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.numberArray"})
      public void testAddSubNumberArrayWithInvalidStringValue(String attributeName)
      {
        SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(new TextNode("hello"));
        numberArray.add(new TextNode("world"));

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(numberAttribute.getFullResourceName())
                                                                                    .valueNode(numberArray)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\",\"world\"]'",
                                errorResponse.getDetail().get());
        List<String> fieldErrors = errorResponse.getFieldErrors().get(numberAttribute.getScimNodeName());
        Assertions.assertEquals(2, fieldErrors.size());
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\",\"world\"]'",
                                fieldErrors.get(0));
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type "
                                              + "'string' with value '\"hello\"'",
                                              numberAttribute.getFullResourceName()),
                                fieldErrors.get(1));
      }
    }

  }

  @DisplayName("Multivalued Complex attribute validation tests")
  @Nested
  public class MultivaluedComplexAttributesTests
  {

    @DisplayName("Resource-Patch Tests")
    @Nested
    class ResourcePatchTests
    {

      /**
       * Replace a multivalued complex attribute in normal array reference
       */
      @DisplayName("success: replace multivalued complex type with array reference (no previous)")
      @ParameterizedTest
      @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
      public void testReplaceMultiComplex(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);

        AllTypes complex = new AllTypes(false);
        complex.setString("hello world");
        complex.setNumber(5L);
        complex.setBool(true);
        complex.setNumberArray(Arrays.asList(6L, 7L));
        complex.setBoolArray(Arrays.asList(true, false));

        ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
        multiComplex.add(complex);
        patchResource.set(attributeName, multiComplex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(1, patchedResource.getMultiComplex().size());

        AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
        Assertions.assertEquals("hello world", patchedComplex.getString().get());
        Assertions.assertEquals(5L, patchedComplex.getNumber().get());
        Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
        Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
        Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
        Assertions.assertEquals(true, patchedComplex.getBool().get());
        Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
        Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
        Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
      }

      /**
       * Replace a multiComplex with an object-reference instead of array
       */
      @DisplayName("success: replace multivalued complex type with object reference (no previous)")
      @ParameterizedTest
      @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
      public void testReplaceMultiComplexWithNonArrayReference(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);

        AllTypes complex = new AllTypes(false);
        complex.setString("hello world");
        complex.setNumber(5L);
        complex.setBool(true);
        complex.setNumberArray(Arrays.asList(6L, 7L));
        complex.setBoolArray(Arrays.asList(true, false));
        patchResource.set(attributeName, complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(1, patchedResource.getMultiComplex().size());

        AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
        Assertions.assertEquals("hello world", patchedComplex.getString().get());
        Assertions.assertEquals(5L, patchedComplex.getNumber().get());
        Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
        Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
        Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
        Assertions.assertEquals(true, patchedComplex.getBool().get());
        Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
        Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
        Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
      }

      /**
       * add a multiComplex with an object-reference instead of array
       */
      @DisplayName("success: add multivalued complex type with object reference (with previous element)")
      @ParameterizedTest
      @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
      public void testAddMultiComplexWithNonArrayReference(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);

        AllTypes complex = new AllTypes(false);
        complex.setString("hello world");
        complex.setNumber(5L);
        complex.setBool(true);
        complex.setNumberArray(Arrays.asList(6L, 7L));
        complex.setBoolArray(Arrays.asList(true, false));
        patchResource.set(attributeName, complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.ADD)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        {
          AllTypes previousComplex = new AllTypes(false);
          previousComplex.setString("world, hello");
          previousComplex.setNumber(55L);
          previousComplex.setBool(false);
          previousComplex.setNumberArray(Arrays.asList(66L, 77L));
          previousComplex.setBoolArray(Arrays.asList(false, true));
          previousComplex.setBinaryArray(Arrays.asList("Mtox".getBytes(StandardCharsets.UTF_8)));
          allTypes.setMultiComplex(Arrays.asList(previousComplex));
        }
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getMultiComplex().size());
        {
          AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
          Assertions.assertEquals("world, hello", patchedComplex.getString().get());
          Assertions.assertEquals(55L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(66L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(77L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(false, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(1));
          Assertions.assertEquals(1, patchedComplex.getBinaryArray().size());
          Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8), patchedComplex.getBinaryArray().get(0));
        }
        {
          AllTypes patchedComplex = patchedResource.getMultiComplex().get(1);
          Assertions.assertEquals("hello world", patchedComplex.getString().get());
          Assertions.assertEquals(5L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(true, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
        }
      }
    }

    @DisplayName("Path-Patch Tests")
    @Nested
    class PathPatchTests
    {

      /**
       * replace an empty field with a multivalued complex attribute by direct path reference
       */
      @DisplayName("success: replace multivalued complex (no previous)")
      @ParameterizedTest
      @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
      public void testAddMultivaluedComplexNoPrevious(String attributeName)
      {
        AllTypes complex = new AllTypes(false);
        complex.setString("hello world");
        complex.setNumber(5L);
        complex.setBool(true);
        complex.setNumberArray(Arrays.asList(6L, 7L));
        complex.setBoolArray(Arrays.asList(true, false));

        ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
        multiComplex.add(complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(multiComplex)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(1, patchedResource.getMultiComplex().size());

        AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
        Assertions.assertEquals("hello world", patchedComplex.getString().get());
        Assertions.assertEquals(5L, patchedComplex.getNumber().get());
        Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
        Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
        Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
        Assertions.assertEquals(true, patchedComplex.getBool().get());
        Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
        Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
        Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
      }

      /**
       * replace an empty field with two multivalued complex attributes by direct path reference
       */
      @DisplayName("success: replace two multivalued complex (no previous)")
      @ParameterizedTest
      @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
      public void testAddTwoMultivaluedComplexNoPrevious(String attributeName)
      {
        AllTypes complex = new AllTypes(false);
        complex.setString("hello world");
        complex.setNumber(5L);
        complex.setBool(true);
        complex.setNumberArray(Arrays.asList(6L, 7L));
        complex.setBoolArray(Arrays.asList(true, false));

        ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
        multiComplex.add(complex);
        multiComplex.add(complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(multiComplex)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getMultiComplex().size());
        for ( int i = 0 ; i < patchedResource.getMultiComplex().size() ; i++ )
        {
          AllTypes patchedComplex = patchedResource.getMultiComplex().get(i);
          Assertions.assertEquals("hello world", patchedComplex.getString().get());
          Assertions.assertEquals(5L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(true, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
        }
      }

      /**
       * replace one existing multiComplex with two multivalued complex attributes by direct path reference
       */
      @DisplayName("success: replace two multivalued complex (with previous elements)")
      @ParameterizedTest
      @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
      public void testReplaceTwoMultivaluedComplex(String attributeName)
      {
        AllTypes complex = new AllTypes(false);
        complex.setString("hello world");
        complex.setNumber(5L);
        complex.setBool(true);
        complex.setNumberArray(Arrays.asList(6L, 7L));
        complex.setBoolArray(Arrays.asList(true, false));

        ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
        multiComplex.add(complex);
        multiComplex.add(complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(multiComplex)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        {
          AllTypes previousComplex = new AllTypes(false);
          previousComplex.setString("world, hello");
          previousComplex.setNumber(55L);
          previousComplex.setBool(false);
          previousComplex.setNumberArray(Arrays.asList(66L, 77L));
          previousComplex.setBoolArray(Arrays.asList(false, true));
          previousComplex.setBinaryArray(Arrays.asList("Mtox".getBytes(StandardCharsets.UTF_8)));
          allTypes.setMultiComplex(Arrays.asList(previousComplex));
        }

        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getMultiComplex().size());
        for ( int i = 0 ; i < patchedResource.getMultiComplex().size() ; i++ )
        {
          AllTypes patchedComplex = patchedResource.getMultiComplex().get(i);
          Assertions.assertEquals("hello world", patchedComplex.getString().get());
          Assertions.assertEquals(5L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(true, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
          Assertions.assertEquals(0, patchedComplex.getBinaryArray().size());
        }
      }

      /**
       * add one multiComplex attribute by direct path reference
       */
      @DisplayName("success: add one multivalued complex (no previous elements)")
      @ParameterizedTest
      @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
      public void testAddOneMultivaluedComplex(String attributeName)
      {
        AllTypes complex = new AllTypes(false);
        complex.setString("hello world");
        complex.setNumber(5L);
        complex.setBool(true);
        complex.setNumberArray(Arrays.asList(6L, 7L));
        complex.setBoolArray(Arrays.asList(true, false));

        ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
        multiComplex.add(complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(multiComplex)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(1, patchedResource.getMultiComplex().size());
        AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
        Assertions.assertEquals("hello world", patchedComplex.getString().get());
        Assertions.assertEquals(5L, patchedComplex.getNumber().get());
        Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
        Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
        Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
        Assertions.assertEquals(true, patchedComplex.getBool().get());
        Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
        Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
        Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
        Assertions.assertEquals(0, patchedComplex.getBinaryArray().size());
      }

      /**
       * add two multiComplex attributes by direct path reference with previous existing elements
       */
      @DisplayName("success: add one multivalued complex (with previous elements)")
      @ParameterizedTest
      @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
      public void testAddOneMultivaluedComplexWithPrevious(String attributeName)
      {
        AllTypes complex = new AllTypes(false);
        complex.setString("hello world");
        complex.setNumber(5L);
        complex.setBool(true);
        complex.setNumberArray(Arrays.asList(6L, 7L));
        complex.setBoolArray(Arrays.asList(true, false));

        ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
        multiComplex.add(complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.ADD)
                                                                                    .path(attributeName)
                                                                                    .valueNode(multiComplex)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        {
          AllTypes previousComplex = new AllTypes(false);
          previousComplex.setString("world, hello");
          previousComplex.setNumber(55L);
          previousComplex.setBool(false);
          previousComplex.setNumberArray(Arrays.asList(66L, 77L));
          previousComplex.setBoolArray(Arrays.asList(false, true));
          previousComplex.setBinaryArray(Arrays.asList("Mtox".getBytes(StandardCharsets.UTF_8)));
          allTypes.setMultiComplex(Arrays.asList(previousComplex));
        }
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getMultiComplex().size());
        {
          AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
          Assertions.assertEquals("world, hello", patchedComplex.getString().get());
          Assertions.assertEquals(55L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(66L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(77L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(false, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(1));
          Assertions.assertEquals(1, patchedComplex.getBinaryArray().size());
          Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8), patchedComplex.getBinaryArray().get(0));
        }
        {
          AllTypes patchedComplex = patchedResource.getMultiComplex().get(1);
          Assertions.assertEquals("hello world", patchedComplex.getString().get());
          Assertions.assertEquals(5L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(true, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
          Assertions.assertEquals(0, patchedComplex.getBinaryArray().size());
        }
      }

      /**
       * replace multivalued complex with filter
       */
      @DisplayName("success: replace one multivalued complex (with filter expression)")
      @ParameterizedTest
      @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
      public void testReplaceOneMultivaluedComplexWithFilter(String attributeName)
      {
        final String filterExpression = String.format("%s[string eq \"world, hello\"]", attributeName);

        AllTypes complex = new AllTypes(false);
        complex.setString("hello world");
        complex.setNumber(5L);
        complex.setBool(true);
        complex.setNumberArray(Arrays.asList(6L, 7L));
        complex.setBoolArray(Arrays.asList(true, false));

        ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
        multiComplex.add(complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(filterExpression)
                                                                                    .valueNode(multiComplex)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        {
          AllTypes previousComplex = new AllTypes(false);
          previousComplex.setString("world, hello");
          previousComplex.setNumber(55L);
          previousComplex.setBool(false);
          previousComplex.setNumberArray(Arrays.asList(66L, 77L));
          previousComplex.setBoolArray(Arrays.asList(false, true));
          previousComplex.setBinaryArray(Arrays.asList("Mtox".getBytes(StandardCharsets.UTF_8)));
          allTypes.setMultiComplex(Arrays.asList(previousComplex));
        }
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(1, patchedResource.getMultiComplex().size());
        {
          AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
          Assertions.assertEquals("hello world", patchedComplex.getString().get());
          Assertions.assertEquals(5L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(true, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
          Assertions.assertEquals(0, patchedComplex.getBinaryArray().size());
        }
      }

      /**
       * replace multivalued complex with filter
       */
      @DisplayName("success: add one multivalued complex (with filter expression)")
      @ParameterizedTest
      @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
      public void testAddOneMultivaluedComplexWithFilter(String attributeName)
      {
        final String filterExpression = String.format("%s[string eq \"world, hello\"]", attributeName);

        AllTypes complex = new AllTypes(false);
        complex.setString("hello world");
        complex.setNumber(5L);
        complex.setBool(true);
        complex.setNumberArray(Arrays.asList(6L, 7L));
        complex.setBoolArray(Arrays.asList(true, false));

        ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
        multiComplex.add(complex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.ADD)
                                                                                    .path(filterExpression)
                                                                                    .valueNode(multiComplex)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        {
          AllTypes previousComplex = new AllTypes(false);
          previousComplex.setString("world, hello");
          previousComplex.setNumber(55L);
          previousComplex.setBool(false);
          previousComplex.setNumberArray(Arrays.asList(66L, 77L));
          previousComplex.setBoolArray(Arrays.asList(false, true));
          previousComplex.setBinaryArray(Arrays.asList("Mtox".getBytes(StandardCharsets.UTF_8)));
          allTypes.setMultiComplex(Arrays.asList(previousComplex));
        }
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(1, patchedResource.getMultiComplex().size());
        {
          AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
          Assertions.assertEquals("hello world", patchedComplex.getString().get());
          Assertions.assertEquals(5L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(true, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
          Assertions.assertEquals(1, patchedComplex.getBinaryArray().size());
          Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8), patchedComplex.getBinaryArray().get(0));
        }
      }

    }

  }

  @DisplayName("Multivalued Complex simple-sub-attribute validation tests")
  @Nested
  public class MultivaluedComplexSimpleSubAttributesTests
  {

    /**
     * success-tests are not needed, they are already covered by
     * {@link MultivaluedComplexAttributesTests.ResourcePatchTests}
     */
    @DisplayName("Resource-Patch Tests")
    @Nested
    class ResourcePatchTests
    {

      /**
       * Replace a number with string on multivalued complex
       */
      @DisplayName("failure: replace multiComplex.number with string value")
      @Test
      public void testReplaceMultiComplexNumberWithString()
      {
        final String attributeName = "multiComplex.number";
        SchemaAttribute multiComplexNumberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        SchemaAttribute multiComplexAttribute = multiComplexNumberAttribute.getParent();

        AllTypes patchResource = new AllTypes(true);

        AllTypes complex = new AllTypes(false);
        JsonNode numberNode = new TextNode("illegal-value");
        complex.set(multiComplexNumberAttribute.getName(), numberNode);

        ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
        multiComplex.add(complex);
        patchResource.set(multiComplexAttribute.getName(), multiComplex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);

        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                              + "with value '\"illegal-value\"'",
                                              multiComplexNumberAttribute.getFullResourceName()),
                                errorResponse.getDetail().get());
        {
          List<String> fieldErrors = errorResponse.getFieldErrors().get(multiComplexAttribute.getScimNodeName());
          Assertions.assertEquals(1, fieldErrors.size());
          Assertions.assertEquals("Found unsupported value in multivalued complex attribute '[{\"number\":\"illegal-value\"}]'",
                                  fieldErrors.get(0));
        }
        {
          List<String> fieldErrors = errorResponse.getFieldErrors().get(multiComplexNumberAttribute.getScimNodeName());
          Assertions.assertEquals(1, fieldErrors.size());
          Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                                + "with value '\"illegal-value\"'",
                                                multiComplexNumberAttribute.getFullResourceName()),
                                  fieldErrors.get(0));
        }
      }
    }

    /**
     * success-tests are not needed, they are already covered by
     * {@link MultivaluedComplexAttributesTests.ResourcePatchTests}
     */
    @DisplayName("Path-Patch Tests")
    @Nested
    class PathPatchTests
    {


      /**
       * Replace a number with string on multivalued complex
       */
      @DisplayName("failure: replace multiComplex.number with string value")
      @Test
      public void testReplaceMultiComplexNumberWithString()
      {
        final String attributeName = "multiComplex.number";
        SchemaAttribute multiComplexNumberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

        JsonNode numberNode = new TextNode("illegal-value");

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(numberNode)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);

        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                              + "with value '\"illegal-value\"'",
                                              multiComplexNumberAttribute.getFullResourceName()),
                                errorResponse.getDetail().get());
        Assertions.assertEquals(1, errorResponse.getFieldErrors().size());
        {
          List<String> fieldErrors = errorResponse.getFieldErrors().get(multiComplexNumberAttribute.getScimNodeName());
          Assertions.assertEquals(1, fieldErrors.size());
          Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                                + "with value '\"illegal-value\"'",
                                                multiComplexNumberAttribute.getFullResourceName()),
                                  fieldErrors.get(0));
        }
      }

    }

  }

  @DisplayName("Multivalued Complex multivalued-sub-attribute validation tests")
  @Nested
  public class MultivaluedComplexMultiSubAttributesTests
  {

    /**
     * success-tests are not needed, they are already covered by
     * {@link MultivaluedComplexAttributesTests.ResourcePatchTests}
     */
    @DisplayName("Resource-Patch Tests")
    @Nested
    class ResourcePatchTests
    {

      /**
       * Replace a number with string on multivalued complex
       */
      @DisplayName("failure: replace multiComplex.number with string value")
      @Test
      public void testReplaceMultiComplexNumberWithString()
      {
        final String attributeName = "multiComplex.numberArray";
        SchemaAttribute multiComplexNumberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
        SchemaAttribute multiComplexAttribute = multiComplexNumberAttribute.getParent();

        AllTypes patchResource = new AllTypes(true);

        AllTypes complex = new AllTypes(false);
        JsonNode numberNode = new TextNode("illegal-value");
        ArrayNode numberArrayNode = new ArrayNode(JsonNodeFactory.instance);
        numberArrayNode.add(numberNode);

        complex.set(multiComplexNumberAttribute.getName(), numberArrayNode);

        ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
        multiComplex.add(complex);
        patchResource.set(multiComplexAttribute.getName(), multiComplex);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);

        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"illegal-value\"]'",
                                errorResponse.getDetail().get());
        Assertions.assertEquals(2, errorResponse.getFieldErrors().size());
        {
          List<String> fieldErrors = errorResponse.getFieldErrors().get(multiComplexAttribute.getScimNodeName());
          Assertions.assertEquals(1, fieldErrors.size());
          Assertions.assertEquals("Found unsupported value in multivalued complex attribute '[{\"numberArray\":[\"illegal-value\"]}]'",
                                  fieldErrors.get(0));
        }
        {
          List<String> fieldErrors = errorResponse.getFieldErrors().get(multiComplexNumberAttribute.getScimNodeName());
          Assertions.assertEquals(2, fieldErrors.size());
          Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"illegal-value\"]'",
                                  fieldErrors.get(0));
          Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                                + "with value '\"illegal-value\"'",
                                                multiComplexNumberAttribute.getFullResourceName()),
                                  fieldErrors.get(1));
        }
      }
    }

    /**
     * success-tests are not needed, they are already covered by
     * {@link MultivaluedComplexAttributesTests.ResourcePatchTests}
     */
    @DisplayName("Path-Patch Tests")
    @Nested
    class PathPatchTests
    {


      /**
       * Replace a number with string on multivalued complex
       */
      @DisplayName("failure: replace multiComplex.numberArray with string value")
      @Test
      public void testReplaceMultiComplexNumberWithString()
      {
        final String attributeName = "multiComplex.numberArray";
        SchemaAttribute multiComplexNumberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

        JsonNode numberNode = new TextNode("illegal-value");

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(numberNode)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        ErrorResponse errorResponse = new ErrorResponse(ex);
        ex.getValidationContext().writeToErrorResponse(errorResponse);

        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"illegal-value\"]'",
                                errorResponse.getDetail().get());
        Assertions.assertEquals(1, errorResponse.getFieldErrors().size());
        {
          List<String> fieldErrors = errorResponse.getFieldErrors().get(multiComplexNumberAttribute.getScimNodeName());
          Assertions.assertEquals(2, fieldErrors.size());
          Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"illegal-value\"]'",
                                  fieldErrors.get(0));
          Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                                + "with value '\"illegal-value\"'",
                                                multiComplexNumberAttribute.getFullResourceName()),
                                  fieldErrors.get(1));
        }
      }

    }

  }
}
