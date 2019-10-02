package de.gold.scim.constants.enums;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;


// @formatter:off
/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:58 <br>
 * <br>
 * A single keyword indicating the circumstances under
 * which the value of the attribute can be (re)defined:
 *
 * readOnly  The attribute SHALL NOT be modified.
 *
 * readWrite  The attribute MAY be updated and read at any time.
 *            This is the default value.
 *
 * immutable  The attribute MAY be defined at resource creation
 *            (e.g., POST) or at record replacement via a request (e.g., a
 *            PUT).  The attribute SHALL NOT be updated.
 *
 * writeOnly  The attribute MAY be updated at any time.  Attribute
 *            values SHALL NOT be returned (e.g., because the value is a
 *            stored hash).  Note: An attribute with a mutability of
 *            "writeOnly" usually also has a returned setting of "never".
 */
// @formatter:on
public enum Mutability
{
  READ_ONLY("readOnly"), READ_WRITE("readWrite"), IMMUTABLE("immutable"), WRITE_ONLY("writeOnly");

  @Getter
  private String value;

  Mutability(String value)
  {
    this.value = value;
  }

  public static Mutability getByValue(String value)
  {
    for ( Mutability mutability : Mutability.values() )
    {
      if (StringUtils.equals(value, mutability.getValue()))
      {
        return mutability;
      }
    }
    return READ_WRITE;
  }
}
