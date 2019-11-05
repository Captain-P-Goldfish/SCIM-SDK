package de.gold.scim.common.resources;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.common.constants.ClassPathReferences;
import de.gold.scim.common.resources.complex.Meta;
import de.gold.scim.common.schemas.Schema;
import de.gold.scim.common.schemas.SchemaAttribute;
import de.gold.scim.common.utils.FileReferences;
import de.gold.scim.common.utils.JsonHelper;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 00:00 <br>
 * <br>
 */
public class ResourceNodeTest implements FileReferences
{

  /**
   * will verify that the attributes of a {@link ResourceNode} are correctly set and read
   */
  @Test
  public void testSetAndGetValues()
  {
    final String id = UUID.randomUUID().toString();
    final String externalId = UUID.randomUUID().toString();
    final Meta meta = buildMetaObject();
    TestResource testResource = new TestResource();
    testResource.setId(id);
    testResource.setExternalId(externalId);
    testResource.setMeta(meta);

    Assertions.assertEquals(id, testResource.getId().get());
    Assertions.assertEquals(externalId, testResource.getExternalId().get());
    Assertions.assertEquals(meta, testResource.getMeta().get());
  }

  /**
   * verifies that the sortBy attribute is correctly returned from a {@link ResourceNode}
   */
  @TestFactory
  public List<DynamicTest> testGetSortByAttribute()
  {
    JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    Schema userSchema = new Schema(userSchemaNode, null);
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);


    List<DynamicTest> dynamicTestList = new ArrayList<>();
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "username", "chuck"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "nickname", "chucky"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "title", "Mr."));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "usertype", "super user"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "name.familyname", "Norris"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "name.givenname", "Carlos"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "name.middlename", "Ray"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "addresses.locality", "Bremen"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "addresses.streetAddress", "somewhere 56"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "addresses.country", "DE"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "phoneNumbers.type", "home"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "phoneNumbers.value", "666-666-666666"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "phoneNumbers.primary", "true"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "emails.value", "chuck@norris.com"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "emails.type", "work"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "emails.primary", "true"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "groups.value", "123456"));
    dynamicTestList.add(getSortByAttributeTest(userSchema, user, "roles.value", "123456"));
    return dynamicTestList;
  }

  private DynamicTest getSortByAttributeTest(Schema userSchema, User user, String attributeName, String expectedValue)
  {
    return DynamicTest.dynamicTest(attributeName + " eq " + expectedValue, () -> {
      SchemaAttribute schemaAttribute = userSchema.getSchemaAttribute(attributeName);
      Optional<JsonNode> sortByAttributeOptional = user.getSortingAttribute(schemaAttribute);
      Assertions.assertTrue(sortByAttributeOptional.isPresent());
      Assertions.assertEquals(expectedValue, sortByAttributeOptional.get().asText());
    });
  }

  /**
   * @return a simple meta object
   */
  private Meta buildMetaObject()
  {
    final LocalDateTime created = LocalDateTime.now().withNano(0);
    final LocalDateTime lastModified = LocalDateTime.now().withNano(0);
    final String resourceType = "User";
    final String location = "/Users/" + UUID.randomUUID().toString();
    final String version = "1";
    return Meta.builder()
               .created(created)
               .lastModified(lastModified)
               .resourceType(resourceType)
               .location(location)
               .version(version)
               .build();
  }

  /**
   * a test implementation of {@link ResourceNode} to have an object to test the methods in the abstract class
   */
  public static class TestResource extends ResourceNode
  {

  }
}
