package de.captaingoldfish.scim.sdk.server.patch;

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

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.PatchConfig;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.resources.AllTypes;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;
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
   * needed to extract the {@link ResourceType}s which are necessary to check if the given attribute-names are
   * valid or not
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the resource type for all types definition. Contains data types of any possible scim representation
   */
  private ResourceType allTypesResourceType;

  /**
   * the service provider with the current endpoints configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * initializes a new {@link ResourceTypeFactory} for the following tests
   */
  @BeforeEach
  public void initialize()
  {
    this.serviceProvider = ServiceProvider.builder().patchConfig(PatchConfig.builder().supported(true).build()).build();
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

    AllTypes complex = new AllTypes(false);
    complex.setString("hello world");
    complex.setNumber(Long.MAX_VALUE);
    complex.setDecimal(5.8);
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("complex", complex)));
    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
    arrayNode.add(complex);
    dynamicTestList.add(getAddSimpleAttributeTest(new NameValuePair("multiComplex", arrayNode)));

    return dynamicTestList;
  }

  /**
   * this test will verify that multivalued complex types will be added to already existing attributes
   */
  @Test
  public void testAddMultiValuedComplexType()
  {
    AllTypes allTypes = new AllTypes(true);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes multiComplex = new AllTypes(false);
    multiComplex.setString("hello world");
    multiComplex.setNumber(Long.MAX_VALUE);
    multiComplex.setDecimal(5.8);
    allTypes.setMultiComplex(Collections.singletonList(multiComplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multiComplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(2, allTypes.getMultiComplex().size());
    Assertions.assertEquals(multiComplex, allTypes.getMultiComplex().get(0));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent());
  }

  /**
   * this test will verify that no effective change is made on a multicomplex type if the replace value is
   * identical to the previous array
   */
  @Test
  public void testReplaceMultiValuedComplexTypeWithNoChange()
  {
    AllTypes allTypes = new AllTypes(true);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes multiComplex = new AllTypes(false);
    multiComplex.setString("hello world");
    multiComplex.setNumber(Long.MAX_VALUE);
    multiComplex.setDecimal(5.8);
    allTypes.setMultiComplex(Collections.singletonList(multiComplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multiComplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(1, allTypes.getMultiComplex().size());
    Assertions.assertEquals(multiComplex, allTypes.getMultiComplex().get(0));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertFalse(allTypes.getMeta().get().getLastModified().isPresent(), allTypes.toPrettyString());
  }

  /**
   * this test will verify that a replace operation of a multicomplex node is done successfully
   */
  @Test
  public void testReplaceMultiValuedComplexTypeWithChange()
  {
    AllTypes allTypes = new AllTypes(true);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes multiComplex = new AllTypes(false);
    multiComplex.setString("hello world");
    multiComplex.setNumber(Long.MAX_VALUE);
    multiComplex.setDecimal(5.8);
    allTypes.setMultiComplex(Collections.singletonList(multiComplex));

    AllTypes allTypeChanges = new AllTypes(true);
    AllTypes multiComplex2 = new AllTypes(false);
    multiComplex2.setString("happy day");
    multiComplex2.setNumber(Long.MIN_VALUE);
    multiComplex2.setDecimal(88454.8);
    allTypeChanges.setMultiComplex(Collections.singletonList(multiComplex2));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
    Assertions.assertEquals(1, allTypes.getMultiComplex().size());
    Assertions.assertEquals(multiComplex2, allTypes.getMultiComplex().get(0));
    Assertions.assertTrue(allTypes.getMeta().isPresent());
    Assertions.assertTrue(allTypes.getMeta().get().getLastModified().isPresent(), allTypes.toPrettyString());
  }

  /**
   * verifies that an exception with an appropriate error message is thrown if the value path is unknown
   */
  @Test
  public void testAttributeDoesNotExist()
  {
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.set("unknown", new TextNode("unknown"));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail();
    }
    catch (ScimException ex)
    {
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
    AllTypes allTypes = new AllTypes(true);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes complex = new AllTypes(false);
    complex.setNumber(Long.MAX_VALUE);
    complex.setDecimal(5.8);
    allTypes.setComplex(complex);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setComplex(complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
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
    AllTypes allTypes = new AllTypes(true);
    AllTypes innerComplex = new AllTypes(false);
    innerComplex.setString("hello world");
    allTypes.setComplex(innerComplex);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes complex = new AllTypes(false);
    complex.setNumber(Long.MAX_VALUE);
    complex.setDecimal(5.8);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setComplex(complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(allTypes.getComplex().isPresent());
    Assertions.assertTrue(allTypes.getComplex().get().getString().isPresent(),
                          allTypes.getComplex().get().toPrettyString());
    Assertions.assertEquals("hello world", allTypes.getComplex().get().getString().get());
    Assertions.assertEquals(3, allTypes.size(), allTypes.toPrettyString());
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
    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
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
    AllTypes allTypes = new AllTypes(true);
    allTypes.setNumberArray(Arrays.asList(1L, 2L));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setNumberArray(Arrays.asList(3L, 4L));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
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
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setNumberArray(Arrays.asList(3L, 4L));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
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
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    multicomplex.set("unknown", new TextNode("unknown"));
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
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
      AllTypes allTypes = new AllTypes(true);
      AllTypes allTypeChanges = new AllTypes(true);
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
      PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
      Assertions.assertEquals(1, allTypes.size(), allTypes.toPrettyString());
      allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.assertTrue(allTypes.size() > 1, allTypes.toPrettyString());
      // the added attribute and lastModifed have been added by patch
      Assertions.assertEquals(3 + (nameValuePairs == null ? 0 : nameValuePairs.length), allTypes.size());
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
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(new AllTypes(true), patchOpRequest);
      Assertions.fail();
    }
    catch (ScimException ex)
    {
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
    List<String> values = Arrays.asList(new AllTypes(true).toString(), new AllTypes(true).toString());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .values(values)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(new AllTypes(true), patchOpRequest);
      Assertions.fail();
    }
    catch (ScimException ex)
    {
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

    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(allTypes.getString().isPresent());
    Assertions.assertEquals("hello world", allTypes.getString().get());
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly does not lead to a
   * {@link BadRequestException} if tried to set to the same value.
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "READ_ONLY"})
  public void testAddImmutableAndReadOnlyAttributeWithNoChange(Mutability mutability)
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta, "string", null, mutability, null, null, null, null, null, null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.assertTrue(allTypes.getString().isPresent());
      Assertions.assertEquals("hello world", allTypes.getString().get());
    }
    catch (ScimException ex)
    {
      Assertions.fail("this point must not be reached");
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly does not lead to a
   * {@link BadRequestException} if tried to set to the same value.
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "READ_ONLY"})
  public void testReplaceImmutableAndReadOnlyAttributeWithNoChange(Mutability mutability)
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta, "string", null, mutability, null, null, null, null, null, null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.assertTrue(allTypes.getString().isPresent());
      Assertions.assertEquals("hello world", allTypes.getString().get());
    }
    catch (ScimException ex)
    {
      log.warn(ex.getDetail());
      Assertions.fail("this point must not be reached");
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly leads to a
   * {@link BadRequestException} if tried to set. The immutable object should fail because it is already set
   * within this test
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "READ_ONLY"})
  public void testAddImmutableAndReadOnlyAttributeWithChange(Mutability mutability)
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta, "string", null, mutability, null, null, null, null, null, null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("new hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly leads to a
   * {@link BadRequestException} if tried to set. The immutable object should fail because it is already set
   * within this test
   */
  @ParameterizedTest
  @ValueSource(strings = {"IMMUTABLE", "READ_ONLY"})
  public void testReplaceImmutableAndReadOnlyAttributeWithChange(Mutability mutability)
  {
    JsonNode allTypesMeta = JsonHelper.loadJsonDocument(ALL_TYPES_JSON_SCHEMA);
    TestHelper.modifyAttributeMetaData(allTypesMeta, "string", null, mutability, null, null, null, null, null, null);
    resourceTypeFactory.getSchemaFactory().registerResourceSchema(allTypesMeta);

    AllTypes allTypes = new AllTypes(true);
    allTypes.setString("hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setString("new hello world");

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly leads to a
   * {@link BadRequestException} if tried to set. The immutable object should fail because it is already set
   * within this test
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

    AllTypes allTypes = new AllTypes(true);
    AllTypes innerComplex = new AllTypes(false);
    innerComplex.setString("hello world");
    allTypes.setComplex(innerComplex);
    Meta meta = Meta.builder().created(LocalDateTime.now()).build();
    allTypes.setMeta(meta);

    AllTypes complex = new AllTypes(false);
    complex.setString("new hello world");

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setComplex(complex);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly leads to a
   * {@link BadRequestException} if tried to set. The immutable object should fail because it is already set
   * within this test
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

    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(ScimType.RFC7644.MUTABILITY, ex.getScimType());
    }
  }

  /**
   * this test must verify that an attribute that has a mutability of immutable or readOnly leads to a
   * {@link BadRequestException} if tried to set. The immutable object should fail because it is already set
   * within this test
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

    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.setString("hello world");
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
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
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.set(AttributeNames.RFC7643.PRIMARY, BooleanNode.getTrue());
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Collections.singletonList(multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
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
    AllTypes allTypes = new AllTypes(true);

    AllTypes multicomplex = new AllTypes(false);
    multicomplex.set(AttributeNames.RFC7643.PRIMARY, BooleanNode.getTrue());
    allTypes.setMultiComplex(Collections.singletonList(multicomplex));

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setMultiComplex(Arrays.asList(multicomplex, multicomplex));

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    try
    {
      patchHandler.patchResource(allTypes, patchOpRequest);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
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
    AllTypes allTypes = new AllTypes(true);

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setEnterpriseUser(EnterpriseUser.builder().costCenter("something").build());

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
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

    AllTypes allTypes = new AllTypes(true);
    allTypes.setNumber(50L);

    AllTypes allTypeChanges = new AllTypes(true);
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().build();
    enterpriseUser.set(ambiguousAttributeName, new TextNode("hello world"));
    allTypeChanges.setEnterpriseUser(enterpriseUser);

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .valueNode(allTypeChanges)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
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

    AllTypes allTypes = new AllTypes(true);
    AllTypes complexAllTypes = new AllTypes(false);
    complexAllTypes.setNumber(50L);
    allTypes.setComplex(complexAllTypes);

    AllTypes allTypeChanges = new AllTypes(true);
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
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    allTypes = patchHandler.patchResource(allTypes, patchOpRequest);
    Assertions.assertTrue(allTypes.getEnterpriseUser().isPresent());
    Assertions.assertEquals("hello world", allTypes.getEnterpriseUser().get().get("complex").get("number").textValue());
  }

  /**
   * this will test that PatchHandler.isChangedResource is true if any attribute is changed, and not just if the
   * last attributes is changed.
   */
  @Test
  public void testIsChangedResourceForExtensionValueInMsAzureAdStyle()
  {
    String employeeNumber = "1111";
    String employeeNumberChanged = "2222";
    String costCenter = "2222";

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setEnterpriseUser(EnterpriseUser.builder()
                                                   .employeeNumber(employeeNumber)
                                                   .costCenter(costCenter)
                                                   .build());

    // @formatter:off
    String valueNode = "{" +
      "  \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber\": \"" +
      employeeNumberChanged+ "\"," +
      "  \"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter\": \"" +
      costCenter+ "\"" +
      "}";
    // @formatter:on

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(valueNode)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    patchHandler.patchResource(allTypeChanges, patchOpRequest);
    Assertions.assertTrue(patchHandler.isChangedResource());
  }

  /**
   * this test will make sure, that the extension is removed if a patch operation with an empty extension is
   * added as operation:
   *
   * <pre>
   * {
   *     "schemas": [
   *         "urn:ietf:params:scim:api:messages:2.0:PatchOp"
   *     ],
   *     "Operations": [
   *         {
   *             "op": "replace",
   *             "value": ["{\"urn:ietf:params:scim:schemas:extension:enterprise:2.0:User\": {}}"]
   *         }
   *     ]
   * }
   * </pre>
   */
  @Test
  public void testExtensionIsRemovedOnEmptyReplace()
  {
    String employeeNumber = "1111";
    String costCenter = "2222";

    AllTypes allTypeChanges = new AllTypes(true);
    allTypeChanges.setEnterpriseUser(EnterpriseUser.builder()
                                                   .employeeNumber(employeeNumber)
                                                   .costCenter(costCenter)
                                                   .build());

    AllTypes allTypesWithEmptyEnterpriseUser = new AllTypes(true);
    allTypesWithEmptyEnterpriseUser.setEnterpriseUser(EnterpriseUser.builder().build());
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(allTypesWithEmptyEnterpriseUser.toString())
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    AllTypes patchedAllTypes = patchHandler.patchResource(allTypeChanges, patchOpRequest);
    Assertions.assertTrue(patchHandler.isChangedResource());
    Assertions.assertFalse(patchedAllTypes.getEnterpriseUser().isPresent());
  }

  /**
   * this test will make sure that the sailspoint workaround for complex types does correctly work.
   *
   * @see https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/327
   */
  @Test
  public void testHandleReplaceOnComplexTypesAsAdd()
  {
    serviceProvider.getPatchConfig().setActivateSailsPointWorkaround(true);

    AllTypes originalResource = new AllTypes(true);
    {
      originalResource.setString("hello world");
      originalResource.setNumber(2L);
      AllTypes complex = new AllTypes(false);
      complex.setString("test");
      complex.setNumber(3L);
      complex.setStringArray(Arrays.asList("test1", "test2"));
      originalResource.setComplex(complex);
    }

    AllTypes patchValue1 = new AllTypes(true);
    {
      AllTypes complex = new AllTypes(false);
      complex.setString("new value");
      patchValue1.setComplex(complex);
    }

    AllTypes patchValue2 = new AllTypes(true);
    {
      AllTypes complex = new AllTypes(false);
      complex.setNumber(999L);
      patchValue2.setComplex(complex);
    }

    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(patchValue1.toString())
                                                                                .build(),
                                                           PatchRequestOperation.builder()
                                                                                .op(PatchOp.REPLACE)
                                                                                .value(patchValue2.toString())
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();
    PatchHandler patchHandler = new PatchHandler(serviceProvider.getPatchConfig(), allTypesResourceType);
    AllTypes patchedAllTypes = patchHandler.patchResource(originalResource, patchOpRequest);

    log.warn(patchedAllTypes.toPrettyString());

    Assertions.assertTrue(patchHandler.isChangedResource());
    Assertions.assertEquals("hello world", patchedAllTypes.getString().get());
    Assertions.assertEquals(2L, patchedAllTypes.getNumber().get());
    Assertions.assertEquals(999L, patchedAllTypes.getComplex().get().getNumber().get());
    Assertions.assertEquals("new value", patchedAllTypes.getComplex().get().getString().get());
    MatcherAssert.assertThat(patchedAllTypes.getComplex().get().getStringArray(),
                             Matchers.containsInAnyOrder("test1", "test2"));
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
