package de.captaingoldfish.scim.sdk.server.patch.msazure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;


public class MsAzurePatchAttributeRebuilderTest
{

  private ResourceType userResourceType;

  @BeforeEach
  public void initialize()
  {
    ResourceTypeFactory resourceTypeFactory = new ResourceTypeFactory();
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    this.userResourceType = resourceTypeFactory.registerResourceType(null,
                                                                     userResourceTypeNode,
                                                                     userSchema,
                                                                     enterpriseUserSchema);
  }

  /**
   * verifies that the values is correctly handled in simple cases
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixValues(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>(Arrays.asList("{ \"name.givenName\": \"captain\", \"name.familyName\": \"goldfish\" }"));
    final List<String> expectedValues = new ArrayList<>(Arrays.asList("{\"name\":{\"givenName\":\"captain\",\"familyName\":\"goldfish\"}}"));

    MsAzurePatchAttributeRebuilder msAzurePatchAttributeRebuilder = new MsAzurePatchAttributeRebuilder(patchOp, values,
                                                                                                       userResourceType);
    List<String> valuesResult = msAzurePatchAttributeRebuilder.fixValues();

    Assertions.assertIterableEquals(expectedValues, valuesResult);
  }


  /**
   * verifies that the original values are returned on REMOVE and ADD operations
   */
  @Test
  public void testFixValuesWithIllegalPatchOp()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final List<String> values = new ArrayList<>(Arrays.asList("{ \"name.givenName\": \"captain\", \"name.familyName\": \"goldfish\" }"));

    MsAzurePatchAttributeRebuilder msAzurePatchAttributeRebuilder = new MsAzurePatchAttributeRebuilder(patchOp, values,
                                                                                                       userResourceType);
    List<String> valuesResult = msAzurePatchAttributeRebuilder.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned if the values operand is empty
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixValuesWithValuesListEmpty(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>();

    MsAzurePatchAttributeRebuilder msAzurePatchAttributeRebuilder = new MsAzurePatchAttributeRebuilder(patchOp, values,
                                                                                                       userResourceType);
    List<String> valuesResult = msAzurePatchAttributeRebuilder.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned if the values operand has more than one item
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixValuesWithValuesListMoreThanOneItem(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>(Arrays.asList("", ""));

    MsAzurePatchAttributeRebuilder msAzurePatchAttributeRebuilder = new MsAzurePatchAttributeRebuilder(patchOp, values,
                                                                                                       userResourceType);
    List<String> valuesResult = msAzurePatchAttributeRebuilder.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned if the values operand is not valid json
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixValuesWithValuesNotValidJson(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>(Arrays.asList("{ not valid"));

    MsAzurePatchAttributeRebuilder msAzurePatchAttributeRebuilder = new MsAzurePatchAttributeRebuilder(patchOp, values,
                                                                                                       userResourceType);
    List<String> valuesResult = msAzurePatchAttributeRebuilder.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned if the values operand is not a JSON object
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixValuesWithValuesNotJsonObject(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>(Arrays.asList("[ \"array\", \"not\", \"object\" ]"));

    MsAzurePatchAttributeRebuilder msAzurePatchAttributeRebuilder = new MsAzurePatchAttributeRebuilder(patchOp, values,
                                                                                                       userResourceType);
    List<String> valuesResult = msAzurePatchAttributeRebuilder.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned if fieldName contains multiple dots (sub-sub-attribute)
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixValuesWithFieldNameWithMoreThanOneDot(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>(Arrays.asList("{ \"first.second.third\": \"value\" }"));

    MsAzurePatchAttributeRebuilder msAzurePatchAttributeRebuilder = new MsAzurePatchAttributeRebuilder(patchOp, values,
                                                                                                       userResourceType);
    List<String> valuesResult = msAzurePatchAttributeRebuilder.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the original values is returned in simple extension cases
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixValuesWithExtensionWithoutWorkaround(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>(Arrays.asList("{ \"schemas\": [ \"urn:ietf:params:scim:schemas:core:2.0:User\", \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\" ], \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\": { \"costCenter\": \"1234\" } }"));


    MsAzurePatchAttributeRebuilder msAzurePatchAttributeRebuilder = new MsAzurePatchAttributeRebuilder(patchOp, values,
                                                                                                       userResourceType);
    List<String> valuesResult = msAzurePatchAttributeRebuilder.fixValues();

    Assertions.assertEquals(values, valuesResult);
  }

  /**
   * verifies that the workaround is applied with extension
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixValuesWithExtensionWithWorkaroundApplied(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>(Arrays.asList("{ \"schemas\": [ \"urn:ietf:params:scim:schemas:core:2.0:User\", \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\" ], \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\": { \"manager.displayName\": \"captain\" } }"));
    final List<String> expectedValues = new ArrayList<>(Arrays.asList("{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:User\",\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\"],\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\":{\"manager\":{\"displayName\":\"captain\"}}}"));


    MsAzurePatchAttributeRebuilder msAzurePatchAttributeRebuilder = new MsAzurePatchAttributeRebuilder(patchOp, values,
                                                                                                       userResourceType);
    List<String> valuesResult = msAzurePatchAttributeRebuilder.fixValues();

    Assertions.assertIterableEquals(expectedValues, valuesResult);
  }


  /**
   * verifies that the workaround is applied on direct resource and fully qualified enterprise user
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixValuesWithUserAndEnterpriseUser(PatchOp patchOp)
  {
    final List<String> values = new ArrayList<>(Arrays.asList("{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:User\",\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\"],\"name.givenName\":\"goldfish\",\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\":{\"costCenter\":\"1234\"}}"));
    final List<String> expectedValues = new ArrayList<>(Arrays.asList("{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:User\",\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\"],\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\":{\"costCenter\":\"1234\"},\"name\":{\"givenName\":\"goldfish\"}}"));

    MsAzurePatchAttributeRebuilder msAzurePatchAttributeRebuilder = new MsAzurePatchAttributeRebuilder(patchOp, values,
                                                                                                       userResourceType);
    List<String> valuesResult = msAzurePatchAttributeRebuilder.fixValues();

    Assertions.assertIterableEquals(expectedValues, valuesResult);
  }


}

