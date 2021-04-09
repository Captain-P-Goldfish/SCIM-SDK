package de.captaingoldfish.scim.sdk.common.constants.enums;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.exceptions.UnknownValueException;
import lombok.Getter;


// @formatter:off
/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 18:00 <br>
 * <br>
 * The attribute's data type.  Valid values are "string",
 * "boolean", "decimal", "integer", "dateTime", "reference", and
 * "complex".  When an attribute is of type "complex", there
 * SHOULD be a corresponding schema attribute "subAttributes"
 * defined, listing the sub-attributes of the attribute.
 */
// @formatter:on
public enum Type
{

  ANY("any"),
  STRING("string"),
  COMPLEX("complex"),
  BOOLEAN("boolean"),
  DECIMAL("decimal"),
  INTEGER("integer"),
  DATE_TIME("dateTime"),
  REFERENCE("reference");

  @Getter
  private String value;

  Type(String value)
  {
    this.value = value;
  }


  public static Type getByValue(String value)
  {
    for ( Type type : Type.values() )
    {
      if (StringUtils.equalsIgnoreCase(value, type.getValue()))
      {
        return type;
      }
    }
    throw new UnknownValueException("value '" + value + "' could not be resolved to type: " + Type.class, null, null,
                                    null);
  }
}
