package de.captaingoldfish.scim.sdk.server.patch.validationtests;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
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
@DisplayName("Simple attribute tests")
public class SimpleAttributeTests extends AbstractPatchTest
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
       * replace a simple number value
       */
      @DisplayName("success: replace simple number value")
      @ParameterizedTest
      @ValueSource(strings = {"number", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:number"})
      public void testReplaceSimpleNumberValue(String attributeName)
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
        AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertTrue(patchRequestHandler.isResourceChanged());
        Assertions.assertEquals(5L, patchedAllTypes.getNumber().get());

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
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * replace a simple number value with an array value
       */
      @DisplayName("success: replace simple number value with array")
      @ParameterizedTest
      @ValueSource(strings = {"number", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:number"})
      public void testReplaceSimpleNumberValueWithArray(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        arrayNode.add(5);
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
        AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertTrue(patchRequestHandler.isResourceChanged());
        Assertions.assertEquals(5L, patchedAllTypes.getNumber().get());

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
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * remove a simple number value
       */
      @DisplayName("success: Remove simple number value")
      @ParameterizedTest
      @ValueSource(strings = {"number", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:number"})
      public void testRemoveSimpleNumberValue(String attributeName)
      {
        AllTypes patchResource = new AllTypes(true);
        patchResource.set(attributeName, NullNode.getInstance());

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        allTypes.setNumber(5L);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertTrue(patchRequestHandler.isResourceChanged());
        Assertions.assertFalse(patchedAllTypes.getNumber().isPresent());

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
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

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

    @DisplayName("Resource-Patch Tests")
    @Nested
    class ExtensionTests
    {

      /**
       * replace costCenter value
       */
      @DisplayName("success: replace costCenter value")
      @Test
      public void testReplaceCostCenter()
      {
        AllTypes patchResource = new AllTypes(true);
        patchResource.setEnterpriseUser(EnterpriseUser.builder().costCenter("new-value").build());

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
        AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertTrue(patchRequestHandler.isResourceChanged());
        Assertions.assertEquals("new-value",
                                patchedAllTypes.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).get());

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
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * remove costCenter
       */
      @DisplayName("success: Remove costCenter")
      @Test
      public void testRemoveSimpleNumberValue()
      {
        AllTypes patchResource = new AllTypes(true);
        EnterpriseUser enterpriseUser = new EnterpriseUser();
        enterpriseUser.set("costCenter", NullNode.getInstance());
        patchResource.setEnterpriseUser(enterpriseUser);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .valueNode(patchResource)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("costCenter").build());
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertTrue(patchRequestHandler.isResourceChanged());
        Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).isPresent());

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
       * replace a simple number value
       */
      @DisplayName("success: replace simple number value")
      @ParameterizedTest
      @ValueSource(strings = {"number", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:number"})
      public void testReplaceSimpleNumberValue(String attributeName)
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
        AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertTrue(patchRequestHandler.isResourceChanged());
        Assertions.assertEquals(5L, patchedAllTypes.getNumber().get());

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
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * replace a simple number value with an array value
       */
      @DisplayName("success: replace simple number value with array")
      @ParameterizedTest
      @ValueSource(strings = {"number", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:number"})
      public void testReplaceSimpleNumberValueWithArray(String attributeName)
      {
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        arrayNode.add(5);

        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(arrayNode)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertTrue(patchRequestHandler.isResourceChanged());
        Assertions.assertEquals(5L, patchedAllTypes.getNumber().get());

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
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * remove a simple number value
       */
      @DisplayName("success: Remove simple number value")
      @ParameterizedTest
      @ValueSource(strings = {"number", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:number"})
      public void testRemoveSimpleNumberValue(String attributeName)
      {
        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REMOVE)
                                                                                    .path(attributeName)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        allTypes.setNumber(5L);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertTrue(patchRequestHandler.isResourceChanged());
        Assertions.assertFalse(patchedAllTypes.getNumber().isPresent());

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
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

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
    class ExtensionTests
    {

      /**
       * replace costCenter
       */
      @DisplayName("success: replace costCenter")
      @ParameterizedTest
      @ValueSource(strings = {"costCenter", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter"})
      public void testReplaceCostCenter(String attributeName)
      {
        final JsonNode costCenter = new TextNode("new-value");
        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REPLACE)
                                                                                    .path(attributeName)
                                                                                    .valueNode(costCenter)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertTrue(patchRequestHandler.isResourceChanged());
        Assertions.assertEquals("new-value",
                                patchedAllTypes.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).get());

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
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }

      /**
       * remove costCenter
       */
      @DisplayName("success: Remove costCenter")
      @ParameterizedTest
      @ValueSource(strings = {"costCenter", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter"})
      public void testRemoveCostCenter(String attributeName)
      {
        List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                    .op(PatchOp.REMOVE)
                                                                                    .path(attributeName)
                                                                                    .build());
        PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

        AllTypes allTypes = new AllTypes(true);
        allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("costCenter").build());
        addAllTypesToProvider(allTypes);
        PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                    allTypesResourceType.getResourceHandlerImpl(),
                                                                                    resourceEndpoint.getPatchWorkarounds(),
                                                                                    new Context(null));
        AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
        Assertions.assertTrue(patchRequestHandler.isResourceChanged());
        Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).isPresent());

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
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
        }
      }
    }
  }
}
