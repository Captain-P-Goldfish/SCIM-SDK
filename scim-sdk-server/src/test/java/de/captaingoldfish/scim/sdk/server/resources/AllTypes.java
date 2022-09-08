package de.captaingoldfish.scim.sdk.server.resources;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 23.10.2019 - 16:57 <br>
 * <br>
 * a simple scim object that is used to test the declared methods within the {@link ScimObjectNode} class
 */
@NoArgsConstructor
public class AllTypes extends ResourceNode
{

  public static final String ALL_TYPES_URI = "urn:gold:params:scim:schemas:custom:2.0:AllTypes";

  public AllTypes(boolean withSchema)
  {
    if (withSchema)
    {
      setSchemas(Arrays.asList(ALL_TYPES_URI));
    }
  }

  public Optional<String> getUserReference()
  {
    return getStringAttribute("allTypesReference");
  }

  public void setUserReference(String userReference)
  {
    setAttribute("allTypesReference", userReference);
  }

  public Optional<String> getString()
  {
    return getStringAttribute("string");
  }

  public void setString(String string)
  {
    setAttribute("string", string);
  }

  public Optional<Long> getNumber()
  {
    return getLongAttribute("number");
  }

  public void setNumber(Long newLong)
  {
    setAttribute("number", newLong);
  }

  public Optional<Double> getDecimal()
  {
    return getDoubleAttribute("decimal");
  }

  public void setDecimal(Double newDouble)
  {
    setAttribute("decimal", newDouble);
  }

  public Optional<Boolean> getBool()
  {
    return getBooleanAttribute("bool");
  }

  public void setBool(Boolean bool)
  {
    setAttribute("bool", bool);
  }

  public Optional<Instant> getDate()
  {
    return getDateTimeAttribute("date");
  }

  public void setDate(String date)
  {
    setAttribute("date", date);
  }

  public void setDate(Instant date)
  {
    setAttribute("date", date.toString());
  }

  public Optional<AllTypes> getComplex()
  {
    return getObjectAttribute("complex", AllTypes.class);
  }

  public void setComplex(AllTypes allTypes)
  {
    setAttribute("complex", allTypes);
  }

  public List<String> getStringArray()
  {
    return getSimpleArrayAttribute("stringArray");
  }

  public void setStringArray(List<String> stringArray)
  {
    setStringAttributeList("stringArray", stringArray);
  }

  public List<Long> getNumberArray()
  {
    return getSimpleArrayAttribute("numberArray", Long.class);
  }

  public void setNumberArray(List<Long> numberArray)
  {
    setAttributeList("numberArray", numberArray);
  }

  public List<Double> getDecimalArray()
  {
    return getSimpleArrayAttribute("decimalArray", Double.class);
  }

  public void setDecimalArray(List<Double> decimalArray)
  {
    setAttributeList("decimalArray", decimalArray);
  }

  public List<Boolean> getBoolArray()
  {
    return getSimpleArrayAttribute("boolArray", Boolean.class);
  }

  public void setBoolArray(List<Boolean> boolArray)
  {
    setAttributeList("boolArray", boolArray);
  }

  public List<Instant> getDateArray()
  {
    return getSimpleArrayAttribute("dateArray", Instant.class);
  }

  public void setDateArray(List<String> dateArray)
  {
    setAttributeList("dateArray", dateArray);
  }

  public void setDateArrayInstant(List<Instant> dateArray)
  {
    setAttributeList("dateArray", dateArray);
  }

  public List<AllTypes> getMultiComplex()
  {
    return getArrayAttribute("multiComplex", AllTypes.class);
  }

  public void setMultiComplex(List<AllTypes> allTypesList)
  {
    setAttribute("multiComplex", allTypesList);
  }

  public Optional<EnterpriseUser> getEnterpriseUser()
  {
    return getObjectAttribute(SchemaUris.ENTERPRISE_USER_URI, EnterpriseUser.class);
  }

  public void setEnterpriseUser(EnterpriseUser enterpriseUser)
  {
    setAttribute(SchemaUris.ENTERPRISE_USER_URI, enterpriseUser);
    Set<String> schemas = getSchemas();
    if (!schemas.contains(SchemaUris.ENTERPRISE_USER_URI))
    {
      schemas.add(SchemaUris.ENTERPRISE_USER_URI);
      setSchemas(schemas);
    }
  }
}
