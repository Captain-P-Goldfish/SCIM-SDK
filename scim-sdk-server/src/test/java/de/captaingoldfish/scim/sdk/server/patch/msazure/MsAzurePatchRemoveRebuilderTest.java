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
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.patch.workarounds.msazure.MsAzurePatchRemoveRebuilder;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;


/**
 * @author Pascal Knueppel
 * @since 07.06.2021
 */
public class MsAzurePatchRemoveRebuilderTest implements FileReferences
{

  /**
   * the patchConfig to activate or deactivate the workaround
   */
  private PatchConfig patchConfig;

  /**
   * needed to extract the {@link ResourceType}s which are necessary to check if the given attribute-names are
   * valid or not
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the resource type for all types definition. Contains data types of any possible scim representation
   */
  private ResourceType allTypesResourceType;

  @BeforeEach
  public void initPatchConfig()
  {
    patchConfig = PatchConfig.builder().supported(true).build();
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
   * verifies that the path is correctly handled in simple cases
   */
  @Test
  public void testFixPath()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": \"123456\"}"));

    final String expectedPath = "members[value eq \"123456\"]";

    PatchRequestOperation operation = PatchRequestOperation.builder().op(patchOp).path(path).values(values).build();
    MsAzurePatchRemoveRebuilder msAzurePatchRemoveRebuilder = new MsAzurePatchRemoveRebuilder();
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.shouldBeHandled(patchConfig, allTypesResourceType, operation));
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.executeOtherHandlers());
    PatchRequestOperation fixedOperation = msAzurePatchRemoveRebuilder.fixPatchRequestOperaton(null, operation);

    Assertions.assertEquals(PatchOp.REMOVE, fixedOperation.getOp());
    Assertions.assertEquals(expectedPath, fixedOperation.getPath().get());
    Assertions.assertTrue(fixedOperation.getValues().isEmpty());
  }

  /**
   * verifies that the path is correctly handled if several value objects are present
   */
  @Test
  public void testFixPathWithSeveralValueObjects()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": \"123456\"}", "{\"value\": \"654321\"}"));

    final String expectedPath = "members[value eq \"123456\" or value eq \"654321\"]";

    PatchRequestOperation operation = PatchRequestOperation.builder().op(patchOp).path(path).values(values).build();
    MsAzurePatchRemoveRebuilder msAzurePatchRemoveRebuilder = new MsAzurePatchRemoveRebuilder();
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.shouldBeHandled(patchConfig, allTypesResourceType, operation));
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.executeOtherHandlers());
    PatchRequestOperation fixedOperation = msAzurePatchRemoveRebuilder.fixPatchRequestOperaton(null, operation);

    Assertions.assertEquals(patchOp, fixedOperation.getOp());
    Assertions.assertEquals(expectedPath, fixedOperation.getPath().get());
    Assertions.assertTrue(fixedOperation.getValues().isEmpty());
  }

  /**
   * verifies that the workaround does not change the path on REPLACE and ADD operations
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testFixPathWithIllegalPatchOp(PatchOp patchOp)
  {
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": \"123456\"}"));

    PatchRequestOperation operation = PatchRequestOperation.builder().op(patchOp).path(path).values(values).build();
    MsAzurePatchRemoveRebuilder msAzurePatchRemoveRebuilder = new MsAzurePatchRemoveRebuilder();
    Assertions.assertFalse(msAzurePatchRemoveRebuilder.shouldBeHandled(patchConfig, allTypesResourceType, operation));
  }

  /**
   * verifies that the original path is returned if the values operand is empty
   */
  @Test
  public void testFixPathWithValuesListEmpty()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>();

    PatchRequestOperation operation = PatchRequestOperation.builder().op(patchOp).path(path).values(values).build();
    MsAzurePatchRemoveRebuilder msAzurePatchRemoveRebuilder = new MsAzurePatchRemoveRebuilder();
    Assertions.assertFalse(msAzurePatchRemoveRebuilder.shouldBeHandled(patchConfig, allTypesResourceType, operation));
  }

  /**
   * verifies that the original path is returned if a nested object was used instead of a simple object
   */
  @Test
  public void testFixPathWithNestedObject()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": {\"value\": \"123456\"}}"));

    PatchRequestOperation operation = PatchRequestOperation.builder().op(patchOp).path(path).values(values).build();
    MsAzurePatchRemoveRebuilder msAzurePatchRemoveRebuilder = new MsAzurePatchRemoveRebuilder();
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.shouldBeHandled(patchConfig, allTypesResourceType, operation));
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.executeOtherHandlers());
    PatchRequestOperation fixedOperation = msAzurePatchRemoveRebuilder.fixPatchRequestOperaton(null, operation);

    Assertions.assertEquals(PatchOp.REMOVE, fixedOperation.getOp());
    Assertions.assertEquals(path, fixedOperation.getPath().get());
    Assertions.assertEquals(values, fixedOperation.getValues());
  }

  /**
   * verifies that the original path is returned if the value has an array instead of a simple value
   */
  @Test
  public void testFixPathWithArrayValue()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": [\"123456\"]}"));

    PatchRequestOperation operation = PatchRequestOperation.builder().op(patchOp).path(path).values(values).build();
    MsAzurePatchRemoveRebuilder msAzurePatchRemoveRebuilder = new MsAzurePatchRemoveRebuilder();
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.shouldBeHandled(patchConfig, allTypesResourceType, operation));
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.executeOtherHandlers());
    PatchRequestOperation fixedOperation = msAzurePatchRemoveRebuilder.fixPatchRequestOperaton(null, operation);

    Assertions.assertEquals(PatchOp.REMOVE, fixedOperation.getOp());
    Assertions.assertEquals(path, fixedOperation.getPath().get());
    Assertions.assertEquals(values, fixedOperation.getValues());
  }

  /**
   * verifies that the original path is returned if the value object has several attributes
   */
  @Test
  public void testFixPathWithSeveralAttributesInValueObject()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("{\"value\": \"123456\", \"display\": \"hello\"}"));

    PatchRequestOperation operation = PatchRequestOperation.builder().op(patchOp).path(path).values(values).build();
    MsAzurePatchRemoveRebuilder msAzurePatchRemoveRebuilder = new MsAzurePatchRemoveRebuilder();
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.shouldBeHandled(patchConfig, allTypesResourceType, operation));
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.executeOtherHandlers());
    PatchRequestOperation fixedOperation = msAzurePatchRemoveRebuilder.fixPatchRequestOperaton(null, operation);

    Assertions.assertEquals(PatchOp.REMOVE, fixedOperation.getOp());
    Assertions.assertEquals(path, fixedOperation.getPath().get());
    Assertions.assertEquals(values, fixedOperation.getValues());
  }

  /**
   * verifies that the original path is returned if the value object has several attributes
   */
  @Test
  public void testFixPathWithNonObjectOnValueArray()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("[\"123456\"]"));

    PatchRequestOperation operation = PatchRequestOperation.builder().op(patchOp).path(path).values(values).build();
    MsAzurePatchRemoveRebuilder msAzurePatchRemoveRebuilder = new MsAzurePatchRemoveRebuilder();
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.shouldBeHandled(patchConfig, allTypesResourceType, operation));
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.executeOtherHandlers());
    PatchRequestOperation fixedOperation = msAzurePatchRemoveRebuilder.fixPatchRequestOperaton(null, operation);

    Assertions.assertEquals(PatchOp.REMOVE, fixedOperation.getOp());
    Assertions.assertEquals(path, fixedOperation.getPath().get());
    Assertions.assertEquals(values, fixedOperation.getValues());
  }

  /**
   * verifies that the original path is returned if the value object has several attributes
   */
  @Test
  public void testFixPathWithNonJsonValue()
  {
    final PatchOp patchOp = PatchOp.REMOVE;
    final String path = "members";
    final List<String> values = new ArrayList<>(Arrays.asList("123456"));

    PatchRequestOperation operation = PatchRequestOperation.builder().op(patchOp).path(path).values(values).build();
    MsAzurePatchRemoveRebuilder msAzurePatchRemoveRebuilder = new MsAzurePatchRemoveRebuilder();
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.shouldBeHandled(patchConfig, allTypesResourceType, operation));
    Assertions.assertTrue(msAzurePatchRemoveRebuilder.executeOtherHandlers());
    PatchRequestOperation fixedOperation = msAzurePatchRemoveRebuilder.fixPatchRequestOperaton(null, operation);

    Assertions.assertEquals(PatchOp.REMOVE, fixedOperation.getOp());
    Assertions.assertEquals(path, fixedOperation.getPath().get());
    Assertions.assertEquals(values, fixedOperation.getValues());
  }
}
