package de.captaingoldfish.scim.sdk.server.endpoints.bulkget;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.server.custom.endpoints.BulkIdReferencesEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpointHandlerUtil;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;


/**
 * @author Pascal Knueppel
 * @since 05.09.2022
 */
public class BulkResourceReferenceSimpleTest
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
   * verifies that the correct resource type (User) is extracted from the resource reference
   */
  @Test
  public void getTestReferenceReferencedUserNode()
  {
    final String nodePath = "userId";
    final String resourceId = UUID.randomUUID().toString();
    JsonNode node = new TextNode(resourceId);

    BulkResourceReferenceSimple resourceReference = new BulkResourceReferenceSimple(resourceTypeFactory,
                                                                                    parentResourceType, nodePath, node);
    Assertions.assertTrue(resourceReference.isResourceRetrievable());
    Assertions.assertEquals(resourceId, resourceReference.getResourceId());
    Assertions.assertEquals(nodePath, resourceReference.getNodePath());
    Assertions.assertEquals(ResourceTypeNames.USER, resourceReference.getResourceType().getName());
  }

  /**
   * verifies that the correct resource type (Group) is extracted from the resource reference
   */
  @Test
  public void getTestReferenceUserReferencedGroupNode()
  {
    final String nodePath = "memberList.groupId";
    final String resourceId = UUID.randomUUID().toString();
    JsonNode node = new TextNode(resourceId);

    BulkResourceReferenceSimple resourceReference = new BulkResourceReferenceSimple(resourceTypeFactory,
                                                                                    parentResourceType, nodePath, node);
    Assertions.assertTrue(resourceReference.isResourceRetrievable());
    Assertions.assertEquals(resourceId, resourceReference.getResourceId());
    Assertions.assertEquals(nodePath, resourceReference.getNodePath());
    Assertions.assertEquals(ResourceTypeNames.GROUPS, resourceReference.getResourceType().getName());
  }
}
