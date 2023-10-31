package de.captaingoldfish.scim.sdk.server.patch.msazure;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames.RFC7643;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 31.10.2023
 */
@Slf4j
public class MsAzurePatchComplexValueRebuilderTest implements FileReferences
{

  /**
   * needed to extract the {@link ResourceType}s which are necessary to check if the given attribute-names are
   * valid or not
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the resource type for all types definition. Contains data types of any possible scim representation
   */
  private ResourceType allTypesResourceType;

  /**
   * initializes a new {@link ResourceTypeFactory} for the following tests
   */
  @BeforeEach
  public void initialize()
  {
    this.resourceTypeFactory = new ResourceTypeFactory();
    JsonNode allTypesResourceType = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    this.allTypesResourceType = resourceTypeFactory.registerResourceType(null,
                                                                         allTypesResourceType,
                                                                         allTypesSchema,
                                                                         enterpriseUserSchema);
  }

  /**
   * verifies that a patch request with an illegal simple-value attribute is correctly wrapped into an object
   */
  @DisplayName("Fix simple attribute-value on complex-path expression")
  @Test
  public void testValuesAreCorrectlyFixed()
  {
    SchemaAttribute manager = allTypesResourceType.getAllSchemaExtensions().get(0).getSchemaAttribute(RFC7643.MANAGER);
    Assertions.assertNotNull(manager);
    Assertions.assertEquals(Type.COMPLEX, manager.getType());
    List<String> patchValues = Arrays.asList("271", "281");

    MsAzurePatchComplexValueRebuilder workaroundHandler = new MsAzurePatchComplexValueRebuilder(manager, patchValues);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(2, fixedValues.size());
    {
      JsonNode jsonNode = JsonHelper.readJsonDocument(fixedValues.get(0));
      Assertions.assertNotNull(jsonNode);
      Assertions.assertTrue(jsonNode.isObject());
      Assertions.assertEquals("271", jsonNode.get(RFC7643.VALUE).textValue());
    }
    {
      JsonNode jsonNode = JsonHelper.readJsonDocument(fixedValues.get(1));
      Assertions.assertNotNull(jsonNode);
      Assertions.assertTrue(jsonNode.isObject());
      Assertions.assertEquals("281", jsonNode.get(RFC7643.VALUE).textValue());
    }
  }

  /**
   * verifies that the workaround is not changing anything if the attribute is not a complex attribute
   */
  @DisplayName("Ignore workaround if not a complex attribute")
  @Test
  public void testIgnoreNonComplexAttributes()
  {
    SchemaAttribute costCenter = allTypesResourceType.getAllSchemaExtensions()
                                                     .get(0)
                                                     .getSchemaAttribute(RFC7643.COST_CENTER);
    Assertions.assertNotEquals(Type.COMPLEX, costCenter.getType());
    Assertions.assertNotNull(costCenter);
    List<String> patchValues = Arrays.asList("271", "281");

    MsAzurePatchComplexValueRebuilder workaroundHandler = new MsAzurePatchComplexValueRebuilder(costCenter,
                                                                                                patchValues);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(patchValues, fixedValues);
  }

  /**
   * verifies that the workaround is not changing anything if the value of the patch-request is an object
   */
  @DisplayName("Ignore workaround if value is an object")
  @Test
  public void testIgnoreIfValueIsAnObject()
  {
    SchemaAttribute manager = allTypesResourceType.getAllSchemaExtensions().get(0).getSchemaAttribute(RFC7643.MANAGER);
    Assertions.assertNotNull(manager);
    Assertions.assertEquals(Type.COMPLEX, manager.getType());
    List<String> patchValues = Arrays.asList("{\"value\": \"271\"}", "{\"value\": \"281\"}");

    MsAzurePatchComplexValueRebuilder workaroundHandler = new MsAzurePatchComplexValueRebuilder(manager, patchValues);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(patchValues, fixedValues);
  }

  /**
   * verifies that the workaround is not changing anything if the value of the patch-request is an array
   */
  @DisplayName("Ignore workaround if value is an array")
  @Test
  public void testIgnoreIfValueIsAnArray()
  {
    SchemaAttribute manager = allTypesResourceType.getAllSchemaExtensions().get(0).getSchemaAttribute(RFC7643.MANAGER);
    Assertions.assertNotNull(manager);
    Assertions.assertEquals(Type.COMPLEX, manager.getType());
    List<String> patchValues = Arrays.asList("[271]", "[281]");

    MsAzurePatchComplexValueRebuilder workaroundHandler = new MsAzurePatchComplexValueRebuilder(manager, patchValues);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(patchValues, fixedValues);
  }

  /**
   * verifies that the workaround is not changing anything if the value of the patch-request is invalid json
   */
  @DisplayName("Ignore workaround if value is invalid json")
  @Test
  public void testIgnoreIfValueIsInvalidJson()
  {
    SchemaAttribute manager = allTypesResourceType.getAllSchemaExtensions().get(0).getSchemaAttribute(RFC7643.MANAGER);
    Assertions.assertNotNull(manager);
    Assertions.assertEquals(Type.COMPLEX, manager.getType());
    List<String> patchValues = Arrays.asList("[271", "{281]");

    MsAzurePatchComplexValueRebuilder workaroundHandler = new MsAzurePatchComplexValueRebuilder(manager, patchValues);
    List<String> fixedValues = workaroundHandler.fixValues();
    Assertions.assertEquals(patchValues, fixedValues);
  }
}
