package de.captaingoldfish.scim.sdk.server.patch.validationtests;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
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
@DisplayName("Unknown attribute tests")
public class PatchUnknownAttributesTests extends AbstractPatchTest
{


  @DisplayName("Resource-Patch Tests")
  @Nested
  class ResourcePatchTests
  {

    /**
     * this test makes sure that an appropriate error is returned if the patch-value is not able to represent the
     * resource itself
     */
    @DisplayName("patch-value is not an object")
    @Test
    public void valueIsNotAResource()
    {
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REPLACE)
                                                                                  .valueNode(new TextNode("hello world"))
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

      AllTypes allTypes = new AllTypes(true);
      addAllTypesToProvider(allTypes);
      PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                  allTypesResourceType.getResourceHandlerImpl(),
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
      BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                       () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
      Assertions.assertEquals("The resourceNode is not a valid JSON-object", ex.getMessage());
    }

    /**
     * this test makes sure that an appropriate error is returned if the patch-value is not able to represent the
     * resource itself
     */
    @DisplayName("patch-value is array")
    @Test
    public void valueIsInvalidJson()
    {
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REPLACE)
                                                                                  .values(Arrays.asList("[]"))
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

      AllTypes allTypes = new AllTypes(true);
      addAllTypesToProvider(allTypes);
      PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                  allTypesResourceType.getResourceHandlerImpl(),
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
      BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                       () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
      Assertions.assertEquals("The resourceNode is not a valid JSON-object", ex.getMessage());
    }

    @DisplayName("Main resource Tests")
    @Nested
    class MainResourceTests
    {

      @DisplayName("Ignore unknown attributes")
      @Nested
      class IgnoreUnknownAttributesTests
      {

        @BeforeEach
        public void ignoreUnknownAttributes()
        {
          serviceProvider.getPatchConfig().setIgnoreUnknownAttribute(true);
        }

        /**
         * adds a simple attribute with an unknown name
         */
        @DisplayName("do not fail: add simple unknown attribute")
        @Test
        public void testRemoveExtensionWithNullNode()
        {
          AllTypes patchResource = new AllTypes(true);

          patchResource.set("unknown", new TextNode("hello world"));

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .valueNode(patchResource)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setString("hello world");
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
          Assertions.assertEquals(3, patchedAllTypes.size());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex attribute with an unknown attribute-name
         */
        @DisplayName("do not fail: add complex unknown attribute")
        @Test
        public void testAddComplexUnknownAttribute()
        {
          AllTypes patchResource = new AllTypes(true);

          ObjectNode complex = new ObjectNode(JsonNodeFactory.instance);
          complex.set("costCenter", new TextNode("hello world"));
          patchResource.set("unknown", complex);

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .valueNode(patchResource)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setString("hello world");
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
          Assertions.assertEquals(3, patchedAllTypes.size());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex sub-attribute with an unknown attribute-name
         */
        @DisplayName("do not fail: add sub-attribute on unknown complex attribute")
        @Test
        public void testAddComplexSubUnknownAttribute()
        {
          AllTypes patchResource = new AllTypes(true);

          ObjectNode name = new ObjectNode(JsonNodeFactory.instance);
          name.set("unknown", new TextNode("hello world"));

          patchResource.set("name", name);

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .valueNode(patchResource)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setString("hello world");
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
          Assertions.assertEquals(3, patchedAllTypes.size());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }
      }

      @DisplayName("Fail on unknown attributes")
      @Nested
      class FailOnUnknownAttributesTests
      {

        /**
         * adds a simple attribute with an unknown name
         */
        @DisplayName("failure: add simple unknown attribute")
        @Test
        public void testAddSimpleUnknownAttribute()
        {
          AllTypes patchResource = new AllTypes(true);

          patchResource.set("unknown", new TextNode("hello world"));

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
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals("Attribute 'unknown' is unknown to resource type 'AllTypes'", ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex attribute with an unknown attribute-name
         */
        @DisplayName("failure: add complex unknown attribute")
        @Test
        public void testAddComplexUnknownAttribute()
        {
          AllTypes patchResource = new AllTypes(true);

          ObjectNode complex = new ObjectNode(JsonNodeFactory.instance);
          complex.set("costCenter", new TextNode("hello world"));
          patchResource.set("unknown", complex);

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
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals("Attribute 'unknown' is unknown to resource type 'AllTypes'", ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex sub-attribute with an unknown attribute-name
         */
        @DisplayName("failure: add sub-attribute on unknown complex attribute")
        @Test
        public void testAddComplexSubUnknownAttribute()
        {
          AllTypes patchResource = new AllTypes(true);

          ObjectNode complex = new ObjectNode(JsonNodeFactory.instance);
          complex.set("unknown", new TextNode("hello world"));
          patchResource.set("complex", complex);

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
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals(String.format("Attribute '%s:complex.unknown' is unknown to resource type 'AllTypes'",
                                                allTypesResourceType.getMainSchema().getNonNullId()),
                                  ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }
      }

    }

    @DisplayName("Extension Tests")
    @Nested
    class ExtensionTests
    {

      @DisplayName("Ignore unknown attributes")
      @Nested
      class IgnoreUnknownAttributesTests
      {

        @BeforeEach
        public void ignoreUnknownAttributes()
        {
          serviceProvider.getPatchConfig().setIgnoreUnknownAttribute(true);
        }

        /**
         * adds a simple attribute with an unknown name
         */
        @DisplayName("do not fail: add simple unknown attribute")
        @Test
        public void testRemoveExtensionWithNullNode()
        {
          AllTypes patchResource = new AllTypes(true);

          EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
          enterpriseUser.set("unknown", new TextNode("hello world"));
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
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("costCenter",
                                  patchedAllTypes.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).get());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex attribute with an unknown attribute-name
         */
        @DisplayName("do not fail: add complex unknown attribute")
        @Test
        public void testAddComplexUnknownAttribute()
        {
          AllTypes patchResource = new AllTypes(true);

          EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
          ObjectNode complex = new ObjectNode(JsonNodeFactory.instance);
          complex.set("costCenter", new TextNode("hello world"));
          enterpriseUser.set("unknown", complex);
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
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("costCenter",
                                  patchedAllTypes.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).get());
          Assertions.assertEquals(3, patchedAllTypes.size());
          Assertions.assertEquals(1, patchedAllTypes.getEnterpriseUser().get().size());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex sub-attribute with an unknown attribute-name
         */
        @DisplayName("do not fail: add sub-attribute on unknown complex attribute")
        @Test
        public void testAddComplexSubUnknownAttribute()
        {
          AllTypes patchResource = new AllTypes(true);

          EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
          ObjectNode complex = new ObjectNode(JsonNodeFactory.instance);
          complex.set("costCenter", new TextNode("hello world"));
          enterpriseUser.set("unknown", complex);
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
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("costCenter",
                                  patchedAllTypes.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).get());
          Assertions.assertEquals(3, patchedAllTypes.size());
          Assertions.assertEquals(1, patchedAllTypes.getEnterpriseUser().get().size());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }
      }

      @DisplayName("Fail on unknown attributes")
      @Nested
      class FailOnUnknownAttributesTests
      {

        /**
         * adds a simple attribute with an unknown name
         */
        @DisplayName("failure: add simple unknown attribute")
        @Test
        public void testAddSimpleUnknownAttribute()
        {
          AllTypes patchResource = new AllTypes(true);

          EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
          enterpriseUser.set("unknown", new TextNode("hello world"));
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
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals("Attribute 'unknown' is unknown to resource type 'AllTypes'", ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex attribute with an unknown attribute-name
         */
        @DisplayName("failure: add complex unknown attribute")
        @Test
        public void testAddComplexUnknownAttribute()
        {
          AllTypes patchResource = new AllTypes(true);

          EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
          ObjectNode complex = new ObjectNode(JsonNodeFactory.instance);
          complex.set("costCenter", new TextNode("hello world"));
          enterpriseUser.set("unknownComplex", complex);
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
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals("Attribute 'unknownComplex' is unknown to resource type 'AllTypes'", ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex sub-attribute with an unknown attribute-name
         */
        @DisplayName("failure: add sub-attribute on unknown complex attribute")
        @Test
        public void testAddComplexSubUnknownAttribute()
        {
          AllTypes patchResource = new AllTypes(true);

          EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
          ObjectNode complex = new ObjectNode(JsonNodeFactory.instance);
          complex.set("unknown", new TextNode("hello world"));
          enterpriseUser.set("manager", complex);
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
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals(String.format("Attribute '%s:manager.unknown' is unknown to resource type 'AllTypes'",
                                                SchemaUris.ENTERPRISE_USER_URI),
                                  ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
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

      @DisplayName("Ignore unknown attributes")
      @Nested
      class IgnoreUnknownAttributesTests
      {

        @BeforeEach
        public void ignoreUnknownAttributes()
        {
          serviceProvider.getPatchConfig().setIgnoreUnknownAttribute(true);
        }

        /**
         * adds a simple attribute with an unknown name
         */
        @DisplayName("do not fail: add simple unknown attribute")
        @Test
        public void testRemoveExtensionWithNullNode()
        {
          JsonNode unknownAttribute = new TextNode("hello world");

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path("unknown")
                                                                                      .valueNode(unknownAttribute)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setString("hello world");
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
          Assertions.assertEquals(3, patchedAllTypes.size());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex attribute with an unknown attribute-name
         */
        @DisplayName("do not fail: add complex unknown attribute")
        @Test
        public void testAddComplexUnknownAttribute()
        {
          ObjectNode unknownAttribute = new ObjectNode(JsonNodeFactory.instance);
          unknownAttribute.set("costCenter", new TextNode("hello world"));

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path("unknown")
                                                                                      .valueNode(unknownAttribute)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setString("hello world");
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
          Assertions.assertEquals(3, patchedAllTypes.size());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex sub-attribute with an unknown attribute-name
         */
        @DisplayName("do not fail: add sub-attribute on unknown complex attribute")
        @Test
        public void testAddComplexSubUnknownAttribute()
        {
          JsonNode unknownAttribute = new TextNode("hello world");

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path("complex.unknown")
                                                                                      .valueNode(unknownAttribute)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setString("hello world");
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
          Assertions.assertEquals(3, patchedAllTypes.size());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }
      }

      @DisplayName("Fail on unknown attributes")
      @Nested
      class FailOnUnknownAttributesTests
      {

        /**
         * adds a simple attribute with an unknown name
         */
        @DisplayName("failure: add simple unknown attribute")
        @Test
        public void testAddSimpleUnknownAttribute()
        {
          JsonNode unknownAttribute = new TextNode("hello world");

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path("unknown")
                                                                                      .valueNode(unknownAttribute)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("costCenter").build());
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals("Attribute 'unknown' is unknown to resource type 'AllTypes'", ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex attribute with an unknown attribute-name
         */
        @DisplayName("failure: add complex unknown attribute")
        @Test
        public void testAddComplexUnknownAttribute()
        {
          ObjectNode unknownAttribute = new ObjectNode(JsonNodeFactory.instance);
          unknownAttribute.set("costCenter", new TextNode("hello world"));

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path("unknown")
                                                                                      .valueNode(unknownAttribute)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("costCenter").build());
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals("Attribute 'unknown' is unknown to resource type 'AllTypes'", ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex sub-attribute with an unknown attribute-name
         */
        @DisplayName("failure: add sub-attribute on unknown complex attribute")
        @Test
        public void testAddComplexSubUnknownAttribute()
        {
          JsonNode unknownAttribute = new TextNode("hello world");

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path("complex.unknown")
                                                                                      .valueNode(unknownAttribute)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("costCenter").build());
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals("Attribute 'complex.unknown' is unknown to resource type 'AllTypes'",
                                  ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }
      }
    }

    @DisplayName("Extension Tests")
    @Nested
    class ExtensionTests
    {

      @DisplayName("Ignore unknown attributes")
      @Nested
      class IgnoreUnknownAttributesTests
      {

        @BeforeEach
        public void ignoreUnknownAttributes()
        {
          serviceProvider.getPatchConfig().setIgnoreUnknownAttribute(true);
        }

        /**
         * adds a simple attribute with an unknown name
         */
        @DisplayName("do not fail: add simple unknown attribute")
        @Test
        public void testRemoveExtensionWithNullNode()
        {
          AllTypes patchResource = new AllTypes(true);

          EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
          enterpriseUser.set("unknown", new TextNode("hello world"));
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
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("costCenter",
                                  patchedAllTypes.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).get());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex attribute with an unknown attribute-name
         */
        @DisplayName("do not fail: add complex unknown attribute")
        @Test
        public void testAddComplexUnknownAttribute()
        {
          ObjectNode complex = new ObjectNode(JsonNodeFactory.instance);
          complex.set("unknown", new TextNode("hello world"));

          final String attributeName = String.format("%s:%s", SchemaUris.ENTERPRISE_USER_URI, "unknown");

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path(attributeName)
                                                                                      .valueNode(complex)
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
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("costCenter",
                                  patchedAllTypes.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).get());
          Assertions.assertEquals(3, patchedAllTypes.size());
          Assertions.assertEquals(1, patchedAllTypes.getEnterpriseUser().get().size());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex sub-attribute with an unknown attribute-name
         */
        @DisplayName("do not fail: add sub-attribute on unknown complex attribute")
        @Test
        public void testAddComplexSubUnknownAttribute()
        {
          JsonNode unknownAttribute = new TextNode("hello world");
          final String attributeName = String.format("%s:%s", SchemaUris.ENTERPRISE_USER_URI, "manager.unknown");

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path(attributeName)
                                                                                      .valueNode(unknownAttribute)
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
          Assertions.assertFalse(patchRequestHandler.isResourceChanged());
          Assertions.assertEquals("costCenter",
                                  patchedAllTypes.getEnterpriseUser().flatMap(EnterpriseUser::getCostCenter).get());
          Assertions.assertEquals(3, patchedAllTypes.size());
          Assertions.assertEquals(1, patchedAllTypes.getEnterpriseUser().get().size());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }
      }

      @DisplayName("Fail on unknown attributes")
      @Nested
      class FailOnUnknownAttributesTests
      {

        /**
         * adds a simple attribute with an unknown name
         */
        @DisplayName("failure: add simple unknown attribute")
        @Test
        public void testAddSimpleUnknownAttribute()
        {
          JsonNode unknownAttribute = new TextNode("hello world");
          final String attributeName = String.format("%s:%s", SchemaUris.ENTERPRISE_USER_URI, "unknown");

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path(attributeName)
                                                                                      .valueNode(unknownAttribute)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("costCenter").build());
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals(String.format("Attribute '%s' is unknown to resource type 'AllTypes'", attributeName),
                                  ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex attribute with an unknown attribute-name
         */
        @DisplayName("failure: add complex unknown attribute")
        @Test
        public void testAddComplexUnknownAttribute()
        {
          ObjectNode complex = new ObjectNode(JsonNodeFactory.instance);
          complex.set("unknown", new TextNode("hello world"));

          final String attributeName = String.format("%s:%s", SchemaUris.ENTERPRISE_USER_URI, "unknown");

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path(attributeName)
                                                                                      .valueNode(complex)
                                                                                      .build());

          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("costCenter").build());
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals(String.format("Attribute '%s' is unknown to resource type 'AllTypes'", attributeName),
                                  ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }

        /**
         * adds a complex sub-attribute with an unknown attribute-name
         */
        @DisplayName("failure: add sub-attribute on unknown complex attribute")
        @Test
        public void testAddComplexSubUnknownAttribute()
        {
          JsonNode unknownAttribute = new TextNode("hello world");
          final String attributeName = String.format("%s:%s", SchemaUris.ENTERPRISE_USER_URI, "manager.unknown");

          List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                      .op(PatchOp.REPLACE)
                                                                                      .path(attributeName)
                                                                                      .valueNode(unknownAttribute)
                                                                                      .build());
          PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

          AllTypes allTypes = new AllTypes(true);
          allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("costCenter").build());
          addAllTypesToProvider(allTypes);
          PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                      allTypesResourceType.getResourceHandlerImpl(),
                                                                                      resourceEndpoint.getPatchWorkarounds(),
                                                                                      new Context(null));
          BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                           () -> patchRequestHandler.handlePatchRequest(patchOpRequest));
          Assertions.assertEquals(String.format("Attribute '%s' is unknown to resource type 'AllTypes'", attributeName),
                                  ex.getMessage());

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
            Mockito.verify(defaultPatchOperationHandler, Mockito.never())
                   .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
          }
        }
      }

    }
  }
}
