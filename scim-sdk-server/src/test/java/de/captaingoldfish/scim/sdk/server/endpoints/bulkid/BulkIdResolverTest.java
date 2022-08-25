package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.UriInfos;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 21.08.2022 - 10:55 <br>
 * <br>
 */
@Slf4j
public class BulkIdResolverTest
{

  private ResourceTypeFactory resourceTypeFactory;

  private ResourceType userResourceType;

  private ResourceType groupResourceType;

  /**
   * will register the user resource type and group resource type on the resource type factory
   */
  @BeforeEach
  public void initialize()
  {
    resourceTypeFactory = new ResourceTypeFactory();

    JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    this.userResourceType = resourceTypeFactory.registerResourceType(null,
                                                                     userResourceTypeNode,
                                                                     userResourceSchema,
                                                                     enterpriseUserExtension);


    JsonNode groupResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_RESOURCE_TYPE_JSON);
    JsonNode groupResourceSchema = JsonHelper.loadJsonDocument(ClassPathReferences.GROUP_SCHEMA_JSON);
    this.groupResourceType = resourceTypeFactory.registerResourceType(null, groupResourceTypeNode, groupResourceSchema);
  }

  /**
   * will verify that no exception occurs if the resource has no bulkId members
   */
  @Test
  public void testResolveBulkIdsInEmptyResource()
  {
    BulkIdResolver bulkIdResolver = new BulkIdResolver();

    String bulkId = UUID.randomUUID().toString();

    Group group = Group.builder().displayName("my-group").build();

    bulkIdResolver.createNewBulkIdResolver(bulkId, buildUriInfos(groupResourceType), group.toString());
    Assertions.assertDoesNotThrow(() -> bulkIdResolver.addResolvedBulkId("1", UUID.randomUUID().toString()));
  }

  /**
   * verifies that the bulkIds of the complex members within the group resource are correctly resolved
   */
  @Test
  public void testResolveBulkIdsWithMembers()
  {
    BulkIdResolver bulkIdResolver = new BulkIdResolver();

    String bulkId = UUID.randomUUID().toString();

    List<Member> memberList = Arrays.asList(Member.builder().value(toReference("1")).build(),
                                            Member.builder().value(toReference("2")).build(),
                                            Member.builder().value(toReference("3")).build(),
                                            Member.builder().value(toReference("4")).build(),
                                            Member.builder().value(toReference("1")).build());

    Group group = Group.builder().members(memberList).build();

    BulkIdResolverAbstract<?> resolver = bulkIdResolver.createNewBulkIdResolver(bulkId,
                                                                                buildUriInfos(groupResourceType),
                                                                                group.toString());

    bulkIdResolver.addResolvedBulkId("1", "11");
    bulkIdResolver.addResolvedBulkId("2", "22");
    bulkIdResolver.addResolvedBulkId("3", "33");
    bulkIdResolver.addResolvedBulkId("4", "44");

    Group replacedGroup = resolver.getResource(Group.class);
    Assertions.assertEquals("11", replacedGroup.getMembers().get(0).getValue().get());
    Assertions.assertEquals("22", replacedGroup.getMembers().get(1).getValue().get());
    Assertions.assertEquals("33", replacedGroup.getMembers().get(2).getValue().get());
    Assertions.assertEquals("44", replacedGroup.getMembers().get(3).getValue().get());
    Assertions.assertEquals("11", replacedGroup.getMembers().get(4).getValue().get());
  }

  /**
   * verifies that the bulkIds of the complex members within the group resource are correctly resolved
   */
  @Test
  public void testResolveBulkIdsWithMixedBulkIdMembers()
  {
    BulkIdResolver bulkIdResolver = new BulkIdResolver();

    String bulkId = UUID.randomUUID().toString();

    final String value1 = toReference("1");
    final String value2 = UUID.randomUUID().toString();
    final String value3 = toReference("3");
    final String value4 = UUID.randomUUID().toString();
    final String value5 = UUID.randomUUID().toString();
    List<Member> memberList = Arrays.asList(Member.builder().value(value1).build(),
                                            Member.builder().value(value2).build(),
                                            Member.builder().value(value3).build(),
                                            Member.builder().value(value4).build(),
                                            Member.builder().value(value5).build());

    Group group = Group.builder().members(memberList).build();

    BulkIdResolverAbstract<?> resolver = bulkIdResolver.createNewBulkIdResolver(bulkId,
                                                                                buildUriInfos(groupResourceType),
                                                                                group.toString());

    bulkIdResolver.addResolvedBulkId("1", "11");
    bulkIdResolver.addResolvedBulkId("3", "33");

    Group replacedGrouop = resolver.getResource(Group.class);
    Assertions.assertEquals("11", replacedGrouop.getMembers().get(0).getValue().get());
    Assertions.assertEquals(value2, replacedGrouop.getMembers().get(1).getValue().get());
    Assertions.assertEquals("33", replacedGrouop.getMembers().get(2).getValue().get());
    Assertions.assertEquals(value4, replacedGrouop.getMembers().get(3).getValue().get());
    Assertions.assertEquals(value5, replacedGrouop.getMembers().get(4).getValue().get());
  }

  /**
   * verifies that the manager reference within the enterprise user is correctly resolved
   */
  @Test
  public void testResolveEnterpriseManagerBulkIdReference()
  {
    BulkIdResolver bulkIdResolver = new BulkIdResolver();

    String bulkId = UUID.randomUUID().toString();

    Manager manager = Manager.builder().value(toReference("1")).build();
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().manager(manager).build();
    User user = User.builder().enterpriseUser(enterpriseUser).build();

    BulkIdResolverAbstract<?> resolver = bulkIdResolver.createNewBulkIdResolver(bulkId,
                                                                                buildUriInfos(userResourceType),
                                                                                user.toString());

    bulkIdResolver.addResolvedBulkId("1", "11");

    User resolvedUser = resolver.getResource(User.class);
    Assertions.assertEquals(resolvedUser.getEnterpriseUser().get().getManager().get().getValue().get(), "11");
  }

  /**
   * this test will make sure that a self-reference is answered with a bad request
   */
  @Test
  public void testRejectSelfReferences()
  {
    BulkIdResolver bulkIdResolver = new BulkIdResolver();

    String bulkId = "1";

    Manager manager = Manager.builder().value(toReference(bulkId)).build();
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().manager(manager).build();
    User user = User.builder().enterpriseUser(enterpriseUser).build();

    Exception ex = Assertions.assertThrows(BadRequestException.class,
                                           () -> bulkIdResolver.createNewBulkIdResolver(bulkId,
                                                                                        buildUriInfos(userResourceType),
                                                                                        user.toString()));
    String errorMessage = String.format("the bulkId '%s' is a self-reference. Self-references will not be resolved",
                                        bulkId);
    Assertions.assertEquals(errorMessage, ex.getMessage());
  }

  /**
   * this test will utilize the enterprise-user manager attribute to build a circular reference between two
   * operations. Such a circular reference cannot be resolved and will cause an error:<br>
   * <br>
   * the following structure is being tested:
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:BulkRequest" ],
   *   "Operations" : [ {
   *     "method" : "POST",
   *     "bulkId" : "1",
   *     "path" : "/Users/201e999b-e64b-4ef6-a4d1-b42c7a003f5e",
   *     "data" : {
   *                   "schemas": [
   *                      "urn:ietf:params:scim:schemas:core:2.0:User",
   *                      "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
   *                   ],
   *                   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *                      "manager": {
   *                         "value": "bulkId:2",
   *                      }
   *                   }
   *               }
   *       }
   *   },{
   *     "method" : "POST",
   *     "bulkId" : "2",
   *     "path" : "/Users/201e999b-e64b-4ef6-a4d1-b42c7a003f5e",
   *     "data" : {
   *                   "schemas": [
   *                      "urn:ietf:params:scim:schemas:core:2.0:User",
   *                      "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
   *                   ],
   *                   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *                      "manager": {
   *                         "value": "bulkId:1",
   *                      }
   *                   }
   *               }
   *       }
   *   }]
   * }
   * </pre>
   */
  @Test
  public void testFindCircularReferenceBetweenUsers()
  {
    BulkIdResolver bulkIdResolver = new BulkIdResolver();

    String bulkId1 = "1";
    String bulkId2 = "2";

    Manager manager1 = Manager.builder().value(toReference(bulkId2)).build();
    EnterpriseUser enterpriseUser1 = EnterpriseUser.builder().manager(manager1).build();
    User user1 = User.builder().enterpriseUser(enterpriseUser1).build();

    Manager manager2 = Manager.builder().value(toReference(bulkId1)).build();
    EnterpriseUser enterpriseUser2 = EnterpriseUser.builder().manager(manager2).build();
    User user2 = User.builder().enterpriseUser(enterpriseUser2).build();

    bulkIdResolver.createNewBulkIdResolver(bulkId1, buildUriInfos(userResourceType), user1.toString());
    // the next call should identify a circular reference and thus an exception should occur
    Exception ex = Assertions.assertThrows(ConflictException.class,
                                           () -> bulkIdResolver.createNewBulkIdResolver(bulkId2,
                                                                                        buildUriInfos(userResourceType),
                                                                                        user2.toString()));
    String errorMessage = String.format("the bulkIds '%s' and '%s' form a direct or indirect circular reference "
                                        + "that cannot be resolved.",
                                        bulkId1,
                                        bulkId2);
    Assertions.assertEquals(errorMessage, ex.getMessage());
  }

  /**
   * this test will utilize the enterprise-user manager attribute to build a circular reference between three
   * operations. Such a circular reference cannot be resolved and will cause an error:<br>
   * <br>
   * the following structure is being tested:
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:BulkRequest" ],
   *   "Operations" : [ {
   *     "method" : "POST",
   *     "bulkId" : "1",
   *     "path" : "/Users/201e999b-e64b-4ef6-a4d1-b42c7a003f5e",
   *     "data" : {
   *                   "schemas": [
   *                      "urn:ietf:params:scim:schemas:core:2.0:User",
   *                      "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
   *                   ],
   *                   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *                      "manager": {
   *                         "value": "bulkId:2",
   *                      }
   *                   }
   *               }
   *       }
   *   },{
   *     "method" : "POST",
   *     "bulkId" : "2",
   *     "path" : "/Users/201e999b-e64b-4ef6-a4d1-b42c7a003f5e",
   *     "data" : {
   *                   "schemas": [
   *                      "urn:ietf:params:scim:schemas:core:2.0:User",
   *                      "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
   *                   ],
   *                   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *                      "manager": {
   *                         "value": "bulkId:3",
   *                      }
   *                   }
   *               }
   *       }
   *   },{
   *     "method" : "POST",
   *     "bulkId" : "3",
   *     "path" : "/Users/201e999b-e64b-4ef6-a4d1-b42c7a003f5e",
   *     "data" : {
   *                   "schemas": [
   *                      "urn:ietf:params:scim:schemas:core:2.0:User",
   *                      "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
   *                   ],
   *                   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *                      "manager": {
   *                         "value": "bulkId:1",
   *                      }
   *                   }
   *               }
   *       }
   *   }]
   *
   * }
   * </pre>
   */
  @Test
  public void testFindCircularReferenceBetweenUsersWithComplexCircle()
  {
    BulkIdResolver bulkIdResolver = new BulkIdResolver();

    String bulkId1 = "1";
    String bulkId2 = "2";
    String bulkId3 = "3";

    Manager manager1 = Manager.builder().value(toReference(bulkId2)).build();
    EnterpriseUser enterpriseUser1 = EnterpriseUser.builder().manager(manager1).build();
    User user1 = User.builder().enterpriseUser(enterpriseUser1).build();

    Manager manager2 = Manager.builder().value(toReference(bulkId3)).build();
    EnterpriseUser enterpriseUser2 = EnterpriseUser.builder().manager(manager2).build();
    User user2 = User.builder().enterpriseUser(enterpriseUser2).build();

    Manager manager3 = Manager.builder().value(toReference(bulkId1)).build();
    EnterpriseUser enterpriseUser3 = EnterpriseUser.builder().manager(manager3).build();
    User user3 = User.builder().enterpriseUser(enterpriseUser3).build();

    bulkIdResolver.createNewBulkIdResolver(bulkId1, buildUriInfos(userResourceType), user1.toString());
    bulkIdResolver.createNewBulkIdResolver(bulkId2, buildUriInfos(userResourceType), user2.toString());
    // the next call should identify a circular reference and thus an exception should occur
    Exception ex = Assertions.assertThrows(ConflictException.class,
                                           () -> bulkIdResolver.createNewBulkIdResolver(bulkId3,
                                                                                        buildUriInfos(userResourceType),
                                                                                        user3.toString()));
    String errorMessage = String.format("the bulkIds '%s' and '%s' form a direct or indirect circular reference "
                                        + "that cannot be resolved.",
                                        bulkId1,
                                        bulkId3);
    Assertions.assertEquals(errorMessage, ex.getMessage());
  }

  /**
   * this test will verify that a bulkId reference within a complex node within a patch request is correctly
   * resolved if the path-attribute points to the complex parent object of a bulkId-reference-field
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:BulkRequest" ],
   *   "Operations" : [ {
   *     "method" : "PATCH",
   *     "bulkId" : "1",
   *     "path" : "/Users/e87c117f-42b1-414a-8253-c7fa247f19ac",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *       "Operations" : [ {
   *         "path" : "manager",
   *         "op" : "replace",
   *         "value" : "{\"value\":\"bulkId:2\"}"
   *       },{
   *         "path" : "manager",
   *         "op" : "replace",                      <- does not make sense doing it twice, it is simply for testing
   *         "value" : "{\"value\":\"bulkId:3\"}"
   *       } ]
   *     }
   *   }, {
   *     "method" : "POST",
   *     "bulkId" : "2",
   *     "path" : "/Users",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User" ],
   *       "userName" : "goldfish"
   *     }
   *   } , {
   *     "method" : "POST",
   *     "bulkId" : "3",
   *     "path" : "/Users",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User" ],
   *       "userName" : "shark"
   *     }
   *   } ]
   * }
   * </pre>
   */
  @Test
  public void testComplexPatchPathWithComplexTypeReference()
  {
    BulkIdResolver bulkIdResolver = new BulkIdResolver();

    final String bulkId1 = "1";
    final String bulkId2 = "2";
    final String bulkId3 = "3";

    final String userId1 = UUID.randomUUID().toString();
    final String userId2 = UUID.randomUUID().toString();

    final String valueNode1 = String.format("{\"%s\": \"%s\"}", AttributeNames.RFC7643.VALUE, toReference(bulkId2));
    final String valueNode2 = String.format("{\"%s\": \"%s\"}", AttributeNames.RFC7643.VALUE, toReference(bulkId3));
    final String expectedValueNode1 = String.format("{\"%s\": \"%s\"}", AttributeNames.RFC7643.VALUE, userId1);
    final String expectedValueNode2 = String.format("{\"%s\": \"%s\"}", AttributeNames.RFC7643.VALUE, userId2);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(AttributeNames.RFC7643.MANAGER)
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode1)
                                                                                .build(),
                                                           PatchRequestOperation.builder()
                                                                                .path(AttributeNames.RFC7643.MANAGER)
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode2)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    User user1 = User.builder().userName("goldfish").build();
    User user2 = User.builder().userName("shark").build();

    bulkIdResolver.createNewBulkIdResolver(bulkId2, buildUriInfos(userResourceType), user1.toString());
    bulkIdResolver.createNewBulkIdResolver(bulkId3, buildUriInfos(userResourceType), user2.toString());

    // now resolve the bulkId context by generating an id to replace the bulkId
    bulkIdResolver.addResolvedBulkId(bulkId2, userId1);
    bulkIdResolver.addResolvedBulkId(bulkId3, userId2);

    BulkIdResolverAbstract<?> resolver = bulkIdResolver.createNewBulkIdResolver(bulkId1,
                                                                                buildUriInfos(userResourceType,
                                                                                              HttpMethod.PATCH),
                                                                                patchOpRequest.toString());
    PatchOpRequest opRequest = (PatchOpRequest)resolver.getResource();
    Assertions.assertEquals(expectedValueNode1, opRequest.getOperations().get(0).getValues().get(0));
    Assertions.assertEquals(expectedValueNode2, opRequest.getOperations().get(1).getValues().get(0));
  }

  /**
   * this test will verify that a bulkId reference within a complex node within a patch request is correctly
   * resolved if the path-attribute points directly to the simple-value-field of a complex bulkId-reference
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:BulkRequest" ],
   *   "Operations" : [ {
   *     "method" : "PATCH",
   *     "bulkId" : "1",
   *     "path" : "/Users/e87c117f-42b1-414a-8253-c7fa247f19ac",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *       "Operations" : [ {
   *         "path" : "manager.value",
   *         "op" : "replace",
   *         "value" : "bulkId:2"
   *       },{
   *         "path" : "manager.value",
   *         "op" : "replace",                      <- does not make sense doing it twice, it is simply for testing
   *         "value" : "bulkId:3"
   *       } ]
   *     }
   *   }, {
   *     "method" : "POST",
   *     "bulkId" : "2",
   *     "path" : "/Users",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User" ],
   *       "userName" : "goldfish"
   *     }
   *   } , {
   *     "method" : "POST",
   *     "bulkId" : "3",
   *     "path" : "/Users",
   *     "data" : {
   *       "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User" ],
   *       "userName" : "shark"
   *     }
   *   } ]
   * }
   * </pre>
   */
  @Test
  public void testComplexPatchPathWithDirectValueReference()
  {
    BulkIdResolver bulkIdResolver = new BulkIdResolver();

    final String bulkId1 = "1";
    final String bulkId2 = "2";
    final String bulkId3 = "3";

    final String attributePath = String.format("%s.%s", AttributeNames.RFC7643.MANAGER, AttributeNames.RFC7643.VALUE);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .path(attributePath)
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(toReference(bulkId2))
                                                                                .build(),
                                                           PatchRequestOperation.builder()
                                                                                .path(attributePath)
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(toReference(bulkId3))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    final String userId1 = UUID.randomUUID().toString();
    final String userId2 = UUID.randomUUID().toString();

    User user1 = User.builder().userName("goldfish").build();
    User user2 = User.builder().userName("shark").build();

    bulkIdResolver.createNewBulkIdResolver(bulkId2, buildUriInfos(userResourceType), user1.toString());
    Assertions.assertTrue(bulkIdResolver.getBulkIdResolver(bulkId2).isPresent());
    bulkIdResolver.createNewBulkIdResolver(bulkId3, buildUriInfos(userResourceType), user2.toString());
    Assertions.assertTrue(bulkIdResolver.getBulkIdResolver(bulkId3).isPresent());

    // now resolve the bulkId context by generating an id to replace the bulkId
    bulkIdResolver.addResolvedBulkId(bulkId2, userId1);
    bulkIdResolver.addResolvedBulkId(bulkId3, userId2);

    BulkIdResolverAbstract resolver = bulkIdResolver.createNewBulkIdResolver(bulkId1,
                                                                             buildUriInfos(userResourceType,
                                                                                           HttpMethod.PATCH),
                                                                             patchOpRequest.toString());
    Assertions.assertEquals(userId1,
                            ((PatchOpRequest)resolver.getResource()).getOperations().get(0).getValues().get(0));
    Assertions.assertEquals(userId2,
                            ((PatchOpRequest)resolver.getResource()).getOperations().get(1).getValues().get(0));
  }

  /**
   * this test will verify that a bulkId reference is resolved correctly if the patch request does not contain a
   * path-attribute but a direct resource reference
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
   *   "Operations" : [ {
   *     "op" : "add",
   *     "value" : "{
   *                    "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User",
   *                                  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User" ],
   *                    "userName" : "goldfish",
   *                    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User" : {
   *                      "manager" : {
   *                        "value" : "bulkId:1"
   *                      }
   *                    }
   *                 }"
   *   }, {
   *     "op" : "add",
   *     "value" : "{
   *                    "schemas" : [ "urn:ietf:params:scim:schemas:core:2.0:User",
   *                                   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User" ],
   *                    "userName" : "shark",
   *                    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User" : {
   *                      "manager" : {
   *                        "value" : "bulkId:1"
   *                      }
   *                    }
   *                }"
   *   } ]
   * }
   * </pre>
   */
  @Test
  public void testResolveBulkIdsInPatchOperationWithoutPath()
  {
    BulkIdResolver bulkIdResolver = new BulkIdResolver();

    final String bulkId1 = "1";
    final String bulkId2 = "2";

    Manager manager1 = Manager.builder().value(toReference(bulkId1)).build();
    EnterpriseUser enterpriseUser1 = EnterpriseUser.builder().manager(manager1).build();
    User user1 = User.builder().userName("goldfish").enterpriseUser(enterpriseUser1).build();

    Manager manager2 = Manager.builder().value(toReference(bulkId1)).build();
    EnterpriseUser enterpriseUser2 = EnterpriseUser.builder().manager(manager2).build();
    User user2 = User.builder().userName("shark").enterpriseUser(enterpriseUser2).build();

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .value(user1.toString())
                                                                                .build(),
                                                           PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .value(user2.toString())
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    // to verify that the behaviour within bulkIdResolver is the same as within the BulkEndpoint
    patchOpRequest = JsonHelper.readJsonDocument(patchOpRequest.toString(), PatchOpRequest.class);

    // this will find the bulkIds but no bulkIds will be resolved yet
    bulkIdResolver.createNewBulkIdResolver(bulkId2,
                                           buildUriInfos(userResourceType, HttpMethod.PATCH),
                                           patchOpRequest.toString());
    Assertions.assertTrue(bulkIdResolver.isOpenBulkIdReferences());

    // now resolve the bulkId context by generating an id to replace the bulkId
    final String resourceId = UUID.randomUUID().toString();
    bulkIdResolver.addResolvedBulkId(bulkId1, resourceId);
    Assertions.assertFalse(bulkIdResolver.isOpenBulkIdReferences());

    BulkIdResolverAbstract<?> resolver = bulkIdResolver.createNewBulkIdResolver(bulkId2,
                                                                                buildUriInfos(userResourceType,
                                                                                              HttpMethod.PATCH),
                                                                                patchOpRequest.toString());

    PatchOpRequest resolvedPatchOpRequest = (PatchOpRequest)resolver.getResource();
    User resolvedUser1 = JsonHelper.readJsonDocument(resolvedPatchOpRequest.getOperations().get(0).getValues().get(0),
                                                     User.class);
    User resolvedUser2 = JsonHelper.readJsonDocument(resolvedPatchOpRequest.getOperations().get(1).getValues().get(0),
                                                     User.class);

    Assertions.assertEquals(resourceId, resolvedUser1.getEnterpriseUser().get().getManager().get().getValue().get());
    Assertions.assertEquals(resourceId, resolvedUser2.getEnterpriseUser().get().getManager().get().getValue().get());
  }

  private String toReference(String bulkId)
  {
    return String.format("%s:%s", AttributeNames.RFC7643.BULK_ID, bulkId);
  }

  private UriInfos buildUriInfos(ResourceType resourceType)
  {
    return buildUriInfos(resourceType, HttpMethod.POST);
  }

  private UriInfos buildUriInfos(ResourceType resourceType, HttpMethod method)
  {
    Map<String, String> httpHeaders = new HashMap<>();
    httpHeaders.put(HttpHeader.CONTENT_TYPE_HEADER, HttpHeader.SCIM_CONTENT_TYPE);
    return UriInfos.builder()
                   .resourceType(resourceType)
                   .httpMethod(method)
                   .resourceEndpoint(resourceType.getEndpoint())
                   .resourceId(method.equals(HttpMethod.POST) ? null : UUID.randomUUID().toString())
                   .httpHeaders(httpHeaders)
                   .build();
  }
}
