package de.gold.scim.resources.base;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.gold.scim.exceptions.InternalServerException;
import de.gold.scim.resources.complex.Name;
import de.gold.scim.resources.multicomplex.PhoneNumber;
import de.gold.scim.utils.FileReferences;
import de.gold.scim.utils.JsonHelper;


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
    Person person = JsonHelper.loadJsonDocument(ALL_TYPES_JSON, Person.class);
    Assertions.assertEquals("chuck", person.getUserName().get());
    Assertions.assertEquals(79, person.getNumber().get());
    Assertions.assertEquals(50.5, person.getDecimal().get());
    Assertions.assertEquals(false, person.isSingle().get());
    Assertions.assertEquals("1940-03-10T00:00:00Z", person.getBirthDate().get().toString());

    Name name = Name.builder()
                    .formatted("Sir Carlos Ray Norris")
                    .familyName("Norris")
                    .givenName("Carlos")
                    .middlename("Ray")
                    .honorificPrefix("Sir")
                    .build();
    Assertions.assertEquals(name, person.getNameNode().get());

    List<PhoneNumber> phoneNumbers = new ArrayList<>();
    phoneNumbers.add(PhoneNumber.builder().value("111-111-111111").type("home").build());
    phoneNumbers.add(PhoneNumber.builder().value("222-222-222222").type("work").build());
    phoneNumbers.add(PhoneNumber.builder().value("333-333-333333").type("mobile").primary(true).build());
    Assertions.assertEquals(phoneNumbers, person.getPhoneNumbers());
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
    Assertions.assertFalse(scimObjectNode.getIntegerAttribute("unknown").isPresent());
    Assertions.assertFalse(scimObjectNode.getDoubleAttribute("unknown").isPresent());
    Assertions.assertFalse(scimObjectNode.getDateTimeAttribute("unknown").isPresent());
    Assertions.assertFalse(scimObjectNode.getObjectAttribute("unknown", Person.class).isPresent());
    Assertions.assertNull(scimObjectNode.get("unknown"));
    Assertions.assertTrue(scimObjectNode.getArrayAttribute("unknown", Person.class).isEmpty());
  }

  /**
   * verifies that an exception is thrown if a simple node is tries to be extracted as object node
   */
  @Test
  public void testExtractSimpleNodeAsObjectNode()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    scimObjectNode.setAttribute(attributeName, 9);
    Assertions.assertThrows(InternalServerException.class,
                            () -> scimObjectNode.getObjectAttribute(attributeName, Person.class));
  }

  /**
   * verifies that an exception is thrown if a simple node is tries to be extracted as array node
   */
  @Test
  public void testExtractSimpleNodeAsArrayNode()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    scimObjectNode.setAttribute(attributeName, 9);
    Assertions.assertThrows(InternalServerException.class,
                            () -> scimObjectNode.getArrayAttribute(attributeName, Person.class));
  }

  /**
   * verifies that an attribute can be removed if null is given as parameter on the set-method
   */
  @Test
  public void testRemoveAttributeInteger()
  {
    ScimObjectNode scimObjectNode = new ScimObjectNode(null);
    final String attributeName = "attr";
    scimObjectNode.setAttribute(attributeName, 9);
    Assertions.assertEquals(9, scimObjectNode.getIntegerAttribute(attributeName).get());
    scimObjectNode.setAttribute(attributeName, (Integer)null);
    Assertions.assertNull(scimObjectNode.get(attributeName));
    Assertions.assertFalse(scimObjectNode.getIntegerAttribute(attributeName).isPresent());
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
    Assertions.assertFalse(scimObjectNode.getIntegerAttribute(attributeName).isPresent());
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
    Assertions.assertFalse(scimObjectNode.getIntegerAttribute(attributeName).isPresent());
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
    Assertions.assertFalse(scimObjectNode.getIntegerAttribute(attributeName).isPresent());
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
    Assertions.assertFalse(scimObjectNode.getIntegerAttribute(attributeName).isPresent());
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
    Assertions.assertFalse(scimObjectNode.getIntegerAttribute(attributeName).isPresent());
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
    Assertions.assertEquals(offsetDateTime.toInstant(),
                            scimObjectNode.getDateTimeAttribute(attributeName).get());
    scimObjectNode.setDateTimeAttribute(attributeName, (OffsetDateTime)null);
    Assertions.assertNull(scimObjectNode.get(attributeName));
    Assertions.assertFalse(scimObjectNode.getIntegerAttribute(attributeName).isPresent());
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
   * a simple scim object that is used to test the declared methods within the {@link ScimObjectNode} class
   */
  public static class Person extends ScimObjectNode
  {

    public Person()
    {
      super(null);
    }

    public Optional<String> getUserName()
    {
      return getStringAttribute("userName");
    }

    public void setUserName(String userName)
    {
      setAttribute("userName", userName);
    }

    public Optional<Integer> getNumber()
    {
      return getIntegerAttribute("number");
    }

    public void setNumber(Integer number)
    {
      setAttribute("number", number);
    }

    public Optional<Double> getDecimal()
    {
      return getDoubleAttribute("decimal");
    }

    public void setDecimal(Double decimal)
    {
      setAttribute("decimal", decimal);
    }

    public Optional<Boolean> isSingle()
    {
      return getBooleanAttribute("single");
    }

    public void isSingle(Boolean single)
    {
      setAttribute("single", single);
    }

    public Optional<Instant> getBirthDate()
    {
      return getDateTimeAttribute("birthDate");
    }

    public void setBirthDate(Instant birthDate)
    {
      setDateTimeAttribute("birthDate", birthDate);
    }

    public void setBirthDate(LocalDateTime birthDate)
    {
      setDateTimeAttribute("birthDate", birthDate);
    }

    public void setBirthDate(OffsetDateTime birthDate)
    {
      setDateTimeAttribute("birthDate", birthDate);
    }

    public Optional<Name> getNameNode()
    {
      return getObjectAttribute("name", Name.class);
    }

    public void setNameNode(Name name)
    {
      setAttribute("name", name);
    }

    public List<PhoneNumber> getPhoneNumbers()
    {
      return getArrayAttribute("phoneNumbers", PhoneNumber.class);
    }

    public void getPhoneNumbers(List<PhoneNumber> phoneNumbers)
    {
      setAttribute("phoneNumbers", phoneNumbers);
    }
  }
}
