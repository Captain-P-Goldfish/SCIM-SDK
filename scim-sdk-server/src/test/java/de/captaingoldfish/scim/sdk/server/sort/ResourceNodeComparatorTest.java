package de.captaingoldfish.scim.sdk.server.sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 05.11.2019 - 16:39 <br>
 * <br>
 */
public class ResourceNodeComparatorTest implements FileReferences
{

  @TestFactory
  public List<DynamicTest> testResourceNodeComparatorTest()
  {
    JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(userSchemaNode, "userName", null, null, null, null, null, null, true, null);
    Schema userSchema = new Schema(userSchemaNode, null);

    User user1 = User.builder().build();
    User user2 = User.builder().build();
    List<ResourceNode> resourceList = Arrays.asList(user1, user2);

    SchemaAttribute resourceAttribute = userSchema.getSchemaAttribute("username");
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    user1,
                                                    user2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    user1,
                                                    user2));

    user1 = User.builder().build();
    user2 = User.builder().userName("bbc").build();
    resourceList = Arrays.asList(user1, user2);
    resourceAttribute = userSchema.getSchemaAttribute("username");
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    user2,
                                                    user1));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    user2,
                                                    user1));

    user1 = User.builder().userName("bbc").build();
    user2 = User.builder().build();
    resourceList = Arrays.asList(user1, user2);
    resourceAttribute = userSchema.getSchemaAttribute("username");
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    user1,
                                                    user2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    user1,
                                                    user2));

    user1 = User.builder().userName("abc").build();
    user2 = User.builder().userName("bbc").build();
    resourceList = Arrays.asList(user1, user2);
    resourceAttribute = userSchema.getSchemaAttribute("username");
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    user1,
                                                    user2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    user2,
                                                    user1));

    user1 = User.builder().userName("abc").build();
    user2 = User.builder().userName("Abc").build();
    resourceList = Arrays.asList(user1, user2);
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    user2,
                                                    user1));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    user1,
                                                    user2));
    user1 = User.builder().userName("abc").build();
    user2 = User.builder().userName("abc").build();
    resourceList = Arrays.asList(user1, user2);
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    user1,
                                                    user2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    user1,
                                                    user2));
    user1 = User.builder().userName("Abc").build();
    user2 = User.builder().userName("abc").build();
    resourceList = Arrays.asList(user1, user2);
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    user1,
                                                    user2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    user2,
                                                    user1));

    JsonNode allTypesSchemaNode = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    Schema allTypesSchema = new Schema(allTypesSchemaNode);
    AllTypes allTypes1 = new AllTypes(true);
    allTypes1.setNumber(1L);
    AllTypes allTypes2 = new AllTypes(true);
    allTypes2.setNumber(2L);
    resourceList = Arrays.asList(allTypes1, allTypes2);
    resourceAttribute = allTypesSchema.getSchemaAttribute("number");
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    allTypes2,
                                                    allTypes1));

    allTypes1.setDecimal(1.1);
    allTypes2.setDecimal(1.2);
    resourceAttribute = allTypesSchema.getSchemaAttribute("decimal");
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    allTypes2,
                                                    allTypes1));

    allTypes1 = new AllTypes(true);
    allTypes2 = new AllTypes(true);
    resourceList = Arrays.asList(allTypes1, allTypes2);
    allTypes1.setDecimal(1.0);
    allTypes2.setDecimal(1.0);
    resourceAttribute = allTypesSchema.getSchemaAttribute("decimal");
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));

    allTypes1.setDate(LocalDateTime.now().toString());
    allTypes2.setDate(LocalDateTime.now().plusMinutes(1).toString());
    resourceAttribute = allTypesSchema.getSchemaAttribute("date");
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    allTypes2,
                                                    allTypes1));
    allTypes1.setString("abc");
    allTypes2.setString("abc");
    resourceAttribute = allTypesSchema.getSchemaAttribute("string");
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));
    allTypes1.setString("abc");
    allTypes2.setString("Abc");
    resourceAttribute = allTypesSchema.getSchemaAttribute("string");
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));
    allTypes1.setString("Abc");
    allTypes2.setString("abc");
    resourceAttribute = allTypesSchema.getSchemaAttribute("string");
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.ASCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));
    dynamicTests.add(getDynamicStringComparisonTest(resourceAttribute,
                                                    SortOrder.DESCENDING,
                                                    resourceList,
                                                    allTypes1,
                                                    allTypes2));
    return dynamicTests;
  }

  private DynamicTest getDynamicStringComparisonTest(SchemaAttribute schemaAttribute,
                                                     SortOrder sortOrder,
                                                     List<ResourceNode> resources,
                                                     ResourceNode... expectedOrder)
  {
    String testName = schemaAttribute.getScimNodeName() + " " + sortOrder;
    return DynamicTest.dynamicTest(testName, () -> {
      ResourceNodeComparator comparator = new ResourceNodeComparator(schemaAttribute, sortOrder);
      List<ResourceNode> sortedResources = resources.stream().sorted(comparator).collect(Collectors.toList());
      MatcherAssert.assertThat(sortedResources, Matchers.contains(expectedOrder));
    });
  }
}
