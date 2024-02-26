package de.captaingoldfish.scim.sdk.server.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;


/**
 * author Pascal Knueppel <br>
 * created at: 13.10.2019 - 15:43 <br>
 * <br>
 */
public class RequestUtilsTest
{

  @ParameterizedTest
  @ValueSource(strings = {"p1=v1", "p1=v1&p2=v2", "&p1=v1&&p2=v2&", "p1=&p2=", "p1&p2", "=v1&=v2"})
  public void testGetQueryParameters(String query)
  {
    Assertions.assertDoesNotThrow(() -> RequestUtils.getQueryParameters(query));
  }

  @Nested
  class TestsWithResourceType
  {

    private ResourceType userResourceType;

    @BeforeEach
    public void beforeEach()
    {
      ResourceTypeFactory resourceTypeFactory = new ResourceTypeFactory();
      UserEndpointDefinition userEndpoint = new UserEndpointDefinition(new UserHandlerImpl(true));
      userResourceType = resourceTypeFactory.registerResourceType(null,
                                                                  userEndpoint.getResourceType(),
                                                                  userEndpoint.getResourceSchema(),
                                                                  userEndpoint.getResourceSchemaExtensions()
                                                                              .toArray(new JsonNode[0]));
    }

    @DisplayName("All user attributes can be added to 'attributes'/'excludedAttributes'-paramater")
    @TestFactory
    public List<DynamicNode> testGetAttributesListWorksAsExpected()
    {
      List<DynamicNode> dynamicNodeList = new ArrayList<>();

      Map<SchemaAttribute, List<DynamicTest>> testGroups = new HashMap<>();


      for ( SchemaAttribute schemaAttribute : userResourceType.getAttributeRegister().values() )
      {
        SchemaAttribute testMappingAttribute = Optional.ofNullable(schemaAttribute.getParent()).orElse(schemaAttribute);
        List<DynamicTest> dynamicTests = testGroups.computeIfAbsent(testMappingAttribute, k -> new ArrayList<>());
        dynamicTests.add(DynamicTest.dynamicTest(schemaAttribute.getScimNodeName(), () -> {
          Assertions.assertDoesNotThrow(() -> RequestUtils.getAttributes(userResourceType,
                                                                         schemaAttribute.getScimNodeName()));
        }));
      }

      // now add the dynamic test-structure
      for ( SchemaAttribute schemaAttribute : testGroups.keySet() )
      {
        SchemaAttribute testMappingAttribute = Optional.ofNullable(schemaAttribute.getParent()).orElse(schemaAttribute);
        List<DynamicTest> attributeTests = testGroups.get(testMappingAttribute);
        if (attributeTests.size() == 1)
        {
          dynamicNodeList.add(attributeTests.get(0));
        }
        else
        {
          DynamicContainer complexContainer = DynamicContainer.dynamicContainer(schemaAttribute.getScimNodeName(),
                                                                                attributeTests);
          dynamicNodeList.add(complexContainer);
        }
      }
      return dynamicNodeList;
    }
  }

}
