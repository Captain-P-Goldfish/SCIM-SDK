package de.captaingoldfish.scim.sdk.server.patch.validationtests;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.NullNode;

import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
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


/**
 * @author Pascal Knueppel
 * @since 19.01.2024
 */
@DisplayName("Extension reference tests")
public class ExtensionRefTests extends AbstractPatchTest
{

  /**
   * adds custom attributes of all types to the enterprise user schema. These attributes are ambiguous to those
   * of the main-schema
   */
  @BeforeEach
  public void extendExtensionByAttributes()
  {
    addCustomAttributesToEnterpriseUserSchema();
  }

  @DisplayName("Resource-Patch Tests")
  @Nested
  class ResourcePatchTests
  {

    /**
     * removes an extension by using a null-node reference
     */
    @DisplayName("success: remove extension with null node")
    @Test
    public void testRemoveExtensionWithNullNode()
    {
      AllTypes patchResource = new AllTypes(true);

      patchResource.set(SchemaUris.ENTERPRISE_USER_URI, NullNode.getInstance());

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
      Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent());

      // must be called
      {
        Mockito.verify(defaultPatchOperationHandler)
               .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
      }
      // must not be called
      {
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
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
      }
    }

    /**
     * patch an extension with implicit remove by using an empty extension object
     */
    @DisplayName("success: Patch extension with empty reference (implicit REMOVE)")
    @Test
    public void testPatchExtensionWithEmptyResource()
    {
      AllTypes patchResource = new AllTypes(true);

      patchResource.setEnterpriseUser(EnterpriseUser.builder().build());

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
      Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent());

      // must be called
      {
        Mockito.verify(defaultPatchOperationHandler)
               .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
      }
      // must not be called
      {
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
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
      }
    }

    /**
     * patch simple attributes on extension
     */
    @DisplayName("success: Patch extension with simple attributes")
    @Test
    public void testPatchExtensionSimpleAttributes()
    {
      AllTypes patchResource = new AllTypes(true);

      patchResource.setEnterpriseUser(EnterpriseUser.builder()
                                                    .costCenter("costCenter")
                                                    .employeeNumber("123456")
                                                    .build());

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
      Assertions.assertEquals("costCenter", patchedAllTypes.getEnterpriseUser().get().getCostCenter().get());
      Assertions.assertEquals("123456", patchedAllTypes.getEnterpriseUser().get().getEmployeeNumber().get());

      // must be called
      {
        Mockito.verify(defaultPatchOperationHandler, Mockito.times(2))
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

  @DisplayName("Path-Patch Tests")
  @Nested
  class PathPatchTests
  {

    /**
     * remove the whole extension with a direct-path reference
     */
    @DisplayName("success: Patch remove extension")
    @Test
    public void testRemoveExtensionWithDirectReference()
    {

      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REMOVE)
                                                                                  .path(SchemaUris.ENTERPRISE_USER_URI)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

      AllTypes allTypes = new AllTypes(true);
      EnterpriseUser enterpriseUser = EnterpriseUser.builder()
                                                    .costCenter("costCenter")
                                                    .employeeNumber("123456")
                                                    .build();
      allTypes.setEnterpriseUser(enterpriseUser);
      addAllTypesToProvider(allTypes);
      PatchRequestHandler<AllTypes> patchRequestHandler = new PatchRequestHandler(allTypes.getId().get(),
                                                                                  allTypesResourceType.getResourceHandlerImpl(),
                                                                                  resourceEndpoint.getPatchWorkarounds(),
                                                                                  new Context(null));
      AllTypes patchedAllTypes = Assertions.assertDoesNotThrow(() -> patchRequestHandler.handlePatchRequest(patchOpRequest));
      Assertions.assertTrue(patchRequestHandler.isResourceChanged());
      Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent());

      // must be called
      {
        Mockito.verify(defaultPatchOperationHandler)
               .handleOperation(Mockito.any(), Mockito.any(RemoveExtensionRefOperation.class));
      }
      // must not be called
      {
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
        Mockito.verify(defaultPatchOperationHandler, Mockito.never())
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
      }
    }

    /**
     * patch a simple number value
     */
    @DisplayName("success: Patch direct extension reference")
    @Test
    public void testPatchExtensionWithDirectReference()
    {
      EnterpriseUser enterpriseUser = EnterpriseUser.builder()
                                                    .costCenter("costCenter")
                                                    .employeeNumber("123456")
                                                    .build();

      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.REPLACE)
                                                                                  .path(SchemaUris.ENTERPRISE_USER_URI)
                                                                                  .valueNode(enterpriseUser)
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
      Assertions.assertEquals("costCenter", patchedAllTypes.getEnterpriseUser().get().getCostCenter().get());
      Assertions.assertEquals("123456", patchedAllTypes.getEnterpriseUser().get().getEmployeeNumber().get());

      // must be called
      {
        Mockito.verify(defaultPatchOperationHandler, Mockito.times(2))
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
               .handleOperation(Mockito.any(), Mockito.any(MultivaluedComplexSimpleSubAttributeOperation.class));
      }
    }
  }
}
