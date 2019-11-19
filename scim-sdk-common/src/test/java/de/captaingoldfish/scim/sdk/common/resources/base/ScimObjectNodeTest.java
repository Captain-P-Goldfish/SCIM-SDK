package de.captaingoldfish.scim.sdk.common.resources.base;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

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
   * validates the an all types attribute
   */
  private void validateAllType(AllTypes allTypes)
  {
    Assertions.assertEquals("chuck", allTypes.getString().get());
    Assertions.assertEquals(Integer.MAX_VALUE + 1L, allTypes.getNumber().get());
    Assertions.assertEquals(50.5, allTypes.getDecimal().get());
    Assertions.assertEquals(false, allTypes.getBool().get());
    Assertions.assertEquals(Instant.parse("1940-03-10T00:00:00Z"), allTypes.getDate().get());
    MatcherAssert.assertThat(allTypes.getStringArray(), Matchers.hasItems("hello", "world"));
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

}
