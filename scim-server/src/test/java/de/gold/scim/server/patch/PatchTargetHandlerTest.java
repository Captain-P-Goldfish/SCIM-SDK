package de.gold.scim.server.patch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.gold.scim.common.constants.AttributeNames;
import de.gold.scim.common.constants.ClassPathReferences;
import de.gold.scim.common.constants.HttpStatus;
import de.gold.scim.common.constants.SchemaUris;
import de.gold.scim.common.constants.ScimType;
import de.gold.scim.common.constants.enums.PatchOp;
import de.gold.scim.common.exceptions.ScimException;
import de.gold.scim.common.request.PatchOpRequest;
import de.gold.scim.common.request.PatchRequestOperation;
import de.gold.scim.common.resources.EnterpriseUser;
import de.gold.scim.common.resources.base.ScimArrayNode;
import de.gold.scim.common.resources.complex.Manager;
import de.gold.scim.common.resources.multicomplex.Email;
import de.gold.scim.common.schemas.Schema;
import de.gold.scim.common.utils.JsonHelper;
import de.gold.scim.server.resources.AllTypes;
import de.gold.scim.server.schemas.ResourceType;
import de.gold.scim.server.schemas.ResourceTypeFactory;
import de.gold.scim.server.utils.FileReferences;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 30.10.2019 - 09:27 <br>
 * <br>
 */
