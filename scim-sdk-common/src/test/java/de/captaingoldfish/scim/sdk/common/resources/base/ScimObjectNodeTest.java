package de.captaingoldfish.scim.sdk.common.resources.base;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.exceptions.IncompatibleAttributeException;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.resources.AllTypes;
import de.captaingoldfish.scim.sdk.common.utils.FileReferences;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.common.utils.TimeUtils;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 01:33 <br>
 * <br>
 */
public class ScimObjectNodeTest implements FileReferences
{

  /**
   * will test for each node type that was defined in the RFC7643 that the types can successfully be extracted
   * and compared
   */
  @Test
  public void testAllDifferentNodeTypesFromDocument()
  {
    AllTypes allTypes = JsonHelper.loadJsonDocument(FileReferences.ALL_TYPES_JSON, AllTypes.class);
    validateAllType(allTypes);

    AllTypes complex = allTypes.getComplex().get();
    validateAllType(complex);

    List<AllTypes> multiComplex = allTypes.getMultiComplex();
    MatcherAssert.assertThat(multiComplex.size(), Matchers.greaterThan(0));
    multiComplex.forEach(this::validateAllType);
  }

  /**
   * validates an all types attribute
   */
  private void validateAllType(AllTypes allTypes)
  {
    Assertions.assertEquals("chuck", allTypes.getString().get());
    Assertions.assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8),
                                 allTypes.getBinary().get(),
                                 new String(allTypes.getBinary().get()));
    Assertions.assertEquals(Integer.MAX_VALUE + 1L, allTypes.getNumber().get());
    Assertions.assertEquals(50.5, allTypes.getDecimal().get());
    Assertions.assertEquals(false, allTypes.getBool().get());
    Assertions.assertEquals(Instant.parse("1940-03-10T00:00:00Z"), allTypes.getDate().get());
    MatcherAssert.assertThat(allTypes.getStringArray(), Matchers.hasItems("hello", "world"));
    MatcherAssert.assertThat(allTypes.getBinaryArray(), Matchers.hasItems("hello".getBytes(), "world".getBytes()));
    MatcherAssert.assertThat(allTypes.getNumberArray(), Matchers.hasItems(44L, 55L, 66L));
    MatcherAssert.assertThat(allTypes.getDecimalArray(), Matchers.hasItems(4.7, 5.1, 6.2));
    Instant[] dateTimes = Stream.of("1976-03-10T00:00:00Z", "1986-03-10T00:00:00Z", "1996-03-10T00:00:00Z")
                                .map(TimeUtils::parseDateTime)
                                .toArray(Instant[]::new);
    MatcherAssert.assertThat(allTypes.getDateArray(), Matchers.hasItems(dateTimes));
    MatcherAssert.assertThat(allTypes.getBoolArray(), Matchers.contains(true, true, false));
  }

  /**
   * test that an empty is returned if the node to extract is not present
   */
  @Test
  public void testGetNonExistentNode()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    Assertions.assertFalse(scimObjectNode.getStringAttribute("unknown").isPresent());
    Assertions.assertFalse(scimObjectNode.getBooleanAttribute("unknown").isPresent());
    Assertions.assertFalse(scimObjectNode.getLongAttribute("unknown").isPresent());
    Assertions.assertFalse(scimObjectNode.getDoubleAttribute("unknown").isPresent());
    Assertions.assertFalse(scimObjectNode.getDateTimeAttribute("unknown").isPresent());
    Assertions.assertFalse(scimObjectNode.getObjectAttribute("unknown", AllTypes.class).isPresent());
    Assertions.assertNull(scimObjectNode.get("unknown"));
    Assertions.assertTrue(scimObjectNode.getArrayAttribute("unknown", AllTypes.class).isEmpty());
  }

  /**
   * verifies that an exception is thrown if a simple node is tries to be extracted as object node
   */
  @Test
  public void testExtractSimpleNodeAsObjectNode()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    scimObjectNode.setAttribute(attributeName, 9L);
    Assertions.assertThrows(InternalServerException.class,
                            () -> scimObjectNode.getObjectAttribute(attributeName, AllTypes.class));
  }

  /**
   * verifies that an exception is thrown if a simple node is tries to be extracted as array node
   */
  @Test
  public void testExtractSimpleNodeAsArrayNode()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    scimObjectNode.setAttribute(attributeName, 9L);
    Assertions.assertThrows(InternalServerException.class,
                            () -> scimObjectNode.getArrayAttribute(attributeName, AllTypes.class));
  }

  /**
   * verifies that an attribute can be removed if null is given as parameter on the set-method
   */
  @Test
  public void testRemoveAttributeInteger()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    scimObjectNode.setAttribute(attributeName, 9L);
    Assertions.assertEquals(9, scimObjectNode.getLongAttribute(attributeName).get());
    scimObjectNode.setAttribute(attributeName, (Long)null);
    Assertions.assertNull(scimObjectNode.get(attributeName));
    Assertions.assertFalse(scimObjectNode.getLongAttribute(attributeName).isPresent());
  }

  /**
   * verifies that an attribute can be removed if null is given as parameter on the set-method
   */
  @Test
  public void testRemoveAttributeDouble()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    scimObjectNode.setAttribute(attributeName, 9.0);
    Assertions.assertEquals(9.0, scimObjectNode.getDoubleAttribute(attributeName).get());
    scimObjectNode.setAttribute(attributeName, (Double)null);
    Assertions.assertNull(scimObjectNode.get(attributeName));
    Assertions.assertFalse(scimObjectNode.getLongAttribute(attributeName).isPresent());
  }

  /**
   * verifies that an attribute can be removed if null is given as parameter on the set-method
   */
  @Test
  public void testRemoveAttributeBoolean()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    scimObjectNode.setAttribute(attributeName, true);
    Assertions.assertEquals(true, scimObjectNode.getBooleanAttribute(attributeName).get());
    scimObjectNode.setAttribute(attributeName, (Boolean)null);
    Assertions.assertNull(scimObjectNode.get(attributeName));
    Assertions.assertFalse(scimObjectNode.getLongAttribute(attributeName).isPresent());
  }

  /**
   * verifies that an attribute can be removed if null is given as parameter on the set-method
   */
  @Test
  public void testRemoveAttributeString()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    scimObjectNode.setAttribute(attributeName, "chuck");
    Assertions.assertEquals("chuck", scimObjectNode.getStringAttribute(attributeName).get());
    scimObjectNode.setAttribute(attributeName, (String)null);
    Assertions.assertNull(scimObjectNode.get(attributeName));
    Assertions.assertFalse(scimObjectNode.getLongAttribute(attributeName).isPresent());
  }

  /**
   * verifies that an attribute can be removed if null is given as parameter on the set-method
   */
  @Test
  public void testRemoveAttributeInstant()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    final Instant instant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    scimObjectNode.setDateTimeAttribute(attributeName, instant);
    Assertions.assertEquals(instant, scimObjectNode.getDateTimeAttribute(attributeName).get());
    scimObjectNode.setDateTimeAttribute(attributeName, (Instant)null);
    Assertions.assertNull(scimObjectNode.get(attributeName));
    Assertions.assertFalse(scimObjectNode.getLongAttribute(attributeName).isPresent());
  }

  /**
   * verifies that an Instant attribute can be set with and without nanos and keeps a strict String
   * representation for both types format @see https://tools.ietf.org/html/rfc7643#section-2.3.5
   */
  @Test
  public void testAttributeInstantStoringFractionals()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";

    int fractionalDigits = TimeUtils.DEFAULT_INSTANT_FRACTIONAL_DIGITS_FORMAT;
    DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant(fractionalDigits).toFormatter();
    Instant now = Instant.now();
    Instant instantWithZeroNanos = now.minus(now.getNano(), ChronoUnit.NANOS);
    Instant instantWithOneFractional = instantWithZeroNanos.plusNanos(1_000_000L);

    scimObjectNode.setDateTimeAttribute(attributeName, instantWithZeroNanos);
    Assertions.assertEquals(instantWithZeroNanos, scimObjectNode.getDateTimeAttribute(attributeName).get());
    Assertions.assertNotEquals(instantWithZeroNanos.toString(), scimObjectNode.getStringAttribute(attributeName).get());
    Assertions.assertEquals(formatter.format(instantWithZeroNanos),
                            scimObjectNode.getStringAttribute(attributeName).get());

    scimObjectNode.setDateTimeAttribute(attributeName, instantWithOneFractional);
    Assertions.assertEquals(instantWithOneFractional, scimObjectNode.getDateTimeAttribute(attributeName).get());
    Assertions.assertEquals(instantWithOneFractional.toString(),
                            scimObjectNode.getStringAttribute(attributeName).get());
    Assertions.assertEquals(formatter.format(instantWithOneFractional),
                            scimObjectNode.getStringAttribute(attributeName).get());

    fractionalDigits = 0;
    formatter = new DateTimeFormatterBuilder().appendInstant(fractionalDigits).toFormatter();
    scimObjectNode.setDateTimeAttribute(attributeName, instantWithZeroNanos, fractionalDigits);
    Assertions.assertEquals(instantWithZeroNanos, scimObjectNode.getDateTimeAttribute(attributeName).get());
    Assertions.assertEquals(instantWithZeroNanos.toString(), scimObjectNode.getStringAttribute(attributeName).get());
    Assertions.assertEquals(formatter.format(instantWithZeroNanos),
                            scimObjectNode.getStringAttribute(attributeName).get());

    scimObjectNode.setDateTimeAttribute(attributeName, instantWithOneFractional, fractionalDigits);
    Assertions.assertNotEquals(instantWithOneFractional, scimObjectNode.getDateTimeAttribute(attributeName).get());
    Assertions.assertNotEquals(instantWithOneFractional.toString(),
                               scimObjectNode.getStringAttribute(attributeName).get());
    Assertions.assertEquals(formatter.format(instantWithOneFractional),
                            scimObjectNode.getStringAttribute(attributeName).get());
  }


  /**
   * verifies that an attribute can be removed if null is given as parameter on the set-method
   */
  @Test
  public void testRemoveAttributeLocalDateTime()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    final LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    scimObjectNode.setDateTimeAttribute(attributeName, localDateTime);
    Assertions.assertEquals(localDateTime.atZone(ZoneId.systemDefault()).toInstant(),
                            scimObjectNode.getDateTimeAttribute(attributeName).get());
    scimObjectNode.setDateTimeAttribute(attributeName, (LocalDateTime)null);
    Assertions.assertNull(scimObjectNode.get(attributeName));
    Assertions.assertFalse(scimObjectNode.getLongAttribute(attributeName).isPresent());
  }

  /**
   * verifies that an attribute can be removed if null is given as parameter on the set-method
   */
  @Test
  public void testRemoveAttributeOffsetDateTime()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    final OffsetDateTime offsetDateTime = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    scimObjectNode.setDateTimeAttribute(attributeName, offsetDateTime);
    Assertions.assertEquals(offsetDateTime.toInstant(), scimObjectNode.getDateTimeAttribute(attributeName).get());
    scimObjectNode.setDateTimeAttribute(attributeName, (OffsetDateTime)null);
    Assertions.assertNull(scimObjectNode.get(attributeName));
    Assertions.assertFalse(scimObjectNode.getLongAttribute(attributeName).isPresent());
  }

  /**
   * verifies that an attribute can be removed if null is given as parameter on the set-method
   */
  @Test
  public void testRemoveAttributeJsonNode()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    JsonNode simpleNode = new TextNode("test");
    scimObjectNode.addAttribute(attributeName, simpleNode);
    Assertions.assertNotNull(scimObjectNode.get(attributeName));
    Assertions.assertTrue(scimObjectNode.get(attributeName).isArray());
    scimObjectNode.addAttribute(attributeName, null);
    Assertions.assertNotNull(scimObjectNode.get(attributeName));
    Assertions.assertTrue(scimObjectNode.get(attributeName).isArray());
  }

  /**
   * will verify that a string type attribute can be extracted from {@link ScimObjectNode} if the
   * {@link ScimTextNode} inheritance has a method with name newInstance and a single string parameter
   */
  @Test
  public void testAddAndGetStringNodeAttributeByType()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    final String tag = "123&/()";
    scimObjectNode.set(attributeName, new TextNode("W/\"" + tag + "\""));
    ETag eTag = scimObjectNode.getStringAttribute(attributeName, ETag.class)
                              .orElseThrow(() -> new IllegalStateException("fail"));
    Assertions.assertEquals(tag, eTag.getTag());
    Assertions.assertTrue(eTag.isWeak());
  }

  /**
   * verifies that attributes set to JSON null are recognized
   */
  @Test
  public void testRecognizeNullValue() throws JsonProcessingException
  {
    JsonNode jsonNode = new ObjectMapper().readTree("{\"attr\": null}");
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    scimObjectNode.setAll((ObjectNode)jsonNode);
    final String attributeName = "attr";

    Assertions.assertFalse(scimObjectNode.getObjectAttribute(attributeName, AllTypes.class).isPresent());

    Assertions.assertFalse(scimObjectNode.getStringAttribute(attributeName).isPresent());

    Assertions.assertTrue(scimObjectNode.getArrayAttribute(attributeName, AllTypes.class).isEmpty());

    Assertions.assertTrue(scimObjectNode.getSimpleArrayAttribute(attributeName, String.class).isEmpty());

    Assertions.assertTrue(scimObjectNode.getSimpleArrayAttributeSet(attributeName, String.class).isEmpty());
  }

  /**
   * makes sure that simple binary data can successfully be retrieved from a {@link ScimObjectNode}
   */
  @Test
  public void testReadBinaryNodeData()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    byte[] binaryData = "hello world".getBytes();
    String base64Data = Base64.getEncoder().encodeToString(binaryData);
    scimObjectNode.setAttribute(attributeName, base64Data);

    byte[] retrievedBinaryData = scimObjectNode.getBinaryAttribute(attributeName).get();
    Assertions.assertArrayEquals(binaryData, retrievedBinaryData);
  }

  /**
   * makes sure that binary array data can successfully be retrieved from a {@link ScimObjectNode}
   */
  @Test
  public void testReadBinaryNodeDataFromArray()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    byte[] binaryData = "hello world".getBytes();
    JsonNode base64Data = new TextNode(Base64.getEncoder().encodeToString(binaryData));
    scimObjectNode.addAttribute(attributeName, base64Data);

    List<byte[]> retrievedBinaryData = scimObjectNode.getSimpleArrayAttribute(attributeName, byte[].class);
    Assertions.assertEquals(1, retrievedBinaryData.size());
    Assertions.assertArrayEquals(binaryData, retrievedBinaryData.get(0));
  }

  /**
   * makes sure that binary array data can successfully be retrieved from a {@link ScimObjectNode}
   */
  @Test
  public void testReadBrokenBinaryNodeDataFromArray()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    byte[] binaryData = "hello world".getBytes();
    JsonNode base64Data = new TextNode(Base64.getEncoder().encodeToString(binaryData) + "__öüä");
    scimObjectNode.addAttribute(attributeName, base64Data);

    IncompatibleAttributeException ex = Assertions.assertThrows(IncompatibleAttributeException.class,
                                                                () -> scimObjectNode.getSimpleArrayAttribute(attributeName,
                                                                                                             byte[].class));
    ex.printStackTrace();
    Assertions.assertEquals("The value of node with name 'attr' is not of type binary. "
                            + "Illegal base64 encoded data",
                            ex.getMessage());
    Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
  }

  /**
   * makes sure that a binary array can be added to a {@link ScimObjectNode}
   */
  @Test
  public void testAddBinaryArrayNodeToScimNode()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    byte[] binaryData = "hello world".getBytes();
    List<byte[]> binaryList = Arrays.asList(binaryData);
    scimObjectNode.setAttributeList(attributeName, binaryList);

    List<byte[]> retrievedBinaryData = scimObjectNode.getSimpleArrayAttribute(attributeName, byte[].class);
    Assertions.assertEquals(1, retrievedBinaryData.size());
    Assertions.assertArrayEquals(binaryData, retrievedBinaryData.get(0));
  }

  /**
   * makes sure that an appropriate exception is thrown if an attribute that should contain binary data does not
   * contain a valid base64 encoded structure
   */
  @Test
  public void testReadBinaryNodeWithBrokenBase64Data()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    byte[] binaryData = "hello world".getBytes();
    String base64Data = Base64.getEncoder().encodeToString(binaryData) + "__öüä";
    scimObjectNode.setAttribute(attributeName, base64Data);

    IncompatibleAttributeException ex = Assertions.assertThrows(IncompatibleAttributeException.class,
                                                                () -> scimObjectNode.getBinaryAttribute(attributeName)
                                                                                    .get());
    Assertions.assertEquals("attribute value 'aGVsbG8gd29ybGQ=__öüä' is not of type BINARY", ex.getMessage());
    Assertions.assertEquals(ScimType.RFC7644.INVALID_VALUE, ex.getScimType());
  }

}
