package de.gold.scim.patch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.ScimType;
import de.gold.scim.constants.enums.PatchOp;
import de.gold.scim.exceptions.ScimException;
import de.gold.scim.request.PatchOpRequest;
import de.gold.scim.request.PatchRequestOperation;
import de.gold.scim.resources.AllTypes;
import de.gold.scim.resources.EnterpriseUser;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;
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
   * needed to extract the {@link de.gold.scim.schemas.ResourceType}s which are necessary to check if the given
   * attribute-names are valid or not
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
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertNotNull(allTypes.get(attributeName));
    Assertions.assertEquals(value, allTypes.get(attributeName).asText());
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertNotNull(allTypes.get(attributeName));
    Assertions.assertEquals(value, allTypes.get(attributeName).asText());
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    allTypes.setStringArray(Arrays.asList("1", "2", "3", "4", "humpty dumpty"));
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
    Assertions.assertEquals(values.size(), allTypes.getStringArray().size());
    MatcherAssert.assertThat(allTypes.getStringArray(), Matchers.contains(values));
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
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    List<Long> numberList = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    allTypes.setNumberArray(numberList);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    List<Long> numberList = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    allTypes.setNumberArray(numberList);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
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

    AllTypes allTypes = new AllTypes();
    allTypes.setString("salty");
    AllTypes complex = new AllTypes();
    complex.setString("sweet");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(3, allTypes.size());
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

    AllTypes allTypes = new AllTypes();
    allTypes.setString("salty");
    AllTypes complex = new AllTypes();
    complex.setString("sweet");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(3, allTypes.size());
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

    List<String> values = Collections.singletonList("{\"number\": " + Long.MAX_VALUE + ","
                                                    + "\"stringArray\":[\"hello world\", \"goodbye world\"]}");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(2, allTypes.getComplex().get().size());
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

    List<String> values = Collections.singletonList("{\"number\": " + Long.MAX_VALUE + ","
                                                    + "\"stringArray\":[\"hello world\", \"goodbye world\"]}");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path(attributeName)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(2, allTypes.getComplex().get().size());
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

    List<String> values = Collections.singletonList("{\"number\": " + Long.MAX_VALUE + ","
                                                    + "\"stringArray\":[\"hello world\", \"goodbye world\"]}");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path("complex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes();
    allTypes.setString("hf");

    AllTypes complex = new AllTypes();
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size());
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertEquals("hf", allTypes.getString().get());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(3, allTypes.getComplex().get().size());
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

    List<String> values = Collections.singletonList("{\"number\": " + Long.MAX_VALUE + ","
                                                    + "\"stringArray\":[\"hello world\", \"goodbye world\"]}");
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .path("complex")
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes();
    allTypes.setString("hf");

    AllTypes complex = new AllTypes();
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setComplex(complex);

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size());
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertEquals("hf", allTypes.getString().get());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(2, allTypes.getComplex().get().size());
    Assertions.assertEquals(2, allTypes.getComplex().get().getStringArray().size());
    Assertions.assertTrue(allTypes.getComplex().get().getString().isPresent());
    Assertions.assertEquals("goldfish", allTypes.getComplex().get().getString().get());
    Assertions.assertFalse(allTypes.getComplex().get().getNumber().isPresent());
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
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    AllTypes complex = new AllTypes();
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Collections.singletonList(complex));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    AllTypes complex = new AllTypes();
    complex.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Collections.singletonList(complex));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);

    Assertions.assertEquals(2, allTypes.size());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    Assertions.assertEquals(complex, allTypes.getMultiComplex().get(0));
    MatcherAssert.assertThat(allTypes.getMultiComplex(), Matchers.not(Matchers.hasItem(complex)));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(0)), allTypes.getMultiComplex().get(0));
    Assertions.assertEquals(JsonHelper.readJsonDocument(values.get(1)), allTypes.getMultiComplex().get(1));
  }

  /**
   * verifies that a {@link de.gold.scim.exceptions.BadRequestException} is thrown if no results are present for
   * the specified target
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
    AllTypes allTypes = new AllTypes();
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.NO_TARGET, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      MatcherAssert.assertThat(ex.getDetail(),
                               Matchers.equalTo("the multi valued complex type "
                                                + "'urn:gold:params:scim:schemas:custom:2.0:AllTypes:multiComplex' "
                                                + "is not set"));
    }
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
    AllTypes allTypes = new AllTypes();
    AllTypes complex = new AllTypes();
    AllTypes complex2 = new AllTypes();
    complex.setString("goldfish");
    complex2.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    AllTypes complex = new AllTypes();
    AllTypes complex2 = new AllTypes();
    complex.setString("goldfish");
    complex2.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
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
    AllTypes allTypes = new AllTypes();
    AllTypes complex = new AllTypes();
    AllTypes complex2 = new AllTypes();
    complex.setString("goldfish");
    complex2.setString("goldfish");
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), "multiComplex and meta must be present");
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
    AllTypes allTypes = new AllTypes();
    AllTypes complex = new AllTypes();
    AllTypes complex2 = new AllTypes();
    complex.setString(value);
    complex2.setString(value);
    complex.setStringArray(Collections.singletonList("happy day"));
    complex2.setStringArray(Collections.singletonList("happy day"));
    allTypes.setMultiComplex(Arrays.asList(complex, complex2));

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(1, allTypes.size(), "multiComplex and meta must be present");
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
  @ParameterizedTest
  @ValueSource(strings = {"ADD", "REPLACE"})
  public void testAddComplexIntoMultiValuedWithIllegalValue(PatchOp patchOp)
  {
    List<String> values = Collections.singletonList("goldfish");
    final String path = "multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye world\"]";
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(patchOp)
                                                                                .path(path)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    AllTypes allTypes = new AllTypes();
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals("the given expression is not valid for an add-operation: 'multiComplex[stringarray eq "
                              + "\"hello world\" or stringarray eq \"goodbye world\"]'. Did you want an expression "
                              + "like this 'multiComplex[stringarray eq \"hello world\" or stringarray eq \"goodbye "
                              + "world\"].subAttributeName'?",
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
    AllTypes allTypes = new AllTypes();
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
    AllTypes allTypes = new AllTypes();
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
    AllTypes allTypes = new AllTypes();
    AllTypes multiComplex1 = new AllTypes();
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes();
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
    AllTypes allTypes = new AllTypes();
    AllTypes multiComplex1 = new AllTypes();
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes();
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes();
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
    AllTypes allTypes = new AllTypes();
    AllTypes multiComplex1 = new AllTypes();
    multiComplex1.setString(value);
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes();
    multiComplex2.setString(value);
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes();
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
    AllTypes allTypes = new AllTypes();
    AllTypes multiComplex1 = new AllTypes();
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes();
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes();
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
    AllTypes allTypes = new AllTypes();
    AllTypes multiComplex1 = new AllTypes();
    multiComplex1.setStringArray(Collections.singletonList("hello world"));
    AllTypes multiComplex2 = new AllTypes();
    multiComplex2.setStringArray(Collections.singletonList("goodbye world"));
    AllTypes multiComplex3 = new AllTypes();
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
    AllTypes allTypes = new AllTypes();
    allTypes.setEnterpriseUser(EnterpriseUser.builder().costCenter("humpty dumpty").build());

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
    Assertions.assertTrue(allTypes.getEnterpriseUser().isPresent());
    Assertions.assertTrue(allTypes.getEnterpriseUser().get().getCostCenter().isPresent());
    Assertions.assertEquals(value, allTypes.getEnterpriseUser().get().getCostCenter().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
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
    AllTypes allTypes = new AllTypes();

    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size());
    Assertions.assertTrue(allTypes.getEnterpriseUser().isPresent());
    Assertions.assertTrue(allTypes.getEnterpriseUser().get().getCostCenter().isPresent());
    Assertions.assertEquals(value, allTypes.getEnterpriseUser().get().getCostCenter().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

}
