package de.gold.scim.patch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.ScimType;
import de.gold.scim.constants.enums.Mutability;
import de.gold.scim.constants.enums.PatchOp;
import de.gold.scim.constants.enums.Type;
import de.gold.scim.exceptions.ScimException;
import de.gold.scim.request.PatchOpRequest;
import de.gold.scim.request.PatchRequestOperation;
import de.gold.scim.resources.AllTypes;
import de.gold.scim.resources.EnterpriseUser;
import de.gold.scim.resources.base.ScimObjectNode;
import de.gold.scim.resources.complex.Meta;
import de.gold.scim.schemas.ResourceType;
import de.gold.scim.schemas.ResourceTypeFactory;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;
import de.gold.scim.utils.TestHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 30.10.2019 - 09:02 <br>
 * <br>
 */
@Slf4j
public class PatchAddResourceHandlerTest implements FileReferences
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
   * verifies that simple attributes can successfully be added by a patch operation
   */
  @TestFactory
  public List<DynamicTest> testAddSimpleAttribute()
  {
    List<DynamicTest> dynamicTestList = new ArrayList<>();
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("string", new TextNode("hello world"))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("date", new TextNode("1940-03-10T00:00:00Z"))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("date",
                                                                    new TextNode("1940-03-10T00:00:00+02:00"))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("number", new IntNode(5))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("number", new LongNode(Long.MAX_VALUE))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("decimal", new DoubleNode(5.4))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("decimal", new DoubleNode(Double.MAX_VALUE))));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("bool", BooleanNode.getTrue())));
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("bool", BooleanNode.getFalse())));

    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("string", new TextNode("hello world")),
                                                  new NameValuePair("number", new IntNode(5)),
                                                  new NameValuePair("bool", BooleanNode.getTrue()),
                                                  new NameValuePair("decimal", new DoubleNode(5.6))));

    AllTypes allTypes = new AllTypes();
    allTypes.setString("hello world");
    allTypes.setNumber(Long.MAX_VALUE);
    allTypes.setDecimal(5.8);
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("complex", allTypes)));
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.add(allTypes);
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("multiComplex", arrayNode)));

    return dynamicTestList;
  }

  /**
   * this test will verify that multi valued complex types will be added to already existing attributes
   */
  @Test
  public void testAddMultiValuedComplexType()
  {
    AllTypes allTypes = new AllTypes();
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes multiComplex = new AllTypes();
    multiComplex.setString("hello world");
    multiComplex.setNumber(Long.MAX_VALUE);
    multiComplex.setDecimal(5.8);
    allTypes.setMultiComplex(Collections.singletonList(multiComplex));

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setMultiComplex(Collections.singletonList(multiComplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    Assertions.assertEquals(multiComplex, allTypes.getMultiComplex().get(0));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that an exception with an appropriate error message is thrown if the value path is unknown
   */
  @Test
  public void testAttributeDoesNotExist()
  {
    AllTypes allTypes = new AllTypes();

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.set("unknown", new TextNode("unknown"));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail();
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_PATH, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * this test will verify that no changes are made to the resource if the given complex type is identical to
   * the existing one
   */
  @Test
  public void testComplexTypeAlreadyExists()
  {
    AllTypes allTypes = new AllTypes();
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes complex = new AllTypes();
    complex.setNumber(Long.MAX_VALUE);
    complex.setDecimal(5.8);
    allTypes.setComplex(complex);

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setComplex(complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertEquals(complex, allTypes.getComplex().get());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    // this is important. The change must not have been made so the lastModified value must not be present!
    Assertions.assertFalse(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * this test will verify that existing attributes from complex nodes are not removed if new attributes are
   * added
   */
  @Test
  public void testAddSimpleValuesToComplexAttribute()
  {
    AllTypes allTypes = new AllTypes();
    AllTypes innerComplex = new AllTypes();
    innerComplex.setString("hello world");
    allTypes.setComplex(innerComplex);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes complex = new AllTypes();
    complex.setNumber(Long.MAX_VALUE);
    complex.setDecimal(5.8);

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setComplex(complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertTrue(allTypes.getComplex().get().getString().isPresent(),
                          allTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals("hello world", allTypes.getComplex().get().getString().get());
    Assertions.assertEquals(2, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertNotEquals(complex, allTypes.getComplex().get());
    Assertions.assertEquals(3, allTypes.getComplex().get().size());
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    // this is important. The change must not have been made so the lastModified value must not be present!
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * verifies that no change was made if a simple attribute was added that has the same value as the existing
   * one
   */
  @Test
  public void testAddIdenticalSimpleValue()
  {
    AllTypes allTypes = new AllTypes();
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertEquals("hello world", allTypes.getString().get());
    // no change must have been made so last modified must not be present
    Assertions.assertFalse(allTypes.getMeta().isPresent());
  }

  /**
   * this test will verify that attributes can be added to simple arrays
   */
  @Test
  public void testAddMultiValuedArrayOnExistingArray()
  {
    AllTypes allTypes = new AllTypes();
    allTypes.setNumberArray(Arrays.asList(1L, 2L));

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setNumberArray(Arrays.asList(3L, 4L));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(4, allTypes.getNumberArray().size());
    MatcherAssert.assertThat(allTypes.getNumberArray(), Matchers.hasItems(1L, 2L, 3L, 4L));
  }

  /**
   * this test will verify that attributes can be added to simple arrays
   */
  @Test
  public void testAddMultiValuedArrayOnNotExistingArray()
  {
    AllTypes allTypes = new AllTypes();

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setNumberArray(Arrays.asList(3L, 4L));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(2, allTypes.getNumberArray().size());
    MatcherAssert.assertThat(allTypes.getNumberArray(), Matchers.hasItems(3L, 4L));
  }

  /**
   * this test will verify that attributes can be added to simple arrays
   */
  @Test
  public void testAddMultiValuedArrayWithUnknownAttribute()
  {
    AllTypes allTypes = new AllTypes();

    AllTypes multicomplex = new AllTypes();
    multicomplex.setString("hello world");
    multicomplex.set("unknown", new TextNode("unknown"));
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail(), ex);
      Assertions.assertEquals(ScimType.Custom.INVALID_PARAMETERS, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * builds a patch operation test in which the given values are added to the resource by the patch operation
   *
   * @param nameValuePair a mandatory name value pair that must be added
   * @param nameValuePairs additional name value pairs to check if several attributes are added
   */
  private DynamicTest getAddSimpleAttributeTest(NameValuePair nameValuePair, NameValuePair... nameValuePairs)
  {
    String testName = "\"" + nameValuePair.getAttributeName() + "\": \"" + nameValuePair.getValue() + "\"";
    if (nameValuePairs != null)
    {
      for ( NameValuePair valuePair : nameValuePairs )
      {
        testName += " ; " + "\"" + valuePair.getAttributeName() + "\": \"" + valuePair.getValue() + "\"";
      }
    }
    return DynamicTest.dynamicTest(testName, () -> {
      AllTypes allTypes = new AllTypes();
      AllTypes allTypeChanges = new AllTypes();
      allTypeChanges.set(nameValuePair.getAttributeName(), nameValuePair.getValue());
      if (nameValuePairs != null)
      {
        for ( NameValuePair valuePair : nameValuePairs )
        {
          allTypeChanges.set(valuePair.getAttributeName(), valuePair.getValue());
        }
      }
      List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                  .op(PatchOp.ADD)
                                                                                  .valueNode(allTypeChanges)
                                                                                  .build());
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
      PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
      Assertions.assertTrue(allTypes.isEmpty());
      allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.assertFalse(allTypes.isEmpty());
      // the added attribute and lastModifed have been added by patch
      Assertions.assertEquals(2 + (nameValuePairs == null ? 0 : nameValuePairs.length), allTypes.size());
      Assertions.assertNotNull(allTypes.get(nameValuePair.getAttributeName()));
      Assertions.assertEquals(nameValuePair.getValue(), allTypes.get(nameValuePair.getAttributeName()));
      Assertions.assertTrue(allTypes.getMeta().isPresent());
      Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
      if (nameValuePairs != null)
      {
        for ( NameValuePair valuePair : nameValuePairs )
        {
          Assertions.assertNotNull(allTypes.get(valuePair.getAttributeName()));
          Assertions.assertEquals(valuePair.getValue(), allTypes.get(valuePair.getAttributeName()));
        }
      }
    });
  }

  /**
   * verifies that an empty value list is not tolerated
   */
  @Test
  public void testNoValuesPresent()
  {
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder().op(PatchOp.ADD).build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    try
    {
      patchHandler.patchResource(new AllTypes(), patchOpRequest);
      Assertions.fail();
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
    }
  }

  /**
   * verifies that several values are not tolerated if no target is specified
   */
  @Test
  public void testNoTargetSpecifiedAndTwoResourcesAreAdded()
  {
    List<String> values = Arrays.asList(new AllTypes().toString(), new AllTypes().toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    try
    {
      patchHandler.patchResource(new AllTypes(), patchOpRequest);
      Assertions.fail();
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
    }
  }

  /**
   * this test must verify that an attribute may be set if the immutable attribute has not been set yet
   */
  @Test
  public void testAddImmutableAttributeIfNotSet()
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "string",
                                       null,
                                       Mutability.IMMUTABLE,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes();

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertEquals("hello world", allTypes.getString().get());
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly leads to a
   * {@link de.gold.scim.exceptions.BadRequestException} if tried to set. The immutable object should fail
   * because it is already set within this test
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "READ_ONLY"})
  public void testAddImmutableAndReadOnlyAttribute(Mutability mutability)
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta, "string", null, mutability, null, null, null, null, null, null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes();
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly leads to a
   * {@link de.gold.scim.exceptions.BadRequestException} if tried to set. The immutable object should fail
   * because it is already set within this test
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "READ_ONLY"})
  public void testAddImmutableAndReadOnlyAttributeForComplex(Mutability mutability)
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "complex.string",
                                       null,
                                       mutability,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes();
    AllTypes innerComplex = new AllTypes();
    innerComplex.setString("hello world");
    allTypes.setComplex(innerComplex);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes complex = new AllTypes();
    complex.setString("new hello world");

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setComplex(complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly leads to a
   * {@link de.gold.scim.exceptions.BadRequestException} if tried to set. The immutable object should fail
   * because it is already set within this test
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "READ_ONLY"})
  public void testAddImmutableAndReadOnlyAttributeForMultiComplexSubAttribute(Mutability mutability)
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "multiComplex.string",
                                       null,
                                       mutability,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes();

    AllTypes multicomplex = new AllTypes();
    multicomplex.setString("hello world");
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly leads to a
   * {@link de.gold.scim.exceptions.BadRequestException} if tried to set. The immutable object should fail
   * because it is already set within this test
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "READ_ONLY"})
  public void testAddImmutableAndReadOnlyAttributeForMultiComplex(Mutability mutability)
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta,
                                       "multiComplex",
                                       null,
                                       mutability,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes();

    AllTypes multicomplex = new AllTypes();
    multicomplex.setString("hello world");
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail(), ex);
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
    }
  }

  /**
   * this method will verify that a second added primary value will replace the previous set primary value
   */
  @Test
  public void testSetPrimary()
  {
    AllTypes allTypes = new AllTypes();

    AllTypes multicomplex = new AllTypes();
    multicomplex.set(AttributeNames.RFC7643.PRIMARY, BooleanNode.getTrue());
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    AtomicBoolean primaryFound = new AtomicBoolean(false);
    for ( JsonNode complex : allTypes.getMultiComplex() )
    {
      JsonNode prime = complex.get(AttributeNames.RFC7643.PRIMARY);
      if (primaryFound.get())
      {
        Assertions.fail("two primary values in the response");
      }
      primaryFound.weakCompareAndSet(false, prime != null && prime.booleanValue());
    }
    Assertions.assertTrue(primaryFound.get());
  }

  /**
   * verifies that it is illegal to add 2 primary values into a multi valued complex type
   */
  @Test
  public void testSetTwoPrimaryValues()
  {
    AllTypes allTypes = new AllTypes();

    AllTypes multicomplex = new AllTypes();
    multicomplex.set(AttributeNames.RFC7643.PRIMARY, BooleanNode.getTrue());
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setMultiComplex(Arrays.asList(multicomplex, multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail(), ex);
      Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }
  }

  /**
   * this test will verify that attribute adding works also for extensions
   */
  @Test
  public void testAddValuesWithinExtension()
  {
    AllTypes allTypes = new AllTypes();

    AllTypes allTypeChanges = new AllTypes();
    allTypeChanges.setEnterpriseUser(EnterpriseUser.builder().costCenter("something").build());

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(allTypes.getEnterpriseUser().isPresent());
  }

  /**
   * this test will verify that attribute adding works also for extensions even if there are naming conflicts
   * with the main schema
   */
  @Test
  public void testAddValuesWithinExtensionWithAttributeNameConflict()
  {
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    final String ambiguousAttributeName = "number";
    this.allTypesResourceType = TestHelper.addAttributeToSchema(resourceTypeFactory,
                                                                ambiguousAttributeName,
                                                                Type.STRING,
                                                                allTypesResourceTypeNode,
                                                                allTypesSchema,
                                                                enterpriseUserSchema);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes();
    allTypes.setNumber(50L);

    AllTypes allTypeChanges = new AllTypes();
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
    enterpriseUser.set(ambiguousAttributeName, new TextNode("hello world"));
    allTypeChanges.setEnterpriseUser(enterpriseUser);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(allTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals("hello world", allTypes.getEnterpriseUser().get().get(ambiguousAttributeName).textValue());
  }

  /**
   * this test will verify that attribute adding works also for extensions even if there are naming conflicts
   * with the main schema
   */
  @Test
  public void testAddValuesWithinExtensionWithAttributeNameConflictOnComplex()
  {
    JsonNode enterpriseUserSchema = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    JsonNode allTypesResourceTypeNode = JsonHelper.loadJsonDocument(ALL_TYPES_RESOURCE_TYPE);
    JsonNode allTypesSchema = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);

    ArrayNode attributes = (ArrayNode)enterpriseUserSchema.get(AttributeNames.RFC7643.ATTRIBUTES);
    attributes.add(JsonHelper.readJsonDocument(getComplexNodeDefinitionForTest()));
    this.allTypesResourceType = resourceTypeFactory.registerResourceType(null,
                                                                         allTypesResourceTypeNode,
                                                                         allTypesSchema,
                                                                         enterpriseUserSchema);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(enterpriseUserSchema);

    AllTypes allTypes = new AllTypes();
    AllTypes complexAllTypes = new AllTypes();
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    AllTypes allTypeChanges = new AllTypes();
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
    ScimObjectNode complexEnterprise = new ScimObjectNode();
    complexEnterprise.set("number", new TextNode("hello world"));
    enterpriseUser.set("complex", complexEnterprise);
    allTypeChanges.setEnterpriseUser(enterpriseUser);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(allTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals("hello world", allTypes.getEnterpriseUser().get().get("complex").get("number").textValue());
  }

  /**
   * this method returns a specific attribute definition that will be added to the enterprise user that is used
   * as extension for the alltypes schema. this shall provoke a naming conflict with a complex type in the
   * extension
   */
  private String getComplexNodeDefinitionForTest()
  {
    // @formatter:off
    return "{\n" +
             "  \"name\": \"complex\",\n" +
             "  \"type\": \"complex\",\n" +
             "  \"mutability\": \"readWrite\",\n" +
             "  \"returned\": \"default\",\n" +
             "  \"uniqueness\": \"none\",\n" +
             "  \"description\": \"test\",\n" +
             "  \"multiValued\": false,\n" +
             "  \"required\": false,\n" +
             "  \"caseExact\": false,\n" +
             "  \"subAttributes\": [\n" +
             "    {\n" +
             "      \"name\": \"number\",\n" +
             "      \"type\": \"string\",\n" +
             "      \"mutability\": \"readWrite\",\n" +
             "      \"returned\": \"default\",\n" +
             "      \"uniqueness\": \"none\",\n" +
             "      \"description\": \"test\",\n" +
             "      \"multiValued\": false,\n" +
             "      \"required\": false,\n" +
             "      \"caseExact\": false\n" +
             "    }\n" +
             "  ]\n" +
             "}";
    // @formatter:on
  }

  /**
   * used for easier creation of dynamic tests
   */
  @AllArgsConstructor
  @Getter
  public static class NameValuePair
  {

    private String attributeName;

    private JsonNode value;
  }
}
