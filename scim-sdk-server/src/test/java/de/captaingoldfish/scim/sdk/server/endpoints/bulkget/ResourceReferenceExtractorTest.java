package de.captaingoldfish.scim.sdk.server.endpoints.bulkget;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.server.custom.endpoints.BulkIdReferencesEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.custom.resources.BulkIdReferences;
import de.captaingoldfish.scim.sdk.server.custom.resources.BulkIdReferences.Member;
import de.captaingoldfish.scim.sdk.server.custom.resources.BulkIdReferences.MemberList;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpointHandlerUtil;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 05.09.2022 - 22:50 <br>
 * <br>
 */
@Slf4j
public class ResourceReferenceExtractorTest
{

  /**
   * the service provider configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * necessary to get access to the resource types
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the parent resource type that contains the nodes that reference other resources
   */
  private ResourceType parentResourceType;

  @BeforeEach
  public void testBulkResourceReferenceSimpleTest()
  {
    serviceProvider = ServiceProvider.builder().bulkConfig(BulkConfig.builder().supported(true).build()).build();
    this.resourceTypeFactory = new ResourceTypeFactory();
    ResourceEndpointHandlerUtil.registerAllEndpoints(resourceTypeFactory, serviceProvider);
    {
      BulkIdReferencesEndpointDefinition endpointDefinition = new BulkIdReferencesEndpointDefinition();
      this.parentResourceType = resourceTypeFactory.registerResourceType(endpointDefinition.getResourceHandler(),
                                                                         endpointDefinition.getResourceType(),
                                                                         endpointDefinition.getResourceSchema(),
                                                                         endpointDefinition.getResourceSchemaExtensions()
                                                                                           .toArray(new JsonNode[0]));
    }
  }

  /**
   * will try to extract simple resource references from the main resource
   */
  @Test
  public void testExtractSimpleResourceReferencesOnMainResource()
  {
    BulkIdReferences resource = BulkIdReferences.builder()
                                                .userId("1")
                                                .userIdList(Arrays.asList("2", "3"))
                                                .member(Member.builder()
                                                              .userId("4")
                                                              .userIdList(Arrays.asList("5", "6"))
                                                              .build())
                                                .memberList(Arrays.asList(MemberList.builder()
                                                                                    .groupId("7")
                                                                                    .userIdList(Arrays.asList("8", "9"))
                                                                                    .build(),
                                                                          MemberList.builder()
                                                                                    .groupId("10")
                                                                                    .userIdList(Arrays.asList("11",
                                                                                                              "12"))
                                                                                    .build()))
                                                .build();

    ResourceReferenceExtractor referenceExtractor = ResourceReferenceExtractor.builder()
                                                                              .resource(resource)
                                                                              .resourceType(parentResourceType)
                                                                              .resourceTypeFactory(resourceTypeFactory)
                                                                              .build();
    List<ResourceReference> resourceReferences = referenceExtractor.getResourceReferences();
    Assertions.assertEquals(12, resourceReferences.size());
    MatcherAssert.assertThat(resourceReferences.stream()
                                               .map(ResourceReference::getResourceId)
                                               .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder("1",
                                                         "2",
                                                         "3",
                                                         "4",
                                                         "5",
                                                         "6",
                                                         "7",
                                                         "8",
                                                         "9",
                                                         "10",
                                                         "11",
                                                         "12"));
  }

