package de.captaingoldfish.scim.sdk.server.endpoints.bulkget;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.BulkConfig;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpointHandlerUtil;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;


/**
 * @author Pascal Knueppel
 * @since 30.08.2022
 */
public class BulkResourceReferenceComplexTest
{

  /**
   * the service provider configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * necessary to get access to the resource types
   */
  private ResourceTypeFactory resourceTypeFactory;

  @BeforeEach
  public void initialize()
  {
    serviceProvider = ServiceProvider.builder().bulkConfig(BulkConfig.builder().supported(true).build()).build();
    this.resourceTypeFactory = new ResourceTypeFactory();
    ResourceEndpointHandlerUtil.registerAllEndpoints(resourceTypeFactory, serviceProvider);
  }

  /**
   * verifies that the data of the referenced resource is correctly extracted if the data is added in its best
   * possible form
   */
  @Test
  public void testResolveComplexNode()
  {
    final String id = UUID.randomUUID().toString();
    final String ref = String.format("https://localhost:8443/scim/v2/Users/%s", id);
    final String type = ResourceTypeNames.USER;
    final String nodePath = "members";

    ObjectNode complexNode = getComplexNode(id, ref, type);
    BulkResourceReferenceComplex resourceReference = new BulkResourceReferenceComplex(resourceTypeFactory, nodePath,
                                                                                      complexNode);
    Assertions.assertTrue(resourceReference.isResourceRetrievable());
    Assertions.assertEquals(nodePath, resourceReference.getNodePath());
    Assertions.assertEquals(id, resourceReference.getResourceId());
    Assertions.assertEquals(type, resourceReference.getResourceType().getName());
  }

  /**
   * verifies that the data of the referenced resource is correctly extracted if the resource id is missing
   * within the value attribute
   */
  @Test
  public void testResolveComplexNodeWithIdValueNull()
  {
    final String id = UUID.randomUUID().toString();
    final String ref = String.format("https://localhost:8443/scim/v2/Users/%s", id);
    final String type = ResourceTypeNames.USER;
    final String nodePath = "members";

    ObjectNode complexNode = getComplexNode(null, ref, type);
    BulkResourceReferenceComplex resourceReference = new BulkResourceReferenceComplex(resourceTypeFactory, nodePath,
                                                                                      complexNode);
    Assertions.assertTrue(resourceReference.isResourceRetrievable());
    Assertions.assertEquals(nodePath, resourceReference.getNodePath());
    Assertions.assertEquals(id, resourceReference.getResourceId());
    Assertions.assertEquals(type, resourceReference.getResourceType().getName());
  }

  /**
   * verifies that the data of the referenced resource is correctly extracted if the resource data is only
   * present within the $ref-attribute
   */
  @Test
  public void testResolveComplexNodeWithOnlyRefAttributePresent()
  {
    final String id = UUID.randomUUID().toString();
    final String ref = String.format("https://localhost:8443/scim/v2/Users/%s", id);
    final String nodePath = "members";

    ObjectNode complexNode = getComplexNode(null, ref, null);
    BulkResourceReferenceComplex resourceReference = new BulkResourceReferenceComplex(resourceTypeFactory, nodePath,
                                                                                      complexNode);
    Assertions.assertTrue(resourceReference.isResourceRetrievable());
    Assertions.assertEquals(nodePath, resourceReference.getNodePath());
    Assertions.assertEquals(id, resourceReference.getResourceId());
    Assertions.assertEquals(ResourceTypeNames.USER, resourceReference.getResourceType().getName());
  }

  /**
   * verifies that the data is marked as not retrievable if not enough data is present to retrieve the resources
   */
  @Test
  public void testResolveComplexNodeWithOnlyIdValuePresent()
  {
    final String id = UUID.randomUUID().toString();
    final String nodePath = "members";

    ObjectNode complexNode = getComplexNode(id, null, null);
    BulkResourceReferenceComplex resourceReference = new BulkResourceReferenceComplex(resourceTypeFactory, nodePath,
                                                                                      complexNode);
    Assertions.assertFalse(resourceReference.isResourceRetrievable());
    Assertions.assertEquals(nodePath, resourceReference.getNodePath());
  }

  /**
   * verifies that the data is marked as not retrievable if only the type value is present
   */
  @Test
  public void testResolveComplexNodeWithOnlyTypeValuePresent()
  {
    final String type = ResourceTypeNames.USER;
    final String nodePath = "members";

    ObjectNode complexNode = getComplexNode(null, null, type);
    BulkResourceReferenceComplex resourceReference = new BulkResourceReferenceComplex(resourceTypeFactory, nodePath,
                                                                                      complexNode);
    Assertions.assertFalse(resourceReference.isResourceRetrievable());
    Assertions.assertEquals(nodePath, resourceReference.getNodePath());
  }

  /**
   * verifies that the data is marked as not retrievable if the $ref-attribute contains a malformed url
   */
  @Test
  public void testResolveComplexNodeWithMalformedUrl()
  {
    final String type = ResourceTypeNames.USER;
    final String ref = "https://localhost:unknownPort/scim/v2/Users";
    final String nodePath = "members";

    ObjectNode complexNode = getComplexNode(null, ref, type);
    BulkResourceReferenceComplex resourceReference = new BulkResourceReferenceComplex(resourceTypeFactory, nodePath,
                                                                                      complexNode);
    Assertions.assertFalse(resourceReference.isResourceRetrievable());
    Assertions.assertEquals(nodePath, resourceReference.getNodePath());
  }


  private ObjectNode getComplexNode(String id, String ref, String type)
  {
    ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
    objectNode.set(AttributeNames.RFC7643.VALUE, new TextNode(id));
    objectNode.set(AttributeNames.RFC7643.REF, new TextNode(ref));
    objectNode.set(AttributeNames.RFC7643.TYPE, new TextNode(type));
    return objectNode.isEmpty() ? null : objectNode;
  }
}
