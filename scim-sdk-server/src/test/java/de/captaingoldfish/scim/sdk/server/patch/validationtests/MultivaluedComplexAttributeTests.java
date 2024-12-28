package de.captaingoldfish.scim.sdk.server.patch.validationtests;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.GroupHandlerImpl;
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
@DisplayName("Multivalued Complex attribute tests")
public class MultivaluedComplexAttributeTests extends AbstractPatchTest
{



  @DisplayName("Multivalued Complex attribute validation tests")
  @Nested
  public class MultivaluedComplexAttributesTests
  {

    @DisplayName("Resource-Patch Tests")
    @Nested
    class ResourcePatchTests
    {

      @DisplayName("Main-Resource Tests")
      @Nested
      class MainResourceTests
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

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
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

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
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
            Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8),
                                         patchedComplex.getBinaryArray().get(0));
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
          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * verifies that an empty complex attribute like this:
         *
         * <pre>
         *  {
         *     "schemas": [
         *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
         *     ],
         *     "Operations": [
         *         {
         *             "name": "addMember",
         *             "op": "add",
         *             "path": "members",
         *             "value": [
         *                 {
         *                     "value": "123-456"
         *                 },
         *                 {}
         *             ]
         *         }
         *     ]
         * }
         * </pre>
         *
         * will simply be ignored. It is handled as if it were not present.
         */
        @DisplayName("success: empty object is ignored if other valid objects are present")
        @Test
        public void testEmptyObjectCausesBadRequest()
        {
          GroupHandlerImpl groupHandler = new GroupHandlerImpl();
          resourceEndpoint.registerEndpoint(new GroupEndpointDefinition(groupHandler));

          Group adminsGroup = Group.builder().id(UUID.randomUUID().toString()).displayName("admins").build();
          groupHandler.getInMemoryMap().put(adminsGroup.getId().get(), adminsGroup);

          ArrayNode members = new ArrayNode(JsonNodeFactory.instance);
          members.add(Member.builder().value("123-456").build());
          members.add(new Member());
          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .path("members")
                                                                                      .op(PatchOp.ADD)
                                                                                      .valueNode(members)
                                                                                      .build());

          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          PatchRequestHandler<Group> patchRequestHandler = new PatchRequestHandler<>(adminsGroup.getId().get(),
                                                                                     groupHandler,
                                                                                     resourceEndpoint.getPatchWorkarounds(),
                                                                                     new Context(null));
          Group patchedGroup = patchRequestHandler.handlePatchRequest(patchOpRequest);
          Assertions.assertEquals(1, patchedGroup.getMembers().size());
          Assertions.assertEquals("123-456", patchedGroup.getMembers().get(0).getValue().get());
        }

        /**
         * verifies that an empty complex attribute like this:
         *
         * <pre>
         *  {
         *     "schemas": [
         *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
         *     ],
         *     "Operations": [
         *         {
         *             "name": "addMember",
         *             "op": "add",
         *             "path": "members",
         *             "value": [
         *                 {}
         *             ]
         *         }
         *     ]
         * }
         * </pre>
         *
         * causes a BadRequestException because there is no object that can be handled
         */
        @DisplayName("failure: empty object on multi-complex is not accepted if single object")
        @Test
        public void testEmptyObjectCausesBadRequest2()
        {
          GroupHandlerImpl groupHandler = new GroupHandlerImpl();
          resourceEndpoint.registerEndpoint(new GroupEndpointDefinition(groupHandler));

          Group adminsGroup = Group.builder()
                                   .id(UUID.randomUUID().toString())
                                   .displayName("admins")
                                   .members(Arrays.asList(Member.builder().value("123-456").build()))
                                   .build();
          groupHandler.getInMemoryMap().put(adminsGroup.getId().get(), adminsGroup);

          ArrayNode members = new ArrayNode(JsonNodeFactory.instance);
          members.add(new Member());
          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .path("members")
                                                                                      .op(PatchOp.ADD)
                                                                                      .valueNode(members)
                                                                                      .build());

          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          PatchRequestHandler<Group> patchRequestHandler = new PatchRequestHandler<>(adminsGroup.getId().get(),
                                                                                     groupHandler,
                                                                                     resourceEndpoint.getPatchWorkarounds(),
                                                                                     new Context(null));
          RequestContextException ex = Assertions.assertThrows(RequestContextException.class,
                                                               () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals("The request document contains errors", ex.getMessage());
          Map<String, List<String>> fieldErrors = ex.getValidationContext().getFieldErrors();
          Assertions.assertEquals(1, fieldErrors.size());
          List<String> memberValueErrors = fieldErrors.get("members.value");
          Assertions.assertNotNull(memberValueErrors);
          Assertions.assertEquals(1, memberValueErrors.size());
          Assertions.assertEquals("Required sub-attribute 'urn:ietf:params:scim:schemas:core:2.0:Group:members.value' "
                                  + "is missing in patch object.",
                                  memberValueErrors.get(0));
        }

        /* ********************************************************************************************* */
      }

      @DisplayName("Extension-Resource Tests")
      @Nested
      class ExtensionResourceTests
      {

        @BeforeEach
        public void extendEnterpriseUserSchema()
        {
          addCustomAttributesToEnterpriseUserSchema();
        }

        /**
         * replace a multiComplex attribute within an extension
         */
        @DisplayName("success: REPLACE multiComplex on extension")
        @Test
        public void testReplaceMultiComplex()
        {
          String attributeName = "multiComplex";
          AllTypes patchResource = new AllTypes(true);

          {
            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setBool(true);
            complex.setNumberArray(Arrays.asList(6L, 7L));
            complex.setBoolArray(Arrays.asList(true, false));

            EnterpriseUser enterpriseUser = new EnterpriseUser();
            enterpriseUser.set(attributeName, new ArrayNode(JsonNodeFactory.instance, Arrays.asList(complex)));
            patchResource.setEnterpriseUser(enterpriseUser);
          }


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
          EnterpriseUser enterpriseUser = patchedResource.getEnterpriseUser().get();
          AllTypes extensionAllTypes = JsonHelper.copyResourceToObject(enterpriseUser, AllTypes.class);
          Assertions.assertEquals(1, extensionAllTypes.getMultiComplex().size());

          AllTypes patchedComplex = extensionAllTypes.getMultiComplex().get(0);
          Assertions.assertEquals("hello world", patchedComplex.getString().get());
          Assertions.assertEquals(5L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(true, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }


        /**
         * replace a multiComplex attribute within an extension with msAzure style reference
         */
        @DisplayName("success: REPLACE multiComplex on extension (msAzure style)")
        @Test
        public void testReplaceMultiComplexMsAzureStyle()
        {
          String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
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
          EnterpriseUser enterpriseUser = patchedResource.getEnterpriseUser().get();
          AllTypes extensionAllTypes = JsonHelper.copyResourceToObject(enterpriseUser, AllTypes.class);
          Assertions.assertEquals(1, extensionAllTypes.getMultiComplex().size());

          AllTypes patchedComplex = extensionAllTypes.getMultiComplex().get(0);
          Assertions.assertEquals("hello world", patchedComplex.getString().get());
          Assertions.assertEquals(5L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(true, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * replaces a multiComplex with an object reference within an extension
         */
        @DisplayName("success: REPLACE multiComplex with object reference on extension")
        @Test
        public void testReplaceMultiComplexWithNonArrayReference()
        {
          String attributeName = "multiComplex";
          AllTypes patchResource = new AllTypes(true);

          {
            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setBool(true);
            complex.setNumberArray(Arrays.asList(6L, 7L));
            complex.setBoolArray(Arrays.asList(true, false));
            EnterpriseUser enterpriseUser = new EnterpriseUser();
            enterpriseUser.set(attributeName, new ArrayNode(JsonNodeFactory.instance, Arrays.asList(complex)));
            patchResource.setEnterpriseUser(enterpriseUser);
          }

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
          EnterpriseUser enterpriseUser = patchedResource.getEnterpriseUser().get();
          AllTypes extensionAllTypes = JsonHelper.copyResourceToObject(enterpriseUser, AllTypes.class);
          Assertions.assertEquals(1, extensionAllTypes.getMultiComplex().size());

          AllTypes patchedComplex = extensionAllTypes.getMultiComplex().get(0);
          Assertions.assertEquals("hello world", patchedComplex.getString().get());
          Assertions.assertEquals(5L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(true, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * replaces a multiComplex with an object reference within an extension with msAzure style reference
         */
        @DisplayName("success: REPLACE multiComplex with object reference on extension (msAzure style)")
        @Test
        public void testReplaceMultiComplexWithNonArrayReferenceMsAzureStyle()
        {
          String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
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
          EnterpriseUser enterpriseUser = patchedResource.getEnterpriseUser().get();
          AllTypes extensionAllTypes = JsonHelper.copyResourceToObject(enterpriseUser, AllTypes.class);
          Assertions.assertEquals(1, extensionAllTypes.getMultiComplex().size());

          AllTypes patchedComplex = extensionAllTypes.getMultiComplex().get(0);
          Assertions.assertEquals("hello world", patchedComplex.getString().get());
          Assertions.assertEquals(5L, patchedComplex.getNumber().get());
          Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
          Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
          Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
          Assertions.assertEquals(true, patchedComplex.getBool().get());
          Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
          Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
          Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));

          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a multiComplex object into an extension
         */
        @DisplayName("success: ADD multiComplex to extension with object reference")
        @Test
        public void testAddMultiComplexWithObjectReference()
        {
          String attributeName = "multiComplex";
          AllTypes patchResource = new AllTypes(true);
          {
            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setBool(true);
            complex.setNumberArray(Arrays.asList(6L, 7L));
            complex.setBoolArray(Arrays.asList(true, false));
            EnterpriseUser enterpriseUser = new EnterpriseUser();
            enterpriseUser.set(attributeName, new ArrayNode(JsonNodeFactory.instance, Arrays.asList(complex)));
            patchResource.set(SchemaUris.ENTERPRISE_USER_URI, enterpriseUser);
          }

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
            EnterpriseUser enterpriseUser = new EnterpriseUser();
            allTypes.set(SchemaUris.ENTERPRISE_USER_URI, enterpriseUser);
            enterpriseUser.set("multiComplex", new ArrayNode(JsonNodeFactory.instance, Arrays.asList(previousComplex)));
          }
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
          AllTypes extensionAllTypes = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
          Assertions.assertEquals(2, extensionAllTypes.getMultiComplex().size());
          {
            AllTypes patchedComplex = extensionAllTypes.getMultiComplex().get(0);
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
            Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8),
                                         patchedComplex.getBinaryArray().get(0));
          }
          {
            AllTypes patchedComplex = extensionAllTypes.getMultiComplex().get(1);
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
          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }

        /**
         * adds a multiComplex object into an extension
         */
        @DisplayName("success: ADD multiComplex to extension with object reference (msAzure style)")
        @Test
        public void testAddMultiComplexWithObjectReferenceMsAzureStyle()
        {
          String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
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
            EnterpriseUser enterpriseUser = new EnterpriseUser();
            allTypes.set(SchemaUris.ENTERPRISE_USER_URI, enterpriseUser);
            enterpriseUser.set("multiComplex", new ArrayNode(JsonNodeFactory.instance, Arrays.asList(previousComplex)));
          }
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          EnterpriseUser enterpriseUser = patchedResource.getEnterpriseUser().get();
          AllTypes extensionAllTypes = JsonHelper.copyResourceToObject(enterpriseUser, AllTypes.class);
          Assertions.assertEquals(2, extensionAllTypes.getMultiComplex().size());
          {
            AllTypes patchedComplex = extensionAllTypes.getMultiComplex().get(0);
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
            Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8),
                                         patchedComplex.getBinaryArray().get(0));
          }
          {
            AllTypes patchedComplex = extensionAllTypes.getMultiComplex().get(1);
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
          // must be called
          {
            Mockito.verify(defaultPatchOperationHandler)
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                   .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(),
                                    Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
          }
        }
      }
    }

    @DisplayName("Path-Patch Tests")
    @Nested
    class PathPatchTests
    {

      @DisplayName("Main Resource Tests")
      @Nested
      class MainResourceTests
      {

        @DisplayName("Without Filter Tests")
        @Nested
        class WithoutFilterTests
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

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
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

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
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
              Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8),
                                           patchedComplex.getBinaryArray().get(0));
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

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * replace a multiComplex with identical value
           */
          @DisplayName("success: Replace multiComplex with identical value")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
          public void testReplaceMultiComplexWithIdenticalValue(String attributeName)
          {
            PatchRequestOperation patchRequestOperation;
            {
              ArrayNode values = new ArrayNode(JsonNodeFactory.instance);
              {
                AllTypes complex = new AllTypes(false);
                complex.setString("hello world");
                complex.setNumber(5L);
                values.add(complex);
              }
              {
                AllTypes complex = new AllTypes(false);
                complex.setStringArray(Arrays.asList("hello", "world"));
                complex.setNumberArray(Arrays.asList(6L, 7L));
                values.add(complex);
              }
              patchRequestOperation = PatchRequestOperation.builder()
                                                           .op(PatchOp.REPLACE)
                                                           .path(attributeName)
                                                           .valueNode(values)
                                                           .build();
            }

            List<PatchRequestOperation> operations = Arrays.asList(patchRequestOperation);
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            allTypes.set("multiComplex", patchRequestOperation.getValueNode().get());
            addAllTypesToProvider(allTypes);

            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertFalse(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());
            {
              AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
              Assertions.assertEquals(2, patchedComplex.size());
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
            }
            {
              AllTypes patchedComplex = patchedResource.getMultiComplex().get(1);
              Assertions.assertEquals(2, patchedComplex.size());
              Assertions.assertEquals(2, patchedComplex.getStringArray().size());
              Assertions.assertEquals("hello", patchedComplex.getStringArray().get(0));
              Assertions.assertEquals("world", patchedComplex.getStringArray().get(1));
              Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
            }
            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }
          /* *************************************************************************************** */
        }

        @DisplayName("With Filter Tests")
        @Nested
        class WithFilterTests
        {

          /**
           * replace a multivalued complex with a single matching element on the filter
           */
          @DisplayName("success: REPLACE multivalued complex single matching element")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
          public void testReplaceMultivaluedComplexWithSingleMatchingElement(String attributeName)
          {
            String filterPath = String.format("%s[numberArray co 5]", attributeName);

            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setBool(true);
            complex.setBoolArray(Arrays.asList(true, false));

            ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
            multiComplex.add(complex);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(filterPath)
                                                                                        .valueNode(multiComplex)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L, 3L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(4L, 5L, 6L));

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());
            {
              AllTypes oldComplex = patchedResource.getMultiComplex().get(0);
              Assertions.assertEquals(1, oldComplex.size());
              Assertions.assertEquals(3, oldComplex.getNumberArray().size());
              Assertions.assertEquals(1L, oldComplex.getNumberArray().get(0));
              Assertions.assertEquals(2L, oldComplex.getNumberArray().get(1));
              Assertions.assertEquals(3L, oldComplex.getNumberArray().get(2));
            }
            {
              AllTypes patchedComplex = patchedResource.getMultiComplex().get(1);
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
              Assertions.assertEquals(3, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(4L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(5L, patchedComplex.getNumberArray().get(1));
              Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(2));
              Assertions.assertEquals(true, patchedComplex.getBool().get());
              Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
            }

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * replace an empty field with a multivalued complex attribute by direct path reference
           */
          @DisplayName("success: REPLACE multivalued complex with two matching elements on the filter")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
          public void testReplaceMultivaluedComplexNoPrevious(String attributeName)
          {
            String filterPath = String.format("%s[numberArray co 5 or numberArray eq 1]", attributeName);

            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setBool(true);
            complex.setBoolArray(Arrays.asList(true, false));

            ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
            multiComplex.add(complex);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(filterPath)
                                                                                        .valueNode(multiComplex)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L, 3L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(4L, 5L, 6L));

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
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
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
              Assertions.assertEquals(3, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(1L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(2L, patchedComplex.getNumberArray().get(1));
              Assertions.assertEquals(3L, patchedComplex.getNumberArray().get(2));
              Assertions.assertEquals(true, patchedComplex.getBool().get());
              Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
            }
            {
              AllTypes patchedComplex = patchedResource.getMultiComplex().get(1);
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
              Assertions.assertEquals(3, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(4L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(5L, patchedComplex.getNumberArray().get(1));
              Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(2));
              Assertions.assertEquals(true, patchedComplex.getBool().get());
              Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
            }

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * add a multivalued complex with a single matching element on the filter
           */
          @DisplayName("success: ADD multivalued complex single matching element")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
          public void testAddMultivaluedComplexWithSingleMatchingElement(String attributeName)
          {
            String filterPath = String.format("%s[numberArray eq 2]", attributeName);

            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setNumberArray(Arrays.asList(2L, 3L));
            complex.setBool(true);
            complex.setBoolArray(Arrays.asList(true, false));

            ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
            multiComplex.add(complex);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.ADD)
                                                                                        .path(filterPath)
                                                                                        .valueNode(multiComplex)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(3L, 4L));

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));

            log.warn(patchedResource.toPrettyString());

            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());
            {
              AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
              Assertions.assertEquals(5, patchedComplex.size());
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
              Assertions.assertEquals(4, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(1L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(2L, patchedComplex.getNumberArray().get(1));
              Assertions.assertEquals(2L, patchedComplex.getNumberArray().get(2));
              Assertions.assertEquals(3L, patchedComplex.getNumberArray().get(3));
              Assertions.assertEquals(true, patchedComplex.getBool().get());
              Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
            }
            {
              AllTypes patchedComplex = patchedResource.getMultiComplex().get(1);
              Assertions.assertEquals(1, patchedComplex.size());
              Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(3L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(4L, patchedComplex.getNumberArray().get(1));
            }

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * add a multivalued complex with a no matching elements
           */
          @DisplayName("failure: ADD multivalued complex with non-matching filter")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
          public void testAddMultivaluedComplexWithNotMatchingFilter(String attributeName)
          {
            String filterPath = String.format("%s[string eq \"world, hello\"]", attributeName);

            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setNumberArray(Arrays.asList(2L, 3L));
            complex.setBool(true);
            complex.setBoolArray(Arrays.asList(true, false));

            ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
            multiComplex.add(complex);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.ADD)
                                                                                        .path(filterPath)
                                                                                        .valueNode(multiComplex)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(3L, 4L));

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertEquals(String.format("Cannot 'ADD' value on path '%s' for no matching object was found",
                                                  filterPath),
                                    ex.getMessage());
            Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
              Assertions.assertEquals(1, patchedComplex.getBinaryArray().size());
              Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8),
                                           patchedComplex.getBinaryArray().get(0));
            }

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
            complex.set("decimalArray", new DoubleNode(5.5));

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
              previousComplex.setDecimalArray(Arrays.asList(1.5, 2.9));
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
              Assertions.assertEquals(7, patchedComplex.size());
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
              Assertions.assertEquals(4, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(66L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(77L, patchedComplex.getNumberArray().get(1));
              Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(2));
              Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(3));
              Assertions.assertEquals(3, patchedComplex.getDecimalArray().size());
              Assertions.assertEquals(1.5, patchedComplex.getDecimalArray().get(0));
              Assertions.assertEquals(2.9, patchedComplex.getDecimalArray().get(1));
              Assertions.assertEquals(5.5, patchedComplex.getDecimalArray().get(2));
              Assertions.assertEquals(true, patchedComplex.getBool().get());
              Assertions.assertEquals(4, patchedComplex.getBoolArray().size());
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(0));
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(1));
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(2));
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(3));
              Assertions.assertEquals(1, patchedComplex.getBinaryArray().size());
              Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8),
                                           patchedComplex.getBinaryArray().get(0));
            }

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * add operation does not change multiComplex with identical value
           */
          @DisplayName("success: Add multiComplex with identical value")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex"})
          public void testAddMultiComplexWithIdenticalValue(String attributeName)
          {
            PatchRequestOperation patchRequestOperation;
            {
              ArrayNode values = new ArrayNode(JsonNodeFactory.instance);
              {
                AllTypes complex = new AllTypes(false);
                complex.setString("hello world");
                complex.setNumber(5L);
                values.add(complex);
              }

              final String filterPath = String.format("%s[string eq \"hello world\"]", attributeName);
              patchRequestOperation = PatchRequestOperation.builder()
                                                           .op(PatchOp.ADD)
                                                           .path(filterPath)
                                                           .valueNode(values)
                                                           .build();
            }

            List<PatchRequestOperation> operations = Arrays.asList(patchRequestOperation);
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            allTypes.set("multiComplex", patchRequestOperation.getValueNode().get());
            addAllTypesToProvider(allTypes);

            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertFalse(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(1, patchedResource.getMultiComplex().size());
            {
              AllTypes patchedComplex = patchedResource.getMultiComplex().get(0);
              Assertions.assertEquals(2, patchedComplex.size());
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
            }
            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }
          /* **************************************************************************************** */
        }
        /* **************************************************************************************** */
      }

      @DisplayName("Extension Resource Tests")
      @Nested
      class ExtensionResourceTests
      {

        @BeforeEach
        public void extendEnterpriseUserSchema()
        {
          addCustomAttributesToEnterpriseUserSchema();
        }

        @DisplayName("Without Filter Tests")
        @Nested
        class WithoutFilterTests
        {

          /**
           * replace a multivalued complex attribute
           */
          @DisplayName("success: replace multivalued complex")
          @Test
          public void testReplaceMultivaluedComplex()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
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
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
            Assertions.assertEquals(1, patchedExtension.getMultiComplex().size());

            AllTypes patchedComplex = patchedExtension.getMultiComplex().get(0);
            Assertions.assertEquals("hello world", patchedComplex.getString().get());
            Assertions.assertEquals(5L, patchedComplex.getNumber().get());
            Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
            Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(0));
            Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(1));
            Assertions.assertEquals(true, patchedComplex.getBool().get());
            Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
            Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
            Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * replace an empty field with two multivalued complex attributes by direct path reference
           */
          @DisplayName("success: replace two multivalued complex (no previous)")
          @Test
          public void testAddTwoMultivaluedComplexNoPrevious()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
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
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
            Assertions.assertEquals(2, patchedExtension.getMultiComplex().size());
            for ( int i = 0 ; i < patchedExtension.getMultiComplex().size() ; i++ )
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(i);
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

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * replace one existing multiComplex with two multivalued complex attributes by direct path reference
           */
          @DisplayName("success: replace two multivalued complex (with previous elements)")
          @Test
          public void testReplaceTwoMultivaluedComplex()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
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
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
            Assertions.assertEquals(2, patchedExtension.getMultiComplex().size());
            for ( int i = 0 ; i < patchedExtension.getMultiComplex().size() ; i++ )
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(i);
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

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * add one multiComplex attribute by direct path reference
           */
          @DisplayName("success: add one multivalued complex (no previous elements)")
          @Test
          public void testAddOneMultivaluedComplex()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
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
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
            Assertions.assertEquals(1, patchedExtension.getMultiComplex().size());
            AllTypes patchedComplex = patchedExtension.getMultiComplex().get(0);
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

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * add two multiComplex attributes by direct path reference with previous existing elements
           */
          @DisplayName("success: add one multivalued complex (with previous elements)")
          @Test
          public void testAddOneMultivaluedComplexWithPrevious()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
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
              EnterpriseUser enterpriseUser = new EnterpriseUser();
              enterpriseUser.set("multiComplex",
                                 new ArrayNode(JsonNodeFactory.instance, Arrays.asList(previousComplex)));
              allTypes.setEnterpriseUser(enterpriseUser);
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
            Assertions.assertEquals(2, patchedExtension.getMultiComplex().size());
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(0);
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
              Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8),
                                           patchedComplex.getBinaryArray().get(0));
            }
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(1);
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

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

        }

        @DisplayName("With Filter Tests")
        @Nested
        class WithFilterTests
        {

          /**
           * replace a multivalued complex with a single matching element on the filter
           */
          @DisplayName("success: REPLACE multivalued complex single matching element")
          @Test
          public void testReplaceMultivaluedComplexWithSingleMatchingElement()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
            String filterPath = String.format("%s[numberArray co 5]", attributeName);

            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setBool(true);
            complex.setBoolArray(Arrays.asList(true, false));

            ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
            multiComplex.add(complex);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(filterPath)
                                                                                        .valueNode(multiComplex)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L, 3L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(4L, 5L, 6L));

              EnterpriseUser enterpriseUser = new EnterpriseUser();
              enterpriseUser.set("multiComplex",
                                 new ArrayNode(JsonNodeFactory.instance, Arrays.asList(multiComplex1, multiComplex2)));
              allTypes.setEnterpriseUser(enterpriseUser);
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
            Assertions.assertEquals(2, patchedExtension.getMultiComplex().size());
            {
              AllTypes oldComplex = patchedExtension.getMultiComplex().get(0);
              Assertions.assertEquals(1, oldComplex.size());
              Assertions.assertEquals(3, oldComplex.getNumberArray().size());
              Assertions.assertEquals(1L, oldComplex.getNumberArray().get(0));
              Assertions.assertEquals(2L, oldComplex.getNumberArray().get(1));
              Assertions.assertEquals(3L, oldComplex.getNumberArray().get(2));
            }
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(1);
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
              Assertions.assertEquals(3, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(4L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(5L, patchedComplex.getNumberArray().get(1));
              Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(2));
              Assertions.assertEquals(true, patchedComplex.getBool().get());
              Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
            }

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * replace an empty field with a multivalued complex attribute by direct path reference
           */
          @DisplayName("success: REPLACE multivalued complex with two matching elements on the filter")
          @Test
          public void testReplaceMultivaluedComplexNoPrevious()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
            String filterPath = String.format("%s[numberArray co 5 or numberArray eq 1]", attributeName);

            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setBool(true);
            complex.setBoolArray(Arrays.asList(true, false));

            ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
            multiComplex.add(complex);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(filterPath)
                                                                                        .valueNode(multiComplex)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L, 3L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(4L, 5L, 6L));

              EnterpriseUser enterpriseUser = new EnterpriseUser();
              enterpriseUser.set("multiComplex",
                                 new ArrayNode(JsonNodeFactory.instance, Arrays.asList(multiComplex1, multiComplex2)));
              allTypes.setEnterpriseUser(enterpriseUser);
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
            Assertions.assertEquals(2, patchedExtension.getMultiComplex().size());
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(0);
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
              Assertions.assertEquals(3, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(1L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(2L, patchedComplex.getNumberArray().get(1));
              Assertions.assertEquals(3L, patchedComplex.getNumberArray().get(2));
              Assertions.assertEquals(true, patchedComplex.getBool().get());
              Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
            }
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(1);
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
              Assertions.assertEquals(3, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(4L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(5L, patchedComplex.getNumberArray().get(1));
              Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(2));
              Assertions.assertEquals(true, patchedComplex.getBool().get());
              Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
            }

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * add a multivalued complex with a single matching element on the filter
           */
          @DisplayName("success: ADD multivalued complex single matching element")
          @Test
          public void testAddMultivaluedComplexWithSingleMatchingElement()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
            String filterPath = String.format("%s[numberArray eq 2]", attributeName);

            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setNumberArray(Arrays.asList(2L, 3L));
            complex.setBool(true);
            complex.setBoolArray(Arrays.asList(true, false));

            ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
            multiComplex.add(complex);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.ADD)
                                                                                        .path(filterPath)
                                                                                        .valueNode(multiComplex)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(3L, 4L));

              EnterpriseUser enterpriseUser = new EnterpriseUser();
              enterpriseUser.set("multiComplex",
                                 new ArrayNode(JsonNodeFactory.instance, Arrays.asList(multiComplex1, multiComplex2)));
              allTypes.setEnterpriseUser(enterpriseUser);
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
            Assertions.assertEquals(2, patchedExtension.getMultiComplex().size());
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(0);
              Assertions.assertEquals(5, patchedComplex.size());
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
              Assertions.assertEquals(4, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(1L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(2L, patchedComplex.getNumberArray().get(1));
              Assertions.assertEquals(2L, patchedComplex.getNumberArray().get(2));
              Assertions.assertEquals(3L, patchedComplex.getNumberArray().get(3));
              Assertions.assertEquals(true, patchedComplex.getBool().get());
              Assertions.assertEquals(2, patchedComplex.getBoolArray().size());
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(0));
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(1));
            }
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(1);
              Assertions.assertEquals(1, patchedComplex.size());
              Assertions.assertEquals(2, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(3L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(4L, patchedComplex.getNumberArray().get(1));
            }

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * add a multivalued complex with a no matching elements
           */
          @DisplayName("failure: ADD multivalued complex with non-matching filter")
          @Test
          public void testAddMultivaluedComplexWithNotMatchingFilter()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
            String filterPath = String.format("%s[string eq \"world, hello\"]", attributeName);

            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setNumberArray(Arrays.asList(2L, 3L));
            complex.setBool(true);
            complex.setBoolArray(Arrays.asList(true, false));

            ArrayNode multiComplex = new ArrayNode(JsonNodeFactory.instance);
            multiComplex.add(complex);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.ADD)
                                                                                        .path(filterPath)
                                                                                        .valueNode(multiComplex)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(3L, 4L));

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                             () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertEquals(String.format("Cannot 'ADD' value on path '%s' for no matching object was found",
                                                  filterPath),
                                    ex.getMessage());
            Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * replace multivalued complex with filter
           */
          @DisplayName("success: replace one multivalued complex (with filter expression)")
          @Test
          public void testReplaceOneMultivaluedComplexWithFilter()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
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

              EnterpriseUser enterpriseUser = new EnterpriseUser();
              enterpriseUser.set("multiComplex",
                                 new ArrayNode(JsonNodeFactory.instance, Arrays.asList(previousComplex)));
              allTypes.setEnterpriseUser(enterpriseUser);
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
            Assertions.assertEquals(1, patchedExtension.getMultiComplex().size());
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(0);
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
              Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8),
                                           patchedComplex.getBinaryArray().get(0));
            }

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * replace multivalued complex with filter
           */
          @DisplayName("success: add one multivalued complex (with filter expression)")
          @Test
          public void testAddOneMultivaluedComplexWithFilter()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
            final String filterExpression = String.format("%s[string eq \"world, hello\"]", attributeName);

            AllTypes complex = new AllTypes(false);
            complex.setString("hello world");
            complex.setNumber(5L);
            complex.setBool(true);
            complex.setNumberArray(Arrays.asList(6L, 7L));
            complex.setBoolArray(Arrays.asList(true, false));
            complex.set("decimalArray", new DoubleNode(5.5));

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
              previousComplex.setDecimalArray(Arrays.asList(1.5, 2.9));
              previousComplex.setBoolArray(Arrays.asList(false, true));
              previousComplex.setBinaryArray(Arrays.asList("Mtox".getBytes(StandardCharsets.UTF_8)));
              EnterpriseUser enterpriseUser = new EnterpriseUser();
              enterpriseUser.set("multiComplex",
                                 new ArrayNode(JsonNodeFactory.instance, Arrays.asList(previousComplex)));
              allTypes.setEnterpriseUser(enterpriseUser);
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);
            Assertions.assertEquals(1, patchedExtension.getMultiComplex().size());
            {
              AllTypes patchedComplex = patchedExtension.getMultiComplex().get(0);
              Assertions.assertEquals(7, patchedComplex.size());
              Assertions.assertEquals("hello world", patchedComplex.getString().get());
              Assertions.assertEquals(5L, patchedComplex.getNumber().get());
              Assertions.assertEquals(4, patchedComplex.getNumberArray().size());
              Assertions.assertEquals(66L, patchedComplex.getNumberArray().get(0));
              Assertions.assertEquals(77L, patchedComplex.getNumberArray().get(1));
              Assertions.assertEquals(6L, patchedComplex.getNumberArray().get(2));
              Assertions.assertEquals(7L, patchedComplex.getNumberArray().get(3));
              Assertions.assertEquals(3, patchedComplex.getDecimalArray().size());
              Assertions.assertEquals(1.5, patchedComplex.getDecimalArray().get(0));
              Assertions.assertEquals(2.9, patchedComplex.getDecimalArray().get(1));
              Assertions.assertEquals(5.5, patchedComplex.getDecimalArray().get(2));
              Assertions.assertEquals(true, patchedComplex.getBool().get());
              Assertions.assertEquals(4, patchedComplex.getBoolArray().size());
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(0));
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(1));
              Assertions.assertEquals(true, patchedComplex.getBoolArray().get(2));
              Assertions.assertEquals(false, patchedComplex.getBoolArray().get(3));
              Assertions.assertEquals(1, patchedComplex.getBinaryArray().size());
              Assertions.assertArrayEquals("Mtox".getBytes(StandardCharsets.UTF_8),
                                           patchedComplex.getBinaryArray().get(0));
            }

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
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
        {
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(SimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedSimpleAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
          Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                 .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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

        @DisplayName("No Filter Tests")
        @Nested
        class NoFilterTests
        {

          /**
           * replace a simple attribute of a multivalued complex attribute without a filter
           */
          @DisplayName("success: REPLACE multiComplex.number")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex.number",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.number"})
          public void testReplaceSimplevalueToMultiComplexNoPrevious(String attributeName)
          {
            JsonNode numberValue = new IntNode(5);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(attributeName)
                                                                                        .valueNode(numberValue)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumber(1L);

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumber(2L);

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedResource.getMultiComplex().get(0);
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex1.getNumber().get());

            AllTypes patchedMultiComplex2 = patchedResource.getMultiComplex().get(1);
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex2.getNumber().get());

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * replace multiComplex.numberArray with a number-value that is not wrapped within an array
           */
          @DisplayName("success: REPLACE multiComplex.numberArray with non-array-value")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex.numberArray",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.numberArray"})
          public void testReplaceMultiComplexNumberArrayWithNonArrayValue(String attributeName)
          {
            JsonNode numberValue = new IntNode(5);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(attributeName)
                                                                                        .valueNode(numberValue)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumber(1L);

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumber(2L);

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedResource.getMultiComplex().get(0);
            Assertions.assertEquals(1, patchedMultiComplex1.getNumberArray().size());
            Assertions.assertEquals(1L, patchedMultiComplex1.getNumber().get());
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex1.getNumberArray().get(0));

            AllTypes patchedMultiComplex2 = patchedResource.getMultiComplex().get(1);
            Assertions.assertEquals(2L, patchedMultiComplex2.getNumber().get());
            Assertions.assertEquals(1, patchedMultiComplex2.getNumberArray().size());
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex2.getNumberArray().get(0));

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            }
          }

          /**
           * replace multiComplex.number with an empty value
           */
          @DisplayName("success: REPLACE multiComplex.number with empty value")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex.number",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.number"})
          public void testReplaceMultiComplexNumberWithEmptyValue(String attributeName)
          {

            SchemaAttribute multiComplexNumberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
            multiComplexNumberAttribute.setRequired(true);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(attributeName)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumber(1L);
              multiComplex1.setString("hello");

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumber(2L);
              multiComplex2.setString("world");

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedResource.getMultiComplex().get(0);
            Assertions.assertNull(patchedMultiComplex1.getNumber().orElse(null));

            AllTypes patchedMultiComplex2 = patchedResource.getMultiComplex().get(1);
            Assertions.assertNull(patchedMultiComplex2.getNumber().orElse(null));

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            }
          }

          /**
           * replace multiComplex.number with an empty value with the resulting objects being empty
           */
          @DisplayName("success: REPLACE multiComplex.number with empty value (objects cleared because empty)")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex.number",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.number"})
          public void testReplaceMultiComplexNumberWithEmptyValueAndClearTheObjects(String attributeName)
          {

            SchemaAttribute multiComplexNumberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();
            multiComplexNumberAttribute.setRequired(true);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(attributeName)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumber(1L);

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumber(2L);

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(0, patchedResource.getMultiComplex().size());

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            }
          }

          /**
           * Replace a number with string on multivalued complex
           */
          @DisplayName("failure: replace multiComplex.number with string value")
          @Test
          public void testReplaceMultiComplexNumberWithString()
          {
            final String attributeName = "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.number";
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
              List<String> fieldErrors = errorResponse.getFieldErrors()
                                                      .get(multiComplexNumberAttribute.getScimNodeName());
              Assertions.assertEquals(1, fieldErrors.size());
              Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                                    + "with value '\"illegal-value\"'",
                                                    multiComplexNumberAttribute.getFullResourceName()),
                                      fieldErrors.get(0));
            }
            {
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
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }
          /* ************************************************************************************** */
        }

        @DisplayName("With Filter Tests")
        @Nested
        class WithFilterTests
        {

          /**
           * replace a simple attribute of a multivalued complex attribute with a filter
           */
          @DisplayName("success: REPLACE multiComplex.number")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex[number eq 1].number",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex[number eq 1].number",
                                  // the following two are not real SCIM, but they work the same way (accidental
                                  // discovery)
                                  "multiComplex.number[number eq 1]",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.number[number eq 1]"})
          public void testReplaceSimpleValueToMultiComplexWithFilter(String filterPath)
          {
            JsonNode numberValue = new IntNode(5);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(filterPath)
                                                                                        .valueNode(numberValue)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumber(1L);

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumber(2L);

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedResource.getMultiComplex().get(0);
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex1.getNumber().get());

            AllTypes patchedMultiComplex2 = patchedResource.getMultiComplex().get(1);
            Assertions.assertEquals(2L, patchedMultiComplex2.getNumber().get());

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * add a multivalued attribute of a multivalued complex attribute with a filter
           */
          @DisplayName("success: ADD multiComplex.numberArray")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex[number eq 1].numberArray",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex[number eq 1].numberArray",
                                  // the following two are not real SCIM, but they work the same way (accidental
                                  // discovery)
                                  "multiComplex.numberArray[number eq 1]",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.numberArray[number eq 1]"})
          public void testAddSimpleValueToMultiComplexWithFilter(String filterPath)
          {
            JsonNode numberValue = new IntNode(5);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.ADD)
                                                                                        .path(filterPath)
                                                                                        .valueNode(numberValue)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumber(1L);
              multiComplex1.setNumberArray(Arrays.asList(1L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumber(2L);
              multiComplex2.setNumberArray(Arrays.asList(2L));

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedResource.getMultiComplex().get(0);
            Assertions.assertEquals(2, patchedMultiComplex1.size());
            Assertions.assertEquals(1L, patchedMultiComplex1.getNumber().get());
            Assertions.assertEquals(2, patchedMultiComplex1.getNumberArray().size());
            Assertions.assertEquals(1L, patchedMultiComplex1.getNumberArray().get(0));
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex1.getNumberArray().get(1));

            AllTypes patchedMultiComplex2 = patchedResource.getMultiComplex().get(1);
            Assertions.assertEquals(2, patchedMultiComplex2.size());
            Assertions.assertEquals(2L, patchedMultiComplex2.getNumber().get());
            Assertions.assertEquals(1, patchedMultiComplex2.getNumberArray().size());
            Assertions.assertEquals(2L, patchedMultiComplex2.getNumberArray().get(0));

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            }
          }
          /* ****************************************************************************** */
        }
        /* ****************************************************************************** */
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

        @DisplayName("No Filter Tests")
        @Nested
        class NoFilterTests
        {

          /**
           * replace a simple attribute of a multivalued complex attribute without a filter
           */
          @DisplayName("success: REPLACE multiComplex.number")
          @Test
          public void testReplaceSimplevalueToMultiComplexNoPrevious()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex.number";
            JsonNode numberValue = new IntNode(5);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(attributeName)
                                                                                        .valueNode(numberValue)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumber(1L);

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumber(2L);

              EnterpriseUser enterpriseUser = new EnterpriseUser();
              enterpriseUser.set("multiComplex",
                                 new ArrayNode(JsonNodeFactory.instance, Arrays.asList(multiComplex1, multiComplex2)));
              allTypes.setEnterpriseUser(enterpriseUser);
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);

            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedExtension.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedExtension.getMultiComplex().get(0);
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex1.getNumber().get());

            AllTypes patchedMultiComplex2 = patchedExtension.getMultiComplex().get(1);
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex2.getNumber().get());

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }

          /**
           * Replace a number with string on multivalued complex
           */
          @DisplayName("failure: replace multiComplex.number with string value")
          @Test
          public void testReplaceMultiComplexNumberWithString()
          {
            final String attributeName = "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.number";
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
              List<String> fieldErrors = errorResponse.getFieldErrors()
                                                      .get(multiComplexNumberAttribute.getScimNodeName());
              Assertions.assertEquals(1, fieldErrors.size());
              Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                                    + "with value '\"illegal-value\"'",
                                                    multiComplexNumberAttribute.getFullResourceName()),
                                      fieldErrors.get(0));
            }
            {
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
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }
          /* ********************************************************************* */
        }

        @DisplayName("With Filter Tests")
        @Nested
        class WithFilterTests
        {

          /**
           * replace a simple attribute of a multivalued complex attribute without a filter
           */
          @DisplayName("success: REPLACE multiComplex.number")
          @Test
          public void testReplaceSimplevalueToMultiComplexNoPrevious()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
            String filterPath = String.format("%s[number eq 2].number", attributeName);
            JsonNode numberValue = new IntNode(5);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(filterPath)
                                                                                        .valueNode(numberValue)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumber(1L);

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumber(2L);

              EnterpriseUser enterpriseUser = new EnterpriseUser();
              enterpriseUser.set("multiComplex",
                                 new ArrayNode(JsonNodeFactory.instance, Arrays.asList(multiComplex1, multiComplex2)));
              allTypes.setEnterpriseUser(enterpriseUser);
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);

            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedExtension.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedExtension.getMultiComplex().get(0);
            Assertions.assertEquals(1L, patchedMultiComplex1.getNumber().get());

            AllTypes patchedMultiComplex2 = patchedExtension.getMultiComplex().get(1);
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex2.getNumber().get());

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }
          /* ********************************************************************* */
        }
        /* ********************************************************************* */
      }
      /* ********************************************************************* */
    }
    /* ********************************************************************* */
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
      @DisplayName("failure: replace multiComplex.numberArray with string value")
      @Test
      public void testReplaceMultiComplexNumberArrayWithString()
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


      @DisplayName("Main Resource Tests")
      @Nested
      class MainResourceTests
      {

        @DisplayName("No Filter Tests")
        @Nested
        class WithoutFilterTests
        {

          /**
           * Replace a number with string on multivalued complex
           */
          @DisplayName("failure: replace multiComplex.numberArray with string value")
          @Test
          public void testReplaceMultiComplexNumberArrayWithString()
          {
            final String attributeName = "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.numberArray";
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
              List<String> fieldErrors = errorResponse.getFieldErrors()
                                                      .get(multiComplexNumberAttribute.getScimNodeName());
              Assertions.assertEquals(2, fieldErrors.size());
              Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"illegal-value\"]'",
                                      fieldErrors.get(0));
              Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                                    + "with value '\"illegal-value\"'",
                                                    multiComplexNumberAttribute.getFullResourceName()),
                                      fieldErrors.get(1));
            }
          }

          /**
           * Replace a number with string on multivalued complex
           */
          @DisplayName("failure: replace multiComplex.numberArray with array value")
          @Test
          public void testReplaceMultiComplexNumberArrayWithStringInArray()
          {
            final String attributeName = "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.numberArray";
            SchemaAttribute multiComplexNumberAttribute = allTypesResourceType.getSchemaAttribute(attributeName).get();

            ArrayNode numberArrayNode = new ArrayNode(JsonNodeFactory.instance,
                                                      Arrays.asList(new TextNode("illegal-value")));

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(attributeName)
                                                                                        .valueNode(numberArrayNode)
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
              List<String> fieldErrors = errorResponse.getFieldErrors()
                                                      .get(multiComplexNumberAttribute.getScimNodeName());
              Assertions.assertEquals(2, fieldErrors.size());
              Assertions.assertEquals("Found unsupported value in multivalued attribute '[\"illegal-value\"]'",
                                      fieldErrors.get(0));
              Assertions.assertEquals(String.format("Value of attribute '%s' is not of type 'integer' but of type 'string' "
                                                    + "with value '\"illegal-value\"'",
                                                    multiComplexNumberAttribute.getFullResourceName()),
                                      fieldErrors.get(1));
            }
            {
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
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
            }
          }
          /* ***************************************************************************************** */
        }

        @DisplayName("With Filter Tests")
        @Nested
        class WithFilterTests
        {

          /**
           * replace a multivalued attribute of a multivalued complex attribute with filter
           */
          @DisplayName("success: REPLACE multiComplex.numberArray")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex.numberArray",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.numberArray"})
          public void testReplaceMultivaluedValueToMultiComplexWithFilter(String attributeName)
          {
            final String filterPath = String.format("%s[numberArray co 1]", attributeName);
            ArrayNode numberArrayNode = new ArrayNode(JsonNodeFactory.instance,
                                                      Arrays.asList(new IntNode(7), new IntNode(8)));

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(filterPath)
                                                                                        .valueNode(numberArrayNode)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L, 3L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(4L, 5L, 6L));

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedResource.getMultiComplex().get(0);
            List<Long> patchedNumberArray1 = patchedMultiComplex1.getNumberArray();
            Assertions.assertEquals(2, patchedNumberArray1.size());
            Assertions.assertEquals(7L, patchedNumberArray1.get(0));
            Assertions.assertEquals(8L, patchedNumberArray1.get(1));

            AllTypes patchedMultiComplex2 = patchedResource.getMultiComplex().get(1);
            List<Long> patchedNumberArray2 = patchedMultiComplex2.getNumberArray();
            Assertions.assertEquals(3, patchedNumberArray2.size());
            Assertions.assertEquals(4L, patchedNumberArray2.get(0));
            Assertions.assertEquals(5L, patchedNumberArray2.get(1));
            Assertions.assertEquals(6L, patchedNumberArray2.get(2));

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            }
          }

          /**
           * replace a multivalued attribute of a multivalued complex attribute with filter
           */
          @DisplayName("success: ADD multiComplex.numberArray")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex.numberArray",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.numberArray"})
          public void testAddMultivaluedValueToMultiComplexWithFilter(String attributeName)
          {
            final String filterPath = String.format("%s[numberArray co 1]", attributeName);
            ArrayNode numberArrayNode = new ArrayNode(JsonNodeFactory.instance,
                                                      Arrays.asList(new IntNode(7), new IntNode(8)));

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.ADD)
                                                                                        .path(filterPath)
                                                                                        .valueNode(numberArrayNode)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L, 3L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(4L, 5L, 6L));

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedResource.getMultiComplex().get(0);
            List<Long> patchedNumberArray1 = patchedMultiComplex1.getNumberArray();
            Assertions.assertEquals(5, patchedNumberArray1.size());
            Assertions.assertEquals(1L, patchedNumberArray1.get(0));
            Assertions.assertEquals(2L, patchedNumberArray1.get(1));
            Assertions.assertEquals(3L, patchedNumberArray1.get(2));
            Assertions.assertEquals(7L, patchedNumberArray1.get(3));
            Assertions.assertEquals(8L, patchedNumberArray1.get(4));

            AllTypes patchedMultiComplex2 = patchedResource.getMultiComplex().get(1);
            List<Long> patchedNumberArray2 = patchedMultiComplex2.getNumberArray();
            Assertions.assertEquals(3, patchedNumberArray2.size());
            Assertions.assertEquals(4L, patchedNumberArray2.get(0));
            Assertions.assertEquals(5L, patchedNumberArray2.get(1));
            Assertions.assertEquals(6L, patchedNumberArray2.get(2));

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            }
          }

          /**
           * replace an array of a multivalued complex attribute without a filter
           */
          @DisplayName("success: REPLACE multiComplex.numberArray")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex.numberArray",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.numberArray"})
          public void testReplaceMultivaluedComplexNoPrevious(String attributeName)
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
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(3L, 4L));

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedResource.getMultiComplex().get(0);
            Assertions.assertEquals(2, patchedMultiComplex1.getNumberArray().size());
            Assertions.assertEquals(5, patchedMultiComplex1.getNumberArray().get(0));
            Assertions.assertEquals(6, patchedMultiComplex1.getNumberArray().get(1));

            AllTypes patchedMultiComplex2 = patchedResource.getMultiComplex().get(1);
            Assertions.assertEquals(2, patchedMultiComplex2.getNumberArray().size());
            Assertions.assertEquals(5, patchedMultiComplex2.getNumberArray().get(0));
            Assertions.assertEquals(6, patchedMultiComplex2.getNumberArray().get(1));

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            }
          }

          /**
           * replace an array of a multivalued complex attribute without a filter
           */
          @DisplayName("success: ADD multiComplex.numberArray")
          @ParameterizedTest
          @ValueSource(strings = {"multiComplex.numberArray",
                                  "urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex.numberArray"})
          public void testAddMultivaluedComplexNoPrevious(String attributeName)
          {
            ArrayNode numberArray = new ArrayNode(JsonNodeFactory.instance);
            numberArray.add(5);
            numberArray.add(6);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.ADD)
                                                                                        .path(attributeName)
                                                                                        .valueNode(numberArray)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumberArray(Arrays.asList(1L, 2L));

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumberArray(Arrays.asList(3L, 4L));

              allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedResource.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedResource.getMultiComplex().get(0);
            Assertions.assertEquals(4, patchedMultiComplex1.getNumberArray().size());
            Assertions.assertEquals(1, patchedMultiComplex1.getNumberArray().get(0));
            Assertions.assertEquals(2, patchedMultiComplex1.getNumberArray().get(1));
            Assertions.assertEquals(5, patchedMultiComplex1.getNumberArray().get(2));
            Assertions.assertEquals(6, patchedMultiComplex1.getNumberArray().get(3));

            AllTypes patchedMultiComplex2 = patchedResource.getMultiComplex().get(1);
            Assertions.assertEquals(4, patchedMultiComplex2.getNumberArray().size());
            Assertions.assertEquals(3, patchedMultiComplex2.getNumberArray().get(0));
            Assertions.assertEquals(4, patchedMultiComplex2.getNumberArray().get(1));
            Assertions.assertEquals(5, patchedMultiComplex2.getNumberArray().get(2));
            Assertions.assertEquals(6, patchedMultiComplex2.getNumberArray().get(3));

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            }
          }
          /* ************************************************************************************** */
        }
        /* ************************************************************************************** */
      }

      @DisplayName("Extension Tests")
      @Nested
      class ExtensionResourceTests
      {

        @BeforeEach
        public void extendEnterpriseUserSchema()
        {
          addCustomAttributesToEnterpriseUserSchema();
        }

        @DisplayName("No Filter Tests")
        @Nested
        class NoFilterTests
        {

          /**
           * replace multiComplex.numberArray with a number-value that is not wrapped within an array
           */
          @DisplayName("success: REPLACE multiComplex.numberArray with non-array-value")
          @Test
          public void testReplaceMultiComplexNumberArrayWithNonArrayValue()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex.numberArray";
            JsonNode numberValue = new IntNode(5);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(attributeName)
                                                                                        .valueNode(numberValue)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumber(1L);

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumber(2L);

              EnterpriseUser enterpriseUser = new EnterpriseUser();
              enterpriseUser.set("multiComplex",
                                 new ArrayNode(JsonNodeFactory.instance, Arrays.asList(multiComplex1, multiComplex2)));
              allTypes.setEnterpriseUser(enterpriseUser);
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);

            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedExtension.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedExtension.getMultiComplex().get(0);
            Assertions.assertEquals(1L, patchedMultiComplex1.getNumber().get());
            Assertions.assertEquals(1, patchedMultiComplex1.getNumberArray().size());
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex1.getNumberArray().get(0));

            AllTypes patchedMultiComplex2 = patchedExtension.getMultiComplex().get(1);
            Assertions.assertEquals(2L, patchedMultiComplex2.getNumber().get());
            Assertions.assertEquals(1, patchedMultiComplex2.getNumberArray().size());
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex2.getNumberArray().get(0));

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            }
          }

          /* ********************************************************************* */
        }

        @DisplayName("With Filter Tests")
        @Nested
        class WithFilterTests
        {

          /**
           * replace multiComplex.numberArray with a number-value that is not wrapped within an array
           */
          @DisplayName("success: REPLACE multiComplex.numberArray with non-array-value")
          @Test
          public void testReplaceMultiComplexNumberArrayWithNonArrayValue()
          {
            String attributeName = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:multiComplex";
            String filterPath = String.format("%s[number eq 2].numberArray", attributeName);
            JsonNode numberValue = new IntNode(5);

            List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                        .op(PatchOp.REPLACE)
                                                                                        .path(filterPath)
                                                                                        .valueNode(numberValue)
                                                                                        .build());
            PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

            AllTypes allTypes = new AllTypes(true);
            {
              AllTypes multiComplex1 = new AllTypes(false);
              multiComplex1.setNumber(1L);

              AllTypes multiComplex2 = new AllTypes(false);
              multiComplex2.setNumber(2L);

              EnterpriseUser enterpriseUser = new EnterpriseUser();
              enterpriseUser.set("multiComplex",
                                 new ArrayNode(JsonNodeFactory.instance, Arrays.asList(multiComplex1, multiComplex2)));
              allTypes.setEnterpriseUser(enterpriseUser);
            }
            addAllTypesToProvider(allTypes);
            PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                        allTypesResourceType.getResourceHandlerImpl(),
                                                                                        resourceEndpoint.getPatchWorkarounds(),
                                                                                        new Context(null));
            AllTypes patchedResource = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
            EnterpriseUser patchedEnterpriseUser = patchedResource.getEnterpriseUser().get();
            AllTypes patchedExtension = JsonHelper.copyResourceToObject(patchedEnterpriseUser, AllTypes.class);

            Assertions.assertTrue(patchRequestHandler.isResourceChanged());
            Assertions.assertEquals(2, patchedExtension.getMultiComplex().size());

            AllTypes patchedMultiComplex1 = patchedExtension.getMultiComplex().get(0);
            Assertions.assertEquals(1L, patchedMultiComplex1.getNumber().get());
            Assertions.assertEquals(0, patchedMultiComplex1.getNumberArray().size());

            AllTypes patchedMultiComplex2 = patchedExtension.getMultiComplex().get(1);
            Assertions.assertEquals(2L, patchedMultiComplex2.getNumber().get());
            Assertions.assertEquals(1, patchedMultiComplex2.getNumberArray().size());
            Assertions.assertEquals(numberValue.longValue(), patchedMultiComplex2.getNumberArray().get(0));

            // must be called
            {
              Mockito.verify(defaultPatchOperationHandler)
                     .handleOperation(Mockito.any(),
                                      Mockito.any(MultivaluedComplexMultivaluedSubAttributeOperation.class));
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
                     .handleOperation(Mockito.any(), Mockito.any(RemoveComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexAttributeOperation.class));
              Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                     .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
            }
          }
          /* ********************************************************************* */
        }
        /* ********************************************************************* */
      }
      /* ********************************************************************* */
    }
    /* ********************************************************************* */
  }
}