  /**
   * will try to extract simple resource references from an extension object
   */
  @Test
  public void testExtractSimpleResourceReferencesOnExtension()
  {
    BulkIdReferences extension = BulkIdReferences.builder()
                                                 .isExtension(true)
                                                 .userId("1")
                                                 .userIdList(Arrays.asList("2", "3"))
                                                 .member(Member.builder()
                                                               .userId("4")
                                                               .userIdList(Arrays.asList("5", "6"))
                                                               .build())
                                                 .memberList(Arrays.asList(MemberList.builder()
                                                                                     .groupId("7")
                                                                                     .userIdList(Arrays.asList("8",
                                                                                                               "9"))
                                                                                     .build(),
                                                                           MemberList.builder()
                                                                                     .groupId("10")
                                                                                     .userIdList(Arrays.asList("11",
                                                                                                               "12"))
                                                                                     .build()))
                                                 .build();
    BulkIdReferences resource = BulkIdReferences.builder().extension(extension).build();

    log.info(resource.toPrettyString());


    ResourceReferenceExtractor referenceExtractor = ResourceReferenceExtractor.builder()
                                                                              .resource(resource)
                                                                              .resourceType(parentResourceType)
                                                                              .resourceTypeFactory(resourceTypeFactory)
                                                                              .build();
    List<ResourceReference> resourceReferences = referenceExtractor.getResourceReferences();
    Assertions.assertEquals(12, resourceReferences.size());
    MatcherAssert.assertThat(resourceReferences.stream()
                                               .map(ResourceReference::getResourceId)
                                               .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder("1",
                                                         "2",
                                                         "3",
                                                         "4",
                                                         "5",
                                                         "6",
                                                         "7",
                                                         "8",
                                                         "9",
                                                         "10",
                                                         "11",
                                                         "12"));
  }

  /**
   * verifies that the extraction from group members works as expected
   */
  @Test
  public void testExtractComplexResourceReferencesFromGroupMembers()
  {
    Group group = Group.builder()
                       .members(Arrays.asList(getMember("1", ResourceTypeNames.USER),
                                              getMember("2", ResourceTypeNames.USER),
                                              getMember("3", ResourceTypeNames.GROUPS),
                                              getMember("4", ResourceTypeNames.GROUPS)))
                       .build();

    ResourceType groupResourceType = resourceTypeFactory.getResourceTypeByName(ResourceTypeNames.GROUPS).get();

    ResourceReferenceExtractor referenceExtractor = ResourceReferenceExtractor.builder()
                                                                              .resource(group)
                                                                              .resourceType(groupResourceType)
                                                                              .resourceTypeFactory(resourceTypeFactory)
                                                                              .build();
    List<ResourceReference> resourceReferences = referenceExtractor.getResourceReferences();
    Assertions.assertEquals(4, resourceReferences.size());
    MatcherAssert.assertThat(resourceReferences.stream()
                                               .map(ResourceReference::getResourceId)
                                               .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder("1", "2", "3", "4"));
  }

  /**
   * verifies that an unresolvable complex references that could not be matched to a resource type will not be
   * returned
   */
  @Test
  public void testUnresolvableResourceReference()
  {
    Group group = Group.builder()
                       .members(Arrays.asList(getMember("1", null),
                                              getMember("2", null),
                                              getMember("3", null),
                                              getMember("4", null)))
                       .build();

    ResourceType groupResourceType = resourceTypeFactory.getResourceTypeByName(ResourceTypeNames.GROUPS).get();

    ResourceReferenceExtractor referenceExtractor = ResourceReferenceExtractor.builder()
                                                                              .resource(group)
                                                                              .resourceType(groupResourceType)
                                                                              .resourceTypeFactory(resourceTypeFactory)
                                                                              .build();
    List<ResourceReference> resourceReferences = referenceExtractor.getResourceReferences();
    Assertions.assertEquals(0, resourceReferences.size());
  }

  /**
   * verifies that the extraction from group members works as expected
   */
  @Test
  public void testExtractComplexResourceReferencesFromEnterpriseUserManager()
  {
    final String id = "1";
    final String ref = String.format("https://localhost:8443/scim/v2%s/%s", EndpointPaths.USERS, id);
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().manager(Manager.builder().ref(ref).build()).build();
    User user = User.builder().enterpriseUser(enterpriseUser).build();

    ResourceType userResourceType = resourceTypeFactory.getResourceTypeByName(ResourceTypeNames.USER).get();

    ResourceReferenceExtractor referenceExtractor = ResourceReferenceExtractor.builder()
                                                                              .resource(user)
                                                                              .resourceType(userResourceType)
                                                                              .resourceTypeFactory(resourceTypeFactory)
                                                                              .build();
    List<ResourceReference> resourceReferences = referenceExtractor.getResourceReferences();
    Assertions.assertEquals(1, resourceReferences.size());
    MatcherAssert.assertThat(resourceReferences.stream()
                                               .map(ResourceReference::getResourceId)
                                               .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder("1"));
  }

  public de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member getMember(String value, String type)
  {
    return de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member.builder().value(value).type(type).build();
  }
}