@Slf4j
public class PatchTargetHandlerTest implements FileReferences
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
   * this test will verify that the patch operation is able to add values that are not yet present within the
   * resource
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "decimal,5.8", "bool,true", "bool,false", "date,1996-03-10T00:00:00Z"})
  public void testAddSimpleAttribute(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertNotNull(allTypes.get(attributeName));
    Assertions.assertEquals(value, allTypes.get(attributeName).asText());
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * this test will verify that the patch operation is able to add values that are not yet present within the
   * resource
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "decimal,5.8", "bool,true", "bool,false", "date,1996-03-10T00:00:00Z"})
  public void testReplaceSimpleAttribute(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertNotNull(allTypes.get(attributeName));
    Assertions.assertEquals(value, allTypes.get(attributeName).asText());
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * will verify that already existing attributes are getting replaced by the add operation
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "decimal,5.8", "bool,true", "date,1996-03-10T00:00:00Z"})
  public void testAddSimpleAttributeWithExistingValues(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertNotNull(allTypes.get(attributeName));
    Assertions.assertEquals(value, allTypes.get(attributeName).asText());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * will verify that already existing attributes are getting replaced by the replace operation
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "decimal,5.8", "bool,true", "date,1996-03-10T00:00:00Z"})
  public void testReplaceSimpleAttributeWithExistingValues(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = JsonHelper.loadJsonDocument(ALL_TYPES_JSON, AllTypes.class);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertNotNull(allTypes.get(attributeName));
    Assertions.assertEquals(value, allTypes.get(attributeName).asText());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verfies that several values can be added to simple string array types
   */
  @Test
  public void testAddSimpleStringArrayAttribute()
  {
    List<String> values = Arrays.asList("hello world", "goodbye world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(values.size(), allTypes.getStringArray().size());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verfies that a string array can be added with the replace operation
   */
  @Test
  public void testAddSimpleStringArrayAttributeWithReplaceOperation()
  {
    List<String> values = Arrays.asList("hello world", "goodbye world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(values.size(), allTypes.getStringArray().size());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verfies that a string array can be replaced with the replace operation
   */
  @Test
  public void testReplaceSimpleStringArrayAttribute()
  {
    List<String> values = Arrays.asList("hello world", "goodbye world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setStringArray(Arrays.asList("1", "2", "3", "4", "humpty dumpty"));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(values.size(), allTypes.getStringArray().size());
    MatcherAssert.assertThat(allTypes.getStringArray(), Matchers.hasItems(values.toArray(new String[0])));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verfies that several values can be added to simple number array types
   */
  @ParameterizedTest
  @ValueSource(strings = {"numberarray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberarray"})
  public void testAddSimpleNumberArrayAttribute(String attributeName)
  {
    List<String> values = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 0).map(String::valueOf).collect(Collectors.toList());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(values.size(), allTypes.getNumberArray().size());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verfies that several values can be added to simple number array types with the replace operation
   */
  @ParameterizedTest
  @ValueSource(strings = {"numberarray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberarray"})
  public void testReplaceSimpleNumberArrayAttribute(String attributeName)
  {
    List<String> values = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 0).map(String::valueOf).collect(Collectors.toList());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(values.size(), allTypes.getNumberArray().size());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verfies that several values can be added to simple number array types
   */
  @ParameterizedTest
  @ValueSource(strings = {"numberarray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberarray"})
  public void testAddSimpleNumberArrayAttributeAndPreserveOldValues(String attributeName)
  {
    List<String> values = Stream.of(9, 0).map(String::valueOf).collect(Collectors.toList());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    List<Long> numberList = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    allTypes.setNumberArray(numberList);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(10, allTypes.getNumberArray().size());
    MatcherAssert.assertThat(allTypes.getNumberArray(), Matchers.hasItems(numberList.toArray(new Long[0])));
    MatcherAssert.assertThat(allTypes.getNumberArray(), Matchers.hasItems(9L, 0L));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verfies that an array attribute can be replaced
   */
  @ParameterizedTest
  @ValueSource(strings = {"numberarray", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:numberarray"})
  public void testReplaceSimpleNumberArrayAttributeAndPreserveOldValues(String attributeName)
  {
    List<String> values = Stream.of(9, 0).map(String::valueOf).collect(Collectors.toList());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    List<Long> numberList = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    allTypes.setNumberArray(numberList);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getNumberArray().size());
    MatcherAssert.assertThat(allTypes.getNumberArray(), Matchers.hasItems(9L, 0L));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that simple attribute can successfully be added to complex types
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "number," + Long.MAX_VALUE, "decimal,5.8", "bool,true", "bool,false",
              "date," + "1996-03-10T00:00:00Z"})
  public void testAddSimpleAttributeToComplexType(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("complex." + attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(value, allTypes.getComplex().get().get(attributeName).asText());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that simple attribute can successfully be added to complex types with the replace operation
   */
  @ParameterizedTest
  @CsvSource({"string,hello world", "number,5", "number," + Long.MAX_VALUE, "decimal,5.8", "bool,true", "bool,false",
              "date," + "1996-03-10T00:00:00Z"})
  public void testAddSimpleAttributeToComplexTypeWithReplaceOperation(String attributeName, String value)
  {
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("complex." + attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getComplex().isPresent(), allTypes.toPrettyString());
    Assertions.assertEquals(value, allTypes.getComplex().get().get(attributeName).asText());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that simple attribute can successfully be added to complex types while preserving already existing
   * values
   */
  @ParameterizedTest
  @ValueSource(strings = {"complex.stringarray",
                          "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.stringarray"})
  public void testAddSimpleAttributeToComplexTypeAndPreserveOldValues(String attributeName)
  {
    List<String> values = Arrays.asList("hello world", "goodbye world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("salty");
    AllTypes complex = new AllTypes(false);
    complex.setString("sweet");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(4, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertEquals("salty", allTypes.getString().get());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertTrue(allTypes.getComplex().get().getString().isPresent());
    Assertions.assertEquals("sweet", allTypes.getComplex().get().getString().get());
    Assertions.assertEquals("happy day", allTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("hello world", allTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertEquals("goodbye world", allTypes.getComplex().get().getStringArray().get(2));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that arrays on complex types can be replaced
   */
  @ParameterizedTest
  @ValueSource(strings = {"complex.stringarray",
                          "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex.stringarray"})
  public void testReplaceArrayOnComplexType(String attributeName)
  {
    List<String> values = Arrays.asList("hello world", "goodbye world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("salty");
    AllTypes complex = new AllTypes(false);
    complex.setString("sweet");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(4, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertEquals("salty", allTypes.getString().get());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertTrue(allTypes.getComplex().get().getString().isPresent());
    Assertions.assertEquals("sweet", allTypes.getComplex().get().getString().get());
    Assertions.assertEquals(2, allTypes.getComplex().get().getStringArray().size());
    Assertions.assertEquals("hello world", allTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("goodbye world", allTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a complex type can be set as whole object
   */
  @ParameterizedTest
  @ValueSource(strings = {"complex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex"})
  public void testAddSimpleAttributeToComplexType(String attributeName)
  {

    // @formatter:off
    List<String> values = Collections.singletonList(  "{"
                                                    + "   \"number\": " + Long.MAX_VALUE + ","
                                                    + "   \"stringArray\":[\"hello world\", \"goodbye world\"]"
                                                    + "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(2, allTypes.getComplex().get().size(), allTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals(2, allTypes.getComplex().get().getStringArray().size());
    Assertions.assertTrue(allTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(Long.MAX_VALUE, allTypes.getComplex().get().getNumber().get());
    Assertions.assertEquals("hello world", allTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("goodbye world", allTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a complex type can be set as whole object with replace operation
   */
  @ParameterizedTest
  @ValueSource(strings = {"complex", "urn:gold:params:scim:schemas:custom:2.0:AllTypes:complex"})
  public void testAddSimpleAttributeToComplexTypeWithReplace(String attributeName)
  {

    // @formatter:off
    List<String> values = Collections.singletonList(  "{"
                                                    + "   \"number\": " + Long.MAX_VALUE + ","
                                                    + "   \"stringArray\":[\"hello world\", \"goodbye world\"]"
                                                    + "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(2, allTypes.getComplex().get().size(), allTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals(2, allTypes.getComplex().get().getStringArray().size());
    Assertions.assertTrue(allTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(Long.MAX_VALUE, allTypes.getComplex().get().getNumber().get());
    Assertions.assertEquals("hello world", allTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("goodbye world", allTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a complex type can be set as whole object and that existing values are kept
   */
  @Test
  public void testAddSimpleAttributeToComplexTypeWithAlreadyExistingValues()
  {

    // @formatter:off
    List<String> values = Collections.singletonList(  "{"
                                                    + "   \"number\": " + Long.MAX_VALUE + ","
                                                    + "   \"stringArray\":[\"hello world\", \"goodbye world\"]"
                                                    + "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("complex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hf");

    AllTypes complex = new AllTypes(false);
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(4, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertEquals("hf", allTypes.getString().get());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(3, allTypes.getComplex().get().size(), allTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals(3, allTypes.getComplex().get().getStringArray().size());
    Assertions.assertTrue(allTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertEquals("hf", allTypes.getString().get());
    Assertions.assertTrue(allTypes.getComplex().get().getString().isPresent());
    Assertions.assertEquals("goldfish", allTypes.getComplex().get().getString().get());
    Assertions.assertEquals(Long.MAX_VALUE, allTypes.getComplex().get().getNumber().get());
    Assertions.assertEquals("happy day", allTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("hello world", allTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertEquals("goodbye world", allTypes.getComplex().get().getStringArray().get(2));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that a complex type can completely replaced
   */
  @Test
  public void testReplaceComplexAttribute()
  {

    // @formatter:off
    List<String> values = Collections.singletonList(  "{"
                                                    + "   \"number\": " + Long.MAX_VALUE + ","
                                                    + "   \"stringArray\":[\"hello world\", \"goodbye world\"]"
                                                    + "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("complex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hf");

    AllTypes complex = new AllTypes(false);
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(4, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertEquals("hf", allTypes.getString().get());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(2, allTypes.getComplex().get().size(), allTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals(2, allTypes.getComplex().get().getStringArray().size());
    Assertions.assertFalse(allTypes.getComplex().get().getString().isPresent());
    Assertions.assertTrue(allTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(Long.MAX_VALUE, allTypes.getComplex().get().getNumber().get());
    Assertions.assertEquals(2, allTypes.getComplex().get().getStringArray().size());
    Assertions.assertEquals("hello world", allTypes.getComplex().get().getStringArray().get(0));
    Assertions.assertEquals("goodbye world", allTypes.getComplex().get().getStringArray().get(1));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that complex types can be added to a multi valued complex type
   */
  @Test
  public void testAddNewAttributeToMultiValuedComplexNode()
  {
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                        "\"string\": \"hello world\"," +
                                        "\"number\": 5," +
                                        "\"boolArray\": [true, false, true]," +
                                        "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}",

                                        "{" +
                                        "\"string\": \"happy day\"," +
                                        "\"number\": 45678987646454," +
                                        "\"stringArray\": [\"true\", \"wtf\", \"hello world\"]," +
                                        "\"dateArray\": [ \"1976-03-10T00:00:00Z\", \"1986-03-10T00:00:00Z\" ]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("multicomplex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(0)), allTypes.getMultiComplex().get(0));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(1)), allTypes.getMultiComplex().get(1));
  }

  /**
   * verifies that complex types can be added to a multi valued complex type with the replace operation
   */
  @Test
  public void testAddNewAttributeToMultiValuedComplexNodeWithReplaceOperation()
  {
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                        "\"string\": \"hello world\"," +
                                        "\"number\": 5," +
                                        "\"boolArray\": [true, false, true]," +
                                        "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}",

                                        "{" +
                                        "\"string\": \"happy day\"," +
                                        "\"number\": 45678987646454," +
                                        "\"stringArray\": [\"true\", \"wtf\", \"hello world\"]," +
                                        "\"dateArray\": [ \"1976-03-10T00:00:00Z\", \"1986-03-10T00:00:00Z\" ]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("multicomplex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(0)), allTypes.getMultiComplex().get(0));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(1)), allTypes.getMultiComplex().get(1));
  }

  /**
   * verifies that complex types can be added to a multi valued complex type while preserving old values
   */
  @Test
  public void testAddNewAttributeToMultiValuedComplexNodeAndPreserveOldEntries()
  {
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                        "\"string\": \"hello world\"," +
                                        "\"number\": 5," +
                                        "\"boolArray\": [true, false, true]," +
                                        "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}",

                                        "{" +
                                        "\"string\": \"happy day\"," +
                                        "\"number\": 45678987646454," +
                                        "\"stringArray\": [\"true\", \"wtf\", \"hello world\"]," +
                                        "\"dateArray\": [ \"1976-03-10T00:00:00Z\", \"1986-03-10T00:00:00Z\" ]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("multicomplex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Collections.singletonList(complex));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(3, allTypes.getMultiComplex().size());
    Assertions.assertEquals(complex, allTypes.getMultiComplex().get(0));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(0)), allTypes.getMultiComplex().get(1));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(1)), allTypes.getMultiComplex().get(2));
  }

  /**
   * verifies that a multi valued complex type can be replaced
   */
  @Test
  public void testReplaceMultiValuedComplexType()
  {
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                        "\"string\": \"hello world\"," +
                                        "\"number\": 5," +
                                        "\"boolArray\": [true, false, true]," +
                                        "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}",

                                        "{" +
                                        "\"string\": \"happy day\"," +
                                        "\"number\": 45678987646454," +
                                        "\"stringArray\": [\"true\", \"wtf\", \"hello world\"]," +
                                        "\"dateArray\": [ \"1976-03-10T00:00:00Z\", \"1986-03-10T00:00:00Z\" ]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("multicomplex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Collections.singletonList(complex));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    MatcherAssert.assertThat(allTypes.getMultiComplex(), Matchers.not(Matchers.hasItem(complex)));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(0)), allTypes.getMultiComplex().get(0));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(1)), allTypes.getMultiComplex().get(1));
  }

  /**
   * verifies nothing happens to the resource if the expression did not match<br>
   * the expression would add the stringarray attribute to all multicomplex nodes but since there is no node the
   * stringarray attribute can neither be added nor replaced
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddArrayAttributeToMultiComplexTypeWithMissingTarget(PatchOp patchOp)
  {
    List<String> values = Collections.singletonList("hello world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path("multicomplex.stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes patchedResource = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertFalse(patchedResource.getMeta().isPresent());
  }

  /**
   * this test will show that values can be added to arrays within multi complex attributes. If no filter is
   * given then the new value will be added to all complex representations within the multi valued complex array
   */
  @Test
  public void testAddArrayAttributeToMultiComplexType()
  {
    List<String> values = Collections.singletonList("hello world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("multicomplex.stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    AllTypes complex2 = new AllTypes(false);
    complex.setString("goldfish");
    complex2.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    Assertions.assertEquals(2, allTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals(2, allTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * this test will show that arrays within multi valued complex types can be replaced.
   */
  @Test
  public void testReplaceArrayAttributeInMultiComplexType()
  {
    List<String> values = Collections.singletonList("hello world");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("multicomplex.stringarray")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    AllTypes complex2 = new AllTypes(false);
    complex.setString("goldfish");
    complex2.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("hello world", allTypes.getMultiComplex().get(0).getStringArray().get(0));
    Assertions.assertEquals("hello world", allTypes.getMultiComplex().get(1).getStringArray().get(0));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * this test will show that a simple attribute within multi valued complex types can be replaced if already
   * existent
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToMultiComplexType(PatchOp patchOp)
  {
    final String value = "hello world";
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path("multicomplex.string")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    AllTypes complex2 = new AllTypes(false);
    complex.setString("goldfish");
    complex2.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), "multiComplex and meta must be present\n" + allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    Assertions.assertEquals(value, allTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals(value, allTypes.getMultiComplex().get(1).getString().get());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that the meta attribute is not touched if no effective change was made
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToMultiComplexTypeWithoutEffectiveChange(PatchOp patchOp)
  {
    final String value = "hello world";
    List<String> values = Collections.singletonList(value);
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path("multicomplex.string")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    AllTypes complex2 = new AllTypes(false);
    complex.setString(value);
    complex2.setString(value);
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), "multiComplex and meta must be present\n" + allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    Assertions.assertEquals(value, allTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals(value, allTypes.getMultiComplex().get(1).getString().get());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertFalse(allTypes.getMeta().isPresent());
  }

  /**
   * this test will show that a sanitized exception is thrown if the client gave an illegal value for a complex
   * type injection
   */
  @Test
  public void testAddComplexIntoMultiValuedWithIllegalValue()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"]";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.debug(ex.getDetail(), ex);
      Assertions.assertEquals("the value must be a whole complex type json structure but was: 'goldfish'",
                              ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * this test will show that a sanitized exception is thrown if the client gave an illegal value for a complex
   * type replacement
   */
  @Test
  public void testReplaceComplexIntoMultiValuedWithIllegalValue()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"]";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.debug(ex.getDetail(), ex);
      Assertions.assertEquals("the values are expected to be valid json representations for an expression as "
                              + "'multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"]' "
                              + "but was: goldfish",
                              ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_PATH, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * verifies that an exception is thrown if the client tries to add a non json value into a complex type
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddComplexIntoMultiValuedWithSimpleValue(PatchOp patchOp)
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals("the value parameters must be valid json representations but was\n'goldfish'",
                              ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_PATH, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * verifies that an exception is thrown if the client tries to add several values to a simple type
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSeveralValuesToSimpleType(PatchOp patchOp)
  {
    List<String> values = Arrays.asList("value1", "value2");
    final String path = "string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals("several values found for non multivalued node of type 'STRING'", ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * verifies that an exception is thrown if the subAttribute of a expression filter is unknown
   */
  @Test
  public void testAddSimpleAttributeToComplexTypesWithUnknownSubAttribute()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].unknown";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2));
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
    }
    catch (ScimException ex)
    {
      log.debug(ex.getDetail(), ex);
      Assertions.assertEquals("the attribute with the name 'multiComplex.unknown' is unknown "
                              + "to resource type 'AllTypes'",
                              ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_PATH, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * verifies that a simple attribute can be added to multi complex type that will match the given filter
   * expression
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToComplexTypesMatchingFilter(PatchOp patchOp)
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.getMultiComplex().size());
    Assertions.assertEquals(2,
                            allTypes.getMultiComplex().get(0).size(),
                            allTypes.getMultiComplex().get(0).toPrettyString());
    Assertions.assertEquals(2,
                            allTypes.getMultiComplex().get(1).size(),
                            allTypes.getMultiComplex().get(1).toPrettyString());
    Assertions.assertEquals(1,
                            allTypes.getMultiComplex().get(2).size(),
                            allTypes.getMultiComplex().get(2).toPrettyString());
    Assertions.assertEquals("goldfish", allTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals("goldfish", allTypes.getMultiComplex().get(1).getString().get());
    Assertions.assertFalse(allTypes.getMultiComplex().get(2).getString().isPresent());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that the last modified value is not set if no effective change was made
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToComplexTypesWithoutEffectiveChange(PatchOp patchOp)
  {
    final String value = "happy day";
    List<String> values = Collections.singletonList(value);
    final String path = "multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setString(value);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setString(value);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.getMultiComplex().size());
    Assertions.assertEquals(2,
                            allTypes.getMultiComplex().get(0).size(),
                            allTypes.getMultiComplex().get(0).toPrettyString());
    Assertions.assertEquals(2,
                            allTypes.getMultiComplex().get(1).size(),
                            allTypes.getMultiComplex().get(1).toPrettyString());
    Assertions.assertEquals(1,
                            allTypes.getMultiComplex().get(2).size(),
                            allTypes.getMultiComplex().get(2).toPrettyString());
    Assertions.assertEquals(value, allTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals(value, allTypes.getMultiComplex().get(1).getString().get());
    Assertions.assertFalse(allTypes.getMultiComplex().get(2).getString().isPresent());
    Assertions.assertFalse(allTypes.getMeta().isPresent(), "no effective change has been made so meta must be empty");
  }

  /**
   * verifies that a simple attribute can be added to multi complex types that will match the given filter
   * expression
   */
  @Test
  public void testAddArrayAttributeToComplexTypesMatchingFilter()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multicomplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.getMultiComplex().size());
    Assertions.assertEquals(1,
                            allTypes.getMultiComplex().get(0).size(),
                            allTypes.getMultiComplex().get(0).toPrettyString());
    Assertions.assertEquals(1,
                            allTypes.getMultiComplex().get(1).size(),
                            allTypes.getMultiComplex().get(1).toPrettyString());
    Assertions.assertEquals(1,
                            allTypes.getMultiComplex().get(2).size(),
                            allTypes.getMultiComplex().get(2).toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals("hello world", allTypes.getMultiComplex().get(0).getStringArray().get(0));
    Assertions.assertEquals("goldfish", allTypes.getMultiComplex().get(0).getStringArray().get(1));
    Assertions.assertEquals(2, allTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("goodbye world", allTypes.getMultiComplex().get(1).getStringArray().get(0));
    Assertions.assertEquals("goldfish", allTypes.getMultiComplex().get(1).getStringArray().get(1));
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(2).getStringArray().size());
    Assertions.assertEquals("empty world", allTypes.getMultiComplex().get(2).getStringArray().get(0));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that an array within a multi valued complex type can be replaced
   */
  @Test
  public void testReplaceArrayAttributeInComplexTypesMatchingFilter()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multicomplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.getMultiComplex().size());
    Assertions.assertEquals(1,
                            allTypes.getMultiComplex().get(0).size(),
                            allTypes.getMultiComplex().get(0).toPrettyString());
    Assertions.assertEquals(1,
                            allTypes.getMultiComplex().get(1).size(),
                            allTypes.getMultiComplex().get(1).toPrettyString());
    Assertions.assertEquals(1,
                            allTypes.getMultiComplex().get(2).size(),
                            allTypes.getMultiComplex().get(2).toPrettyString());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals("goldfish", allTypes.getMultiComplex().get(0).getStringArray().get(0));
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("goldfish", allTypes.getMultiComplex().get(1).getStringArray().get(0));
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(2).getStringArray().size());
    Assertions.assertEquals("empty world", allTypes.getMultiComplex().get(2).getStringArray().get(0));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that an exception is thrown if the filter does not return any results
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testReplaceWithNotMatchingFilter(PatchOp patchOp)
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multicomplex[stringarray eq \"goldfish\" or stringarray eq \"blubb\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));
    AllTypes patchedResource = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertFalse(patchedResource.getMeta().isPresent());
  }

  /**
   * verifies that an exception is thrown if the filter does not return any results
   */
  @Test
  public void testAndExpressionFilter()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multicomplex[stringarray eq \"hello world\" and string eq \"blubb\"].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setString("blubb");
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(3, allTypes.getMultiComplex().size());

    Assertions.assertEquals(2, allTypes.getMultiComplex().get(0).size());
    Assertions.assertEquals("blubb", allTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals("goldfish", allTypes.getMultiComplex().get(0).getStringArray().get(0));

    Assertions.assertEquals(1, allTypes.getMultiComplex().get(1).size());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("goodbye world", allTypes.getMultiComplex().get(1).getStringArray().get(0));

    Assertions.assertEquals(1, allTypes.getMultiComplex().get(2).size());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(2).getStringArray().size());
    Assertions.assertEquals("empty world", allTypes.getMultiComplex().get(2).getStringArray().get(0));

    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that an exception is thrown if the filter does not return any results
   */
  @Test
  public void testNotExpressionFilter()
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multicomplex[not (stringarray eq \"hello world\")].stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes multiComplex1 = new AllTypes(false);
    multiComplex1.setString("blubb");
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes(false);
    multiComplex3.setStringArray(Collections.singletonList("empty world"));

    allTypes.setMultiComplex(Arrays.asList(multiComplex1, multiComplex2, multiComplex3));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(3, allTypes.getMultiComplex().size());

    Assertions.assertEquals(2, allTypes.getMultiComplex().get(0).size());
    Assertions.assertEquals("blubb", allTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(0).getStringArray().size());
    Assertions.assertEquals("hello world", allTypes.getMultiComplex().get(0).getStringArray().get(0));

    Assertions.assertEquals(1, allTypes.getMultiComplex().get(1).size());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(1).getStringArray().size());
    Assertions.assertEquals("goldfish", allTypes.getMultiComplex().get(1).getStringArray().get(0));

    Assertions.assertEquals(1, allTypes.getMultiComplex().get(2).size());
    Assertions.assertEquals(1, allTypes.getMultiComplex().get(2).getStringArray().size());
    Assertions.assertEquals("goldfish", allTypes.getMultiComplex().get(2).getStringArray().get(0));

    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies an add operation will also be handled on an extension
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToExtension(PatchOp patchOp)
  {
    final String value = "hello world";
    List<String> values = Collections.singletonList(value);
    final String path = "costCenter";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("humpty dumpty").build());

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getEnterpriseUser().isPresent());
    Assertions.assertTrue(allTypes.getEnterpriseUser().get().getCostCenter().isPresent());
    Assertions.assertEquals(value, allTypes.getEnterpriseUser().get().getCostCenter().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
    Assertions.assertEquals(2, allTypes.getSchemas().size(), allTypes.toPrettyString());
    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * verifies a remove operation will successfully remove an attribute from an extension
   */
  @Test
  public void testRemoveSimpleAttributeFromExtension()
  {
    final String path = "costCenter";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("humpty dumpty").department("department").build());

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getEnterpriseUser().isPresent());
    Assertions.assertTrue(allTypes.getEnterpriseUser().get().getDepartment().isPresent());
    Assertions.assertFalse(allTypes.getEnterpriseUser().get().getCostCenter().isPresent());
    Assertions.assertEquals(2, allTypes.getSchemas().size(), allTypes.toPrettyString());
    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * verifies a remove operation will successfully remove a multi valued complex type attribute from an
   * extension
   */
  @Test
  public void testRemoveMultiValuedComplexAttributeFromExtension()
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema enterpriseUserSchema = resourceTypeFactory.getSchemaFactory()
                                                     .getResourceSchema(SchemaUris.ENTERPRISE_USER_URI);
    enterpriseUserSchema.addAttribute(emailsDef);


    final String path = AttributeNames.RFC7643.EMAILS;
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
    ScimArrayNode emailsArrayNode = new ScimArrayNode(null);
    Email email = Email.builder().value(UUID.randomUUID().toString()).build();
    emailsArrayNode.add(email);
    enterpriseUser.set(AttributeNames.RFC7643.EMAILS, emailsArrayNode);
    allTypes.setEnterpriseUser(enterpriseUser);

    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
    Assertions.assertEquals("hello world", allTypes.getString().get());
    Assertions.assertFalse(allTypes.getEnterpriseUser().isPresent());
    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes"));
  }

  /**
   * verifies adding or replacing a multivalued complex type within an extension does work as specified
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddMultiValuedComplexAttributeToExtension(PatchOp patchOp)
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema enterpriseUserSchema = resourceTypeFactory.getSchemaFactory()
                                                     .getResourceSchema(SchemaUris.ENTERPRISE_USER_URI);
    enterpriseUserSchema.addAttribute(emailsDef);


    // @formatter:off
    final List<String> values = Arrays.asList("{"
                                                  + "\"value\": \"chuck@norris.com\","
                                                  + "\"type\": \"work\","
                                                  + "\"primary\": true"
                                             + "}");
    // @formatter:on
    final String path = AttributeNames.RFC7643.EMAILS;
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);

    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes"));
    AllTypes patchedResource = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, patchedResource.size(), patchedResource.toPrettyString());
    Assertions.assertTrue(patchedResource.getMeta().isPresent(), patchedResource.toPrettyString());
    Assertions.assertTrue(patchedResource.getMeta().get().getLastModified().isPresent(),
                          patchedResource.toPrettyString());
    Assertions.assertTrue(patchedResource.getEnterpriseUser().isPresent(), patchedResource.toPrettyString());
    ArrayNode emails = (ArrayNode)patchedResource.getEnterpriseUser().get().get(AttributeNames.RFC7643.EMAILS);
    Assertions.assertNotNull(emails, patchedResource.toPrettyString());
    Assertions.assertEquals(1, emails.size(), patchedResource.toPrettyString());
    MatcherAssert.assertThat(patchedResource.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * verifies a remove operation will successfully remove also the extension itself if the extension is empty
   * after the removal of the attribute
   */
  @Test
  public void testRemoveSimpleAttributeFromExtensionAndExtensionItself()
  {
    final String path = "costCenter";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("humpty dumpty").build());

    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertFalse(allTypes.getEnterpriseUser().isPresent(), allTypes.toPrettyString());
    Assertions.assertEquals(1, allTypes.getSchemas().size(), allTypes.toPrettyString());
    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes"));
  }

  /**
   * verifies a remove operation will successfully remove also the extension itself if the extension is empty
   * after the removal of the attribute but in this case we will at first make the manager attribute empty
   */
  @Test
  public void testRemoveSimpleAttributeFromExtensionComplexAndExtensionItself()
  {
    final String path = "manager.value";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setEnterpriseUser(EnterpriseUser.builder().manager(Manager.builder().value("123456").build()).build());

    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes",
                                               SchemaUris.ENTERPRISE_USER_URI));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertFalse(allTypes.getEnterpriseUser().isPresent(), allTypes.toPrettyString());
    Assertions.assertEquals(1, allTypes.getSchemas().size(), allTypes.toPrettyString());
    MatcherAssert.assertThat(allTypes.getSchemas(),
                             Matchers.contains("urn:gold:params:scim:schemas:custom:2.0:AllTypes"));
  }

  /**
   * verifies an add operation will also be handled on an extension that has not been set yet
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddSimpleAttributeToExtensionIfExtensionNotPresent(PatchOp patchOp)
  {
    final String value = "hello world";
    List<String> values = Collections.singletonList(value);
    final String path = "costCenter";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getEnterpriseUser().isPresent());
    Assertions.assertTrue(allTypes.getEnterpriseUser().get().getCostCenter().isPresent());
    Assertions.assertEquals(value, allTypes.getEnterpriseUser().get().getCostCenter().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies an add operation will also be handled on an extension that has not been set yet
   */
  @Test
  public void testMissingTargetForRemove()
  {
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder().op(PatchOp.REMOVE).build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);

    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.debug(ex.getDetail(), ex);
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals("missing target for remove operation", ex.getDetail());
    }
  }

  /**
   * verifies a remove operation will fail if values are added into the request
   */
  @Test
  public void testRemoveWithValuesSet()
  {
    final String value = "hello world";
    List<String> values = Collections.singletonList(value);
    final String path = "string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);

    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.debug(ex.getDetail(), ex);
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals("values must not be set for remove operation but was: hello world", ex.getDetail());
    }
  }

  /**
   * verifies that nothing happens if the target does not exist
   */
  @ParameterizedTest
  @ValueSource(strings = {"string", "stringArray", "complex", "complex.string", "complex.stringarray", "multicomplex",
                          "multicomplex.string", "multicomplex.stringarray"})
  public void testRemoveNotExistingTarget(String path)
  {
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);

    AllTypes patchedResouce = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(allTypes, patchedResouce);
    Assertions.assertFalse(patchedResouce.getMeta().isPresent(), patchedResouce.toPrettyString());
  }

  /**
   * verifies removing a simple attribute works as expected
   */
  @Test
  public void testRemoveSimpleAttribute()
  {
    final String path = "string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a simple array attribute works as expected
   */
  @Test
  public void testRemoveSimpleArrayAttribute()
  {
    final String path = "stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    allTypes.setStringArray(Collections.singletonList("hello world"));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that removing an attribute from a complex attribute and the complex attribute is empty afterwards,
   * that the whole complex attribute is removed
   */
  @Test
  public void testRemoveSimpleAttributeWithinComplex()
  {
    final String path = "complex.string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    allTypes.setComplex(complex);

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertFalse(allTypes.getComplex().isPresent());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a simple attribute from a complex type works as expected
   */
  @Test
  public void testRemoveSimpleAttributeWithinComplexWithSeveralValues()
  {
    final String path = "complex.string";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    complex.setNumber(5L);
    allTypes.setComplex(complex);

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(1, allTypes.getComplex().get().size());
    Assertions.assertFalse(allTypes.getComplex().get().getString().isPresent());
    Assertions.assertTrue(allTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(5L, allTypes.getComplex().get().getNumber().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a simple array attribute works as expected
   */
  @Test
  public void testRemoveArrayAttributeFromComplex()
  {
    final String path = "complex.stringarray";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setStringArray(Collections.singletonList("hello world"));
    complex.setNumber(5L);
    allTypes.setComplex(complex);

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(1, allTypes.getComplex().get().size(), allTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals(0, allTypes.getComplex().get().getStringArray().size());
    Assertions.assertTrue(allTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(5L, allTypes.getComplex().get().getNumber().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a multi complex type works as expected
   */
  @Test
  public void testRemoveMulticomplex()
  {
    final String path = "multicomplex";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setStringArray(Collections.singletonList("hello world"));
    complex.setNumber(5L);
    AllTypes complex2 = new AllTypes(false);
    complex2.setNumber(3L);
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(0, allTypes.getMultiComplex().size());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing the only value from a multi complex type removes the whole type
   */
  @Test
  public void testRemoveTheOnlyAttributeFromMulticomplex()
  {
    final String path = "multicomplex.number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setNumber(5L);
    allTypes.setMultiComplex(Collections.singletonList(complex));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(0, allTypes.getMultiComplex().size());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a value from a multi complex type works as expected
   */
  @Test
  public void testRemoveMulticomplexSimpleAttribute()
  {
    final String path = "multicomplex.number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setNumber(5L);
    complex.setBool(true);
    allTypes.setMultiComplex(Collections.singletonList(complex));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(1, allTypes.getMultiComplex().size());
    Assertions.assertTrue(allTypes.getMultiComplex().get(0).getBool().isPresent());
    Assertions.assertTrue(allTypes.getMultiComplex().get(0).getBool().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a value from a multi valued complex type with filter removes the whole complex type if it
   * was the only attribute
   */
  @Test
  public void testRemoveMulticomplexSimpleAttributeByFilter()
  {
    final String path = "multicomplex[number eq 5].number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setNumber(5L);
    AllTypes complex2 = new AllTypes(false);
    complex2.setNumber(10L);
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(1, allTypes.getMultiComplex().size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getMultiComplex().get(0).getNumber().isPresent());
    Assertions.assertEquals(10L, allTypes.getMultiComplex().get(0).getNumber().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a value from a multi valued complex type with filter works as expected
   */
  @Test
  public void testRemoveMulticomplexSimpleAttributeByFilter2()
  {
    final String path = "multicomplex[string eq \"hello world\"].number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    complex.setNumber(5L);
    complex.setBool(true);
    AllTypes complex2 = new AllTypes(false);
    complex2.setNumber(10L);
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    Assertions.assertFalse(allTypes.getMultiComplex().get(0).getNumber().isPresent());
    Assertions.assertTrue(allTypes.getMultiComplex().get(0).getString().isPresent());
    Assertions.assertEquals("hello world", allTypes.getMultiComplex().get(0).getString().get());
    Assertions.assertTrue(allTypes.getMultiComplex().get(1).getNumber().isPresent());
    Assertions.assertEquals(10L, allTypes.getMultiComplex().get(1).getNumber().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies removing a whole complex type from an array with a filter works as expected
   */
  @Test
  public void testRemoveMulticomplexByFilter()
  {
    final String path = "multicomplex[string eq \"hello world\"]";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REMOVE)
                                                                                .path(path)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    complex.setNumber(5L);
    complex.setBool(true);
    AllTypes complex2 = new AllTypes(false);
    complex2.setNumber(10L);
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(1, allTypes.getMultiComplex().size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getMultiComplex().get(0).getNumber().isPresent());
    Assertions.assertEquals(10L, allTypes.getMultiComplex().get(0).getNumber().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies adding and replacing a subattribute to a complex type with a matching filter expression works
   * correctly
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddAttributeWithFilterExpression(PatchOp patchOp)
  {
    final String path = "complex[string eq \"hello world\"].number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(Collections.singletonList("5"))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    allTypes.setComplex(complex);

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getComplex().isPresent(), allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getComplex().get().size(), allTypes.toPrettyString());
    Assertions.assertEquals("hello world", allTypes.getComplex().get().getString().get(), allTypes.toPrettyString());
    Assertions.assertEquals(5L, allTypes.getComplex().get().getNumber().get(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
    Assertions.assertEquals(1, allTypes.getSchemas().size());
  }

  /**
   * verifies adding and replacing a subattribute to a complex type with a none matching filter will result in
   * an unchanged resource
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddAttributeWithFilterExpressionThatDoesNotMatch(PatchOp patchOp)
  {
    final String path = "complex[string eq \"hello world\"].number";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(Collections.singletonList("5"))
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("jippie ay yay");
    allTypes.setComplex(complex);

    AllTypes patchedResource = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertFalse(patchedResource.getMeta().isPresent());
  }

  /**
   * verifies that nothing happens if the filter expression does not match for a complex type attribute<br>
   * the filter here would normally add the given values or replace them if the filter matches but since the
   * filter will not match no changes should be made
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testReplaceComplexTypeValuesWithNoneMatchingFilterPath(PatchOp patchOp)
  {
    final String path = "complex[string eq \"hello world\"]";
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                          "\"string\": \"hello world\"," +
                                          "\"number\": 5," +
                                          "\"boolArray\": [true, false, true]," +
                                          "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("jippie ay yay");
    allTypes.setComplex(complex);

    AllTypes patchedResource = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertFalse(patchedResource.getMeta().isPresent());
  }

  /**
   * verifies that the values within a complex type are added/replaced if the filter expression does match
   */
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testReplaceComplexTypeValuesWithFilterPath(PatchOp patchOp)
  {
    final String path = "complex[string eq \"hello world\"]";
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                          "\"string\": \"dooms day\"," +
                                          "\"number\": 5," +
                                          "\"boolArray\": [true, false, true]," +
                                          "\"decimalArray\": [1.1, 2.2, 3.3]" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes(true);
    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    allTypes.setComplex(complex);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertTrue(allTypes.getComplex().get().getString().isPresent());
    Assertions.assertEquals("dooms day", allTypes.getComplex().get().getString().get());
    Assertions.assertTrue(allTypes.getComplex().get().getNumber().isPresent());
    Assertions.assertEquals(5L, allTypes.getComplex().get().getNumber().get());
    MatcherAssert.assertThat(allTypes.getComplex().get().getBoolArray(), Matchers.contains(true, false, true));
    MatcherAssert.assertThat(allTypes.getComplex().get().getDecimalArray(), Matchers.contains(1.1, 2.2, 3.3));
  }

  /**
   * this test will verify that the implementation follows the defined behaviour of RFC7644 for primary types:
   * <br>
   *
   * <pre>
   *   For multi-valued attributes, a PATCH operation that sets a value's
   *   "primary" sub-attribute to "true" SHALL cause the server to
   *   automatically set "primary" to "false" for any other values in the
   *   array.
   * </pre>
   */
  @Test
  public void testSetNewPrimaryValueWithAdd()
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(emailsDef);
    List<Email> emails = Arrays.asList(Email.builder().value("1@1.de").primary(true).build(),
                                       Email.builder().value("2@2.de").build(),
                                       Email.builder().value("3@3.de").build());
    AllTypes allTypes = new AllTypes(true);
    ScimArrayNode emailArray = new ScimArrayNode(null);
    emails.forEach(emailArray::add);
    allTypes.set(AttributeNames.RFC7643.EMAILS, emailArray);

    final String path = "emails";
    // @formatter:off
    List<String> values = Arrays.asList("{" +
                                          "\"value\": \"4@4.de\"," +
                                          "\"primary\": true" +
                                        "}");
    // @formatter:on
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(1, allTypes.getSchemas().size(), allTypes.toPrettyString());
    emailArray = (ScimArrayNode)allTypes.get(AttributeNames.RFC7643.EMAILS);
    Assertions.assertNotNull(emailArray);
    Assertions.assertEquals(4, emailArray.size());
    for ( JsonNode email : emailArray )
    {
      String emailText = email.get(AttributeNames.RFC7643.VALUE).textValue();
      if (emailText.equals("4@4.de"))
      {
        Assertions.assertTrue(email.get(AttributeNames.RFC7643.PRIMARY).booleanValue(), allTypes.toPrettyString());
      }
      else
      {
        Assertions.assertNull(email.get(AttributeNames.RFC7643.PRIMARY), allTypes.toPrettyString());
      }
    }
  }

  /**
   * this test will verify that the implementation follows the defined behaviour of RFC7644 for primary types:
   * <br>
   *
   * <pre>
   *   For multi-valued attributes, a PATCH operation that sets a value's
   *   "primary" sub-attribute to "true" SHALL cause the server to
   *   automatically set "primary" to "false" for any other values in the
   *   array.
   * </pre>
   */
  @Test
  public void testSetNewPrimaryValueWithReplaceAndFilter()
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(emailsDef);
    List<Email> emails = Arrays.asList(Email.builder().value("1@1.de").primary(true).build(),
                                       Email.builder().value("2@2.de").build(),
                                       Email.builder().value("3@3.de").build());
    AllTypes allTypes = new AllTypes(true);
    ScimArrayNode emailArray = new ScimArrayNode(null);
    emails.forEach(emailArray::add);
    allTypes.set(AttributeNames.RFC7643.EMAILS, emailArray);

    final String path = "emails[value sw \"2\"].primary";
    List<String> values = Arrays.asList("true");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(1, allTypes.getSchemas().size(), allTypes.toPrettyString());
    emailArray = (ScimArrayNode)allTypes.get(AttributeNames.RFC7643.EMAILS);
    Assertions.assertNotNull(emailArray);
    Assertions.assertEquals(3, emailArray.size());
    for ( JsonNode email : emailArray )
    {
      String emailText = email.get(AttributeNames.RFC7643.VALUE).textValue();
      if (emailText.equals("2@2.de"))
      {
        Assertions.assertTrue(email.get(AttributeNames.RFC7643.PRIMARY).booleanValue(), allTypes.toPrettyString());
      }
      else
      {
        Assertions.assertNull(email.get(AttributeNames.RFC7643.PRIMARY), allTypes.toPrettyString());
      }
    }
  }

  /**
   * this test will verify that the implementation follows the defined behaviour of RFC7644 for primary types:
   * <br>
   *
   * <pre>
   *   For multi-valued attributes, a PATCH operation that sets a value's
   *   "primary" sub-attribute to "true" SHALL cause the server to
   *   automatically set "primary" to "false" for any other values in the
   *   array.
   * </pre>
   */
  @Test
  public void testSetNewPrimaryValueWithAddAndResourceValue()
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(emailsDef);
    List<Email> emails = Arrays.asList(Email.builder().value("1@1.de").primary(true).build(),
                                       Email.builder().value("2@2.de").build(),
                                       Email.builder().value("3@3.de").build());
    AllTypes originalAllTypes = new AllTypes(true);
    ScimArrayNode emailArray = new ScimArrayNode(null);
    emails.forEach(emailArray::add);
    originalAllTypes.set(AttributeNames.RFC7643.EMAILS, emailArray);

    AllTypes patchResource = new AllTypes(true);
    ScimArrayNode patchEmailArray = new ScimArrayNode(null);
    patchEmailArray.add(Email.builder().value("4@4.de").primary(true).build());
    patchResource.set(AttributeNames.RFC7643.EMAILS, patchEmailArray);

    List<String> values = Collections.singletonList(patchResource.toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .values(values)
                                                                                .build());


    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    originalAllTypes = patchHandler.patchResource(originalAllTypes, patchOpRequest);
    Assertions.assertEquals(1, originalAllTypes.getSchemas().size(), originalAllTypes.toPrettyString());
    emailArray = (ScimArrayNode)originalAllTypes.get(AttributeNames.RFC7643.EMAILS);
    Assertions.assertNotNull(emailArray, originalAllTypes.toPrettyString());
    Assertions.assertEquals(4, emailArray.size(), originalAllTypes.toPrettyString());
    for ( JsonNode email : emailArray )
    {
      String emailText = email.get(AttributeNames.RFC7643.VALUE).textValue();
      if (emailText.equals("4@4.de"))
      {
        Assertions.assertTrue(email.get(AttributeNames.RFC7643.PRIMARY).booleanValue(),
                              originalAllTypes.toPrettyString());
      }
      else
      {
        Assertions.assertNull(email.get(AttributeNames.RFC7643.PRIMARY), originalAllTypes.toPrettyString());
      }
    }
  }

  /**
   * this test will verify that the implementation follows the defined behaviour of RFC7644 for primary types:
   * <br>
   *
   * <pre>
   *   For multi-valued attributes, a PATCH operation that sets a value's
   *   "primary" sub-attribute to "true" SHALL cause the server to
   *   automatically set "primary" to "false" for any other values in the
   *   array.
   * </pre>
   */
  @Test
  public void testSetNewPrimaryValueWithReplaceAndResourceValue()
  {
    JsonNode emailsDef = JsonHelper.loadJsonDocument(EMAILS_ATTRIBUTE);
    Schema allTypesSchema = resourceTypeFactory.getSchemaFactory().getResourceSchema(AllTypes.ALL_TYPES_URI);
    allTypesSchema.addAttribute(emailsDef);
    List<Email> emails = Arrays.asList(Email.builder().value("1@1.de").primary(true).build(),
                                       Email.builder().value("2@2.de").build(),
                                       Email.builder().value("3@3.de").build());
    AllTypes originalAllTypes = new AllTypes(true);
    ScimArrayNode emailArray = new ScimArrayNode(null);
    emails.forEach(emailArray::add);
    originalAllTypes.set(AttributeNames.RFC7643.EMAILS, emailArray);

    AllTypes patchResource = new AllTypes(true);
    ScimArrayNode patchEmailArray = new ScimArrayNode(null);
    patchEmailArray.add(Email.builder().value("4@4.de").primary(true).build());
    patchResource.set(AttributeNames.RFC7643.EMAILS, patchEmailArray);

    List<String> values = Collections.singletonList(patchResource.toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .values(values)
                                                                                .build());


    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    originalAllTypes = patchHandler.patchResource(originalAllTypes, patchOpRequest);
    Assertions.assertEquals(1, originalAllTypes.getSchemas().size(), originalAllTypes.toPrettyString());
    emailArray = (ScimArrayNode)originalAllTypes.get(AttributeNames.RFC7643.EMAILS);
    Assertions.assertNotNull(emailArray, originalAllTypes.toPrettyString());
    Assertions.assertEquals(1, emailArray.size(), originalAllTypes.toPrettyString());
    JsonNode email = emailArray.get(0);
    String emailText = email.get(AttributeNames.RFC7643.VALUE).textValue();
    Assertions.assertEquals("4@4.de", emailText);
    Assertions.assertTrue(email.get(AttributeNames.RFC7643.PRIMARY).booleanValue(), originalAllTypes.toPrettyString());
  }

}
