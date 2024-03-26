package de.captaingoldfish.scim.sdk.server.patch.validationtests;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 19.01.2024
 */
@Slf4j
@DisplayName("Complex attribute tests")
public class ComplexAttributeTests extends AbstractPatchTest
{


  @DisplayName("Resource-Patch Tests")
  @Nested
  class ResourcePatchTests
  {

    @DisplayName("Main resource Tests")
    @Nested
    class MainResourceTests
    {


      @DisplayName("complex attribute as full object")
      @Nested
      class ComplexAttributeAsFullObjectTests
      {

        /**
         * sets the string-value of the allTypes object
         */
        @DisplayName("success: add complex.string value")
        @Test
        public void testAddStringValue()
        {
          AllTypes patchObject = new AllTypes(true);

          AllTypes complex = new AllTypes(false);
          complex.setString("hello world");
          patchObject.setComplex(complex);

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
          Assertions.assertEquals("hello world", patchedResource.getComplex().flatMap(AllTypes::getString).get());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a string into the complex.numberArray attribute with the msAzure style attribute reference
         */
        @DisplayName("success: add complex.numberArray")
        @Test
        public void testAddNumberArray()
        {
          AllTypes patchResource = new AllTypes(true);

          ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
          numberArray.add(new IntNode(5));
          numberArray.add(new IntNode(6));

          AllTypes complex = new AllTypes(false);
          complex.set("numberArray", numberArray);
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
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * remove a complex attribute
         */
        @DisplayName("success: remove complex attribute")
        @Test
        public void testRemoveComplex()
        {
          AllTypes patchResource = new AllTypes(true);

          patchResource.set("complex", NullNode.getInstance());

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .valueNode(patchResource)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          {
            ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
            numberArray.add(new IntNode(5));
            numberArray.add(new IntNode(6));

            AllTypes complex = new AllTypes(false);
            complex.set("numberArray", numberArray);
            allTypes.setComplex(complex);
          }
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertTrue(patchRequestHandler.isResourceChanged());
          Assertions.assertFalse(patchedResource.getComplex().isPresent());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a string into the complex.numberArray attribute
         */
        @DisplayName("failure: complex.numberArray with string value")
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

      @DisplayName("Ms Azure style attribute reference tests")
      @Nested
      class MsAzureAttributeReferenceStyleTests
      {

        /**
         * adds a string into the complex.numberArray attribute with the msAzure style attribute reference
         */
        @DisplayName("success: add complex.string")
        @ParameterizedTest
        @ValueSource(strings = {"complex.string", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.string"})
        public void testAddNumberArrayMsAzureStyle(String attributeName)
        {
          AllTypes patchResource = new AllTypes(true);
          patchResource.set(attributeName, new TextNode("hello world"));

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
          Assertions.assertEquals("hello world", patchedResource.getComplex().flatMap(AllTypes::getString).get());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a string into the complex.numberArray attribute with the msAzure style attribute reference
         */
        @DisplayName("success: add complex.numberArray")
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
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a string into the complex.numberArray attribute with the msAzure style attribute reference
         */
        @DisplayName("failure: numberArray with string value")
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

          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
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

      @DisplayName("complex attribute as full object Tests")
      @Nested
      class ComplexAttributeAsFullObjectTests
      {

        /**
         * sets the string-value of the allTypes object
         */
        @DisplayName("success: add complex.string value")
        @Test
        public void testAddStringValue()
        {
          AllTypes patchObject = new AllTypes(true);

          AllTypes complex = new AllTypes(false);
          complex.setString("hello world");
          EnterpriseUser enterpriseUser = new EnterpriseUser();
          enterpriseUser.set("complex", complex);
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
          ObjectNode patchedComplex = (ObjectNode)patchedEnterpriseUser.get("complex");
          JsonNode stringNode = patchedComplex.get("string");
          Assertions.assertEquals("hello world", stringNode.textValue());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a string into the complex.numberArray attribute with the msAzure style attribute reference
         */
        @DisplayName("success: add complex.numberArray")
        @Test
        public void testAddNumberArray()
        {
          AllTypes patchObject = new AllTypes(true);

          ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
          numberArray.add(new IntNode(5));
          numberArray.add(new IntNode(6));

          AllTypes complex = new AllTypes(false);
          complex.set("numberArray", numberArray);
          EnterpriseUser enterpriseUser = new EnterpriseUser();
          enterpriseUser.set("complex", complex);
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
          ObjectNode patchedComplex = (ObjectNode)patchedEnterpriseUser.get("complex");
          ArrayNode patchedNumberNode = (ArrayNode)patchedComplex.get("numberArray");
          Assertions.assertEquals(2, patchedNumberNode.size());
          Assertions.assertEquals(5L, patchedNumberNode.get(0).longValue());
          Assertions.assertEquals(6L, patchedNumberNode.get(1).longValue());

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
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * remove a complex attribute
         */
        @DisplayName("success: remove complex")
        @Test
        public void testRemoveComplex()
        {
          AllTypes patchObject = new AllTypes(true);

          EnterpriseUser enterpriseUser = new EnterpriseUser();
          enterpriseUser.set("complex", NullNode.getInstance());
          patchObject.setEnterpriseUser(enterpriseUser);

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .valueNode(patchObject)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          {
            ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
            numberArray.add(new IntNode(5));
            numberArray.add(new IntNode(6));

            AllTypes complex = new AllTypes(false);
            complex.set("numberArray", numberArray);
            EnterpriseUser originalEnterpriseUser = new EnterpriseUser();
            originalEnterpriseUser.setCostCenter("hello world");
            originalEnterpriseUser.set("complex", complex);
            allTypes.setEnterpriseUser(originalEnterpriseUser);
          }
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertTrue(patchRequestHandler.isResourceChanged());
          EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
          JsonNode patchedComplex = patchedEnterpriseUser.get("complex");
          Assertions.assertNull(patchedComplex);
          Assertions.assertEquals("hello world", patchedEnterpriseUser.getCostCenter().get());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

      }

      @DisplayName("Ms Azure style attribute reference tests")
      @Nested
      class MsAzureAttributeReferenceStyleTests
      {

        /**
         * adds a string into the complex.numberArray attribute with the msAzure style attribute reference
         */
        @DisplayName("success: add complex.string")
        @Test
        public void testAddNumberArrayMsAzureStyle()
        {
          final String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:complex.string";
          AllTypes patchResource = new AllTypes(true);
          patchResource.set(attributeName, new TextNode("hello world"));

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
          EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
          ObjectNode patchedComplex = (ObjectNode)patchedEnterpriseUser.get("complex");
          JsonNode stringNode = patchedComplex.get("string");
          Assertions.assertEquals("hello world", stringNode.textValue());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a string into the complex.numberArray attribute with the msAzure style attribute reference
         */
        @DisplayName("success: add complex.numberArray")
        @Test
        public void testAddSubNumberArrayWithMsAzureStyleRef()
        {
          final String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:complex.numberArray";
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
          EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
          ObjectNode patchedComplex = (ObjectNode)patchedEnterpriseUser.get("complex");
          ArrayNode patchedNumberNode = (ArrayNode)patchedComplex.get("numberArray");
          Assertions.assertEquals(2, patchedNumberNode.size());
          Assertions.assertEquals(5L, patchedNumberNode.get(0).longValue());
          Assertions.assertEquals(6L, patchedNumberNode.get(1).longValue());

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
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

      }
    }
  }

  @DisplayName("Path-Patch Tests")
  @Nested
  class PathPatchTests
  {

    @DisplayName("Full complex attribute patch")
    @Nested
    class FullComplexAttributePatch
    {

      @DisplayName("Main resource Tests")
      @Nested
      class MainResourceTests
      {

        /**
         * adds a string value and a numberArray with a full complex type update
         */
        @DisplayName("success: add complex.string")
        @ParameterizedTest
        @ValueSource(strings = {"complex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex"})
        public void testAddStringAndNumberArrayToComplex(String attributeName)
        {
          AllTypes complex = new AllTypes(false);
          complex.setString("hello world");
          complex.setNumberArray(Arrays.asList(5L, 6L));

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path(attributeName)
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
          Assertions.assertEquals("hello world", patchedResource.getComplex().flatMap(AllTypes::getString).get());
          Assertions.assertEquals(2, patchedResource.getComplex().get().getNumberArray().size());
          Assertions.assertEquals(5L, patchedResource.getComplex().get().getNumberArray().get(0));
          Assertions.assertEquals(6L, patchedResource.getComplex().get().getNumberArray().get(1));

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex attribute as a string-representation in an array structure with a single attribute. This is
         * a special case that is actually invalid syntax. The SCIM SDK should still support this case because it
         * might happen more often than expected that requests will contain the details like this.
         */
        @DisplayName("success: add complex as string representation in array")
        @ParameterizedTest
        @ValueSource(strings = {"complex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex"})
        public void testAddStringAndNumberArrayToComplexAsString(String attributeName)
        {
          AllTypes complex = new AllTypes(false);
          complex.setString("hello world");
          complex.setNumberArray(Arrays.asList(5L, 6L));

          ArrayNode valueNode = new ArrayNode(JsonNodeFactory.instance);
          valueNode.add(complex.toString());
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
          AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals("hello world", patchedResource.getComplex().flatMap(AllTypes::getString).get());
          Assertions.assertEquals(2, patchedResource.getComplex().get().getNumberArray().size());
          Assertions.assertEquals(5L, patchedResource.getComplex().get().getNumberArray().get(0));
          Assertions.assertEquals(6L, patchedResource.getComplex().get().getNumberArray().get(1));

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * remove a complex attribute
         */
        @DisplayName("success: remove complex")
        @ParameterizedTest
        @ValueSource(strings = {"complex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex"})
        public void testRemoveComplex(String attributeName)
        {

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REMOVE)
                                                                                      .path(attributeName)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          {
            AllTypes complex = new AllTypes(false);
            complex.set("number", new TextNode("hello world"));
            allTypes.setComplex(complex);
          }
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertFalse(patchedResource.getComplex().isPresent());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a string within a numberValue
         */
        @DisplayName("failure: add string into complex.number")
        @ParameterizedTest
        @ValueSource(strings = {"complex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex"})
        public void testAddStringForNumberValue(String attributeName)
        {
          AllTypes complex = new AllTypes(false);
          complex.set("number", new TextNode("hello world"));

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path(attributeName)
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

          SchemaAttribute complexNumberAttribute = allTypesResourceType.getSchemaAttribute(attributeName + ".number")
                                                                       .get();
          ErrorResponse errorResponse = new ErrorResponse(ex);
          ex.getValidationContext().writeToErrorResponse(errorResponse);
          Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                                + "with value '\"hello world\"'",
                                                complexNumberAttribute.getFullResourceName()),
                                  errorResponse.getDetail().get());
          Map<String, List<String>> fieldErrors = errorResponse.getFieldErrors();
          Assertions.assertEquals(1, fieldErrors.size());
          List<String> complexNumberErrors = fieldErrors.get("complex.number");
          Assertions.assertEquals(1, complexNumberErrors.size());
          Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                                + "with value '\"hello world\"'",
                                                complexNumberAttribute.getFullResourceName()),
                                  complexNumberErrors.get(0));

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
         * adds a string value and a numberArray with a full complex type update
         */
        @DisplayName("success: add complex.string and complex.numberArray")
        @Test
        public void testAddStringAndNumberArrayToComplex()
        {
          String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:complex";
          AllTypes complex = new AllTypes(false);
          complex.setString("hello world");
          complex.setNumberArray(Arrays.asList(5L, 6L));

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path(attributeName)
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

          EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
          ObjectNode patchedComplex = (ObjectNode)patchedEnterpriseUser.get("complex");
          ArrayNode patchedNumberArray = (ArrayNode)patchedComplex.get("numberArray");

          Assertions.assertEquals("hello world", patchedComplex.get("string").textValue());
          Assertions.assertEquals(2, patchedNumberArray.size());
          Assertions.assertEquals(5L, patchedNumberArray.get(0).longValue());
          Assertions.assertEquals(6L, patchedNumberArray.get(1).longValue());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * remove a complex attribute
         */
        @DisplayName("success: remove complex")
        @Test
        public void testRemoveComplex()
        {
          String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:complex";
          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REMOVE)
                                                                                      .path(attributeName)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          {
            AllTypes complex = new AllTypes(false);
            EnterpriseUser enterpriseUser = new EnterpriseUser();
            enterpriseUser.setCostCenter("hello world");
            complex.set("number", new TextNode("hello world"));
            enterpriseUser.set("complex", complex);
            allTypes.setEnterpriseUser(enterpriseUser);
          }
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertTrue(patchedResource.getEnterpriseUser().isPresent());
          EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
          Assertions.assertNull(patchedEnterpriseUser.get("complex"));
          Assertions.assertEquals("hello world", patchedEnterpriseUser.getCostCenter().get());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

      }
    }

    @DisplayName("sub-attribute patch on complex")
    @Nested
    class SubAttributePatchOnComplex
    {

      @DisplayName("Main resource Tests")
      @Nested
      class MainResourceTests
      {

        /**
         * adds a string value into its appropriate place in a complex-attribute
         */
        @DisplayName("success: add complex.string")
        @ParameterizedTest
        @ValueSource(strings = {"complex.string", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.string"})
        public void testAddStringValueToComplex(String attributeName)
        {
          JsonNode stringNode = new TextNode("hello world");
          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path(attributeName)
                                                                                      .valueNode(stringNode)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals("hello world", patchedResource.getComplex().flatMap(AllTypes::getString).get());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * remove the attribute complex.string
         */
        @DisplayName("success: remove complex.string")
        @ParameterizedTest
        @ValueSource(strings = {"complex.string", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.string"})
        public void testRemoveStringValueToComplex(String attributeName)
        {
          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REMOVE)
                                                                                      .path(attributeName)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          {
            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            allTypes.setComplex(complex);
          }
          addAllTypesToProvider(allTypes);
          log.warn(allTypes.toPrettyString());
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertTrue(patchRequestHandler.isResourceChanged());
          Assertions.assertFalse(patchedResource.getComplex().flatMap(AllTypes::getString).isPresent());
          Assertions.assertTrue(patchedResource.getComplex().flatMap(AllTypes::getNumber).isPresent());
          Assertions.assertEquals(5L, patchedResource.getComplex().flatMap(AllTypes::getNumber).get());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a number-array into its appropriate place in a complex-attribute
         */
        @DisplayName("success: add complex.numberArray")
        @ParameterizedTest
        @ValueSource(strings = {"complex.numberArray",
                                "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.numberArray"})
        public void testAddNumberArrayToComplex(String attributeName)
        {
          ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
          numberArray.add(new IntNode(5));
          numberArray.add(new IntNode(6));

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
          Assertions.assertEquals(2, patchedResource.getComplex().get().getNumberArray().size());
          Assertions.assertEquals(5L, patchedResource.getComplex().get().getNumberArray().get(0));
          Assertions.assertEquals(6L, patchedResource.getComplex().get().getNumberArray().get(1));

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
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
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

          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
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
         * adds a string value into its appropriate place in a complex-attribute
         */
        @DisplayName("success: add complex.string")
        @Test
        public void testAddStringValueToComplex()
        {
          String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:complex.string";
          JsonNode stringNode = new TextNode("hello world");
          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path(attributeName)
                                                                                      .valueNode(stringNode)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertTrue(patchRequestHandler.isResourceChanged());
          EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
          ObjectNode patchedComplex = (ObjectNode)patchedEnterpriseUser.get("complex");
          JsonNode patchedStringNode = patchedComplex.get("string");
          Assertions.assertEquals("hello world", patchedStringNode.textValue());

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          }
          // must not be called
          {
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a number-array into its appropriate place in a complex-attribute
         */
        @DisplayName("success: add complex.numberArray")
        @Test
        public void testAddNumberArrayToComplex()
        {
          String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:complex.numberArray";
          ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
          numberArray.add(new IntNode(5));
          numberArray.add(new IntNode(6));

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
          ObjectNode patchedComplex = (ObjectNode)patchedEnterpriseUser.get("complex");
          ArrayNode patchedNumberArray = (ArrayNode)patchedComplex.get("numberArray");

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
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a string into the complex.numberArray attribute
         */
        @DisplayName("failure: sub-attribute numberArray with string value")
        @Test
        public void testWrongTypeOnNumberWithSimpleAttributeRef()
        {
          String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:complex.numberArray";
          SchemaAttribute numberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

          ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
          numberArray.add(new TextNode("hello"));

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
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
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
    }


  }
}
