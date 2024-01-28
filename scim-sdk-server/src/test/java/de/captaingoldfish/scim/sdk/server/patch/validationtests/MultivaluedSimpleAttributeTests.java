package de.captaingoldfish.scim.sdk.server.patch.validationtests;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.validation.RequestContextException;
import de.captaingoldfish.scim.sdk.server.patch.PatchRequestHandler;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexMultivaluedSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedComplexSimpleSubAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.MultivaluedSimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.RemoveComplexAttributeOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.RemoveExtensionRefOperation;
import de.captaingoldfish.scim.sdk.server.patch.operations.SimpleAttributeOperation;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;


/**
 * @author Pascal Knueppel
 * @since 19.01.2024
 */
@DisplayName("Simple multivalued attribute tests")
public class MultivaluedSimpleAttributeTests extends AbstractPatchTest
{


  @DisplayName("Resource-Patch Tests")
  @Nested
  class ResourcePatchTests
  {

    @DisplayName("Main resource Tests")
    @Nested
    class MainResourceTests
    {

      /**
       * Verifies that a simple multivalued attribute can be replaced
       */
      @DisplayName("success: replace numberArray")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testReplaceNumberArray(String attributeName)
      {
        AllTypes patchObject = new AllTypes(true);
        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(5);
        numberArray.add(6);
        patchObject.set(attributeName, numberArray);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchObject)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getNumberArray().size());
        Assertions.assertEquals(5L, patchedResource.getNumberArray().get(0));
        Assertions.assertEquals(6L, patchedResource.getNumberArray().get(1));

        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

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
        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * Verifies that no exception is thrown if the numberArray is assigned a simple value that is not an array
       */
      @DisplayName("success: Remove numberArray")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testRemoveNumberArray(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);
        patchResource.set(attributeName, NullNode.getInstance());

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        allTypes.setNumberArray(Arrays.asList(5L, 6L, 7L));
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(0, patchedResource.getNumberArray().size());
        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * adds a string into the numberArray attribute
       */
      @DisplayName("failure: NumberArray with non-array string value")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testWrongTypeOnNumberWithNonArray(String attributeName)
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

        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\"]'",
                                errorResponse.getDetail().get());
        List<String> fieldErrors = errorResponse.getFieldErrors().get(numberAttribute.getScimNodeName());
        Assertions.assertEquals(2, fieldErrors.size());
        Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"hello\"]'", fieldErrors.get(0));
        Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type "
                                              + "'string' with value '\"hello\"'",
                                              numberAttribute.getFullResourceName()),
                                fieldErrors.get(1));

        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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

        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
      }
    }

    @DisplayName("Extension resource Tests")
    @Nested
    class ExtensionResourceTests
    {

      @BeforeEach
      public void extendEnterpriseUserSchema()
      {
        addCustomAttributesToEnterpriseUserSchema();
      }

      /**
       * Verifies that a simple multivalued attribute can be replaced
       */
      @DisplayName("success: replace numberArray")
      @Test
      public void testReplaceNumberArray()
      {
        AllTypes patchObject = new AllTypes(true);
        EnterpriseUser enterpriseUser = new EnterpriseUser();
        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(5);
        numberArray.add(6);
        enterpriseUser.set("numberArray", numberArray);
        patchObject.setEnterpriseUser(enterpriseUser);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchObject)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
        ArrayNode patchedNumberArray = (ArrayNode)patchedEnterpriseUser.get("numberArray");
        Assertions.assertEquals(2, patchedNumberArray.size());
        Assertions.assertEquals(5L, patchedNumberArray.get(0).longValue());
        Assertions.assertEquals(6L, patchedNumberArray.get(1).longValue());

        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * Verifies that a simple multivalued attribute can be replaced
       */
      @DisplayName("success: replace numberArray msAzure style")
      @Test
      public void testReplaceNumberArrayMsAzureStyle()
      {
        String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:numberArray";
        AllTypes patchObject = new AllTypes(true);
        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(5);
        numberArray.add(6);
        patchObject.set(attributeName, numberArray);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchObject)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        EnterpriseUser enterpriseUser = patchedResource.getEnterpriseUser().get();
        ArrayNode patchedNumberArray = (ArrayNode)enterpriseUser.get("numberArray");
        Assertions.assertEquals(2, patchedNumberArray.size());
        Assertions.assertEquals(5L, patchedNumberArray.get(0).longValue());
        Assertions.assertEquals(6L, patchedNumberArray.get(1).longValue());

        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

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
        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * Verifies that no exception is thrown if the numberArray is assigned a simple value that is not an array
       */
      @DisplayName("success: Remove numberArray")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testRemoveNumberArray(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);
        patchResource.set(attributeName, NullNode.getInstance());

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        allTypes.setNumberArray(Arrays.asList(5L, 6L, 7L));
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(0, patchedResource.getNumberArray().size());
        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }
    }
  }

  @DisplayName("Path-Patch Tests")
  @Nested
  class PathPatchTests
  {

    @DisplayName("Main resource Tests")
    @Nested
    class MainResourceTests
    {

      /**
       * Verifies that a simple multivalued attribute can be easily replaced
       */
      @DisplayName("success: replace numberArray")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testReplaceNumberArray(String attributeName)
      {
        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(5);
        numberArray.add(6);

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
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getNumberArray().size());
        Assertions.assertEquals(5L, patchedResource.getNumberArray().get(0));
        Assertions.assertEquals(6L, patchedResource.getNumberArray().get(1));

        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

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

        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * Verifies that no exception is thrown if the numberArray is assigned a simple value that is not an array
       */
      @DisplayName("success: Remove numberArray")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testRemoveNumberArray(String attributeName)
      {
        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REMOVE)
                                                                                    .path(attributeName)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        allTypes.setNumberArray(Arrays.asList(5L, 6L, 7L));
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(0, patchedResource.getNumberArray().size());
        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * Verifies that no exception is thrown if the numberArray is assigned a simple value that is not an array
       */
      @DisplayName("success: Remove value from numberArray with filter")
      @ParameterizedTest
      @ValueSource(strings = {"numberArray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberArray"})
      public void testRemoveElementFromNumberArrayWithFilter(String attributeName)
      {
        final String filterPath = String.format("%s[value eq 6]", attributeName);
        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REMOVE)
                                                                                    .path(filterPath)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        allTypes.setNumberArray(Arrays.asList(5L, 6L, 7L));
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(2, patchedResource.getNumberArray().size());
        Assertions.assertEquals(5L, patchedResource.getNumberArray().get(0));
        Assertions.assertEquals(7L, patchedResource.getNumberArray().get(1));
        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
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

        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
      }
    }

    @DisplayName("Extension resource Tests")
    @Nested
    class ExtensionResourceTests
    {

      @BeforeEach
      public void extendEnterpriseUserSchema()
      {
        addCustomAttributesToEnterpriseUserSchema();
      }

      /**
       * Verifies that a simple multivalued attribute can be replaced
       */
      @DisplayName("success: replace numberArray")
      @Test
      public void testReplaceNumberArray()
      {
        final String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:numberArray";
        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(5);
        numberArray.add(6);

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
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
        ArrayNode patchedNumberArray = (ArrayNode)patchedEnterpriseUser.get("numberArray");
        Assertions.assertEquals(2, patchedNumberArray.size());
        Assertions.assertEquals(5L, patchedNumberArray.get(0).longValue());
        Assertions.assertEquals(6L, patchedNumberArray.get(1).longValue());

        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * Verifies that a simple multivalued attribute can be replaced with a non-array value
       */
      @DisplayName("success: replace numberArray with non-array value")
      @Test
      public void testReplaceNumberArrayWithNonArrayValue()
      {
        final String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:numberArray";

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
        EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
        ArrayNode patchedNumberArray = (ArrayNode)patchedEnterpriseUser.get("numberArray");
        Assertions.assertEquals(1, patchedNumberArray.size());
        Assertions.assertEquals(5L, patchedNumberArray.get(0).longValue());

        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * Verifies that a simple multivalued attribute can be replaced
       */
      @DisplayName("success: replace numberArray msAzure style")
      @Test
      public void testReplaceNumberArrayMsAzureStyle()
      {
        String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:numberArray";
        AllTypes patchObject = new AllTypes(true);
        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(5);
        numberArray.add(6);
        patchObject.set(attributeName, numberArray);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchObject)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        EnterpriseUser enterpriseUser = patchedResource.getEnterpriseUser().get();
        ArrayNode patchedNumberArray = (ArrayNode)enterpriseUser.get("numberArray");
        Assertions.assertEquals(2, patchedNumberArray.size());
        Assertions.assertEquals(5L, patchedNumberArray.get(0).longValue());
        Assertions.assertEquals(6L, patchedNumberArray.get(1).longValue());

        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * Verifies that no exception is thrown if the numberArray is assigned a simple value that is not an array
       */
      @DisplayName("success: Remove numberArray")
      @Test
      public void testRemoveNumberArray()
      {
        final String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:numberArray";
        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REMOVE)
                                                                                    .path(attributeName)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        EnterpriseUser enterpriseUser = new EnterpriseUser();
        ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
        numberArray.add(5);
        numberArray.add(6);
        enterpriseUser.set("numberArray", numberArray);
        allTypes.setEnterpriseUser(enterpriseUser);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertEquals(0, patchedResource.getNumberArray().size());
        // must be called
        {
          Mockito.verify(defaultPatchOperationHandler)
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
        }
        // must not be called
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }
    }
  }
}
