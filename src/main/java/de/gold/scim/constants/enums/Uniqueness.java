package de.gold.scim.constants.enums;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;


// @formatter:off
/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:55 <br>
 * <br>
 * A single keyword value that specifies how the service
 * provider enforces uniqueness of attribute values.  A server MAY
 * reject an invalid value based on uniqueness by returning HTTP
 * response code 400 (Bad Request).  A client MAY enforce
 * uniqueness on the client side to a greater degree than the
 * service provider enforces.  For example, a client could make a
 * value unique while the server has uniqueness of "none".  Valid
 * keywords are as follows:
 *
 * none  The values are not intended to be unique in any way.
 *       DEFAULT.
 *
 * server  The value SHOULD be unique within the context of the
 *         current SCIM endpoint (or tenancy) and MAY be globally
 *         unique (e.g., a "username", email address, or other
 *         server-generated key or counter).  No two resources on the
 *         same server SHOULD possess the same value.
 *
 * global  The value SHOULD be globally unique (e.g., an email
 *         address, a GUID, or other value).  No two resources on any
 *         server SHOULD possess the same value.
 */
// @formatter:on
public enum Uniqueness
{

  NONE("none"), SERVER("server"), GLOBAL("global");

  @Getter
  private String value;

  Uniqueness(String value)
  {
    this.value = value;
  }

  public static Uniqueness getByValue(String value)
  {
    for ( Uniqueness uniqueness : Uniqueness.values() )
    {
      if (StringUtils.equals(value, uniqueness.getValue()))
      {
        return uniqueness;
      }
    }
    return NONE;
  }
}
