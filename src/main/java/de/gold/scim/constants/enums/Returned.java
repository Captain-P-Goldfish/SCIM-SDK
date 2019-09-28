package de.gold.scim.constants.enums;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;


// @formatter:off
/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 17:57 <br>
 * <br>
 * A single keyword that indicates when an attribute and
 * associated values are returned in response to a GET request or
 * in response to a PUT, POST, or PATCH request.  Valid keywords
 * are as follows:
 *
 * always  The attribute is always returned, regardless of the
 *         contents of the "attributes" parameter.  For example, "id"
 *         is always returned to identify a SCIM resource.
 *
 * never  The attribute is never returned.  This may occur because
 *        the original attribute value (e.g., a hashed value) is not
 *        retained by the service provider.  A service provider MAY
 *        allow attributes to be used in a search filter.
 * default  The attribute is returned by default in all SCIM
 *          operation responses where attribute values are returned.  If
 *          the GET request "attributes" parameter is specified,
 *          attribute values are only returned if the attribute is named
 *          in the "attributes" parameter.  DEFAULT.
 * 
 * request  The attribute is returned in response to any PUT,
 *          POST, or PATCH operations if the attribute was specified by
 *          the client (for example, the attribute was modified).  The
 *          attribute is returned in a SCIM query operation only if
 *          specified in the "attributes" parameter.
 */
// @formatter:on
public enum Returned
{
  DEFAULT("default"), ALWAYS("always"), NEVER("never"), REQUEST("request");

  @Getter
  private String value;

  private Returned(String value)
  {
    this.value = value;
  }

  public static Returned getByValue(String value)
  {
    for ( Returned returned : Returned.values() )
    {
      if (StringUtils.equals(value, returned.getValue()))
      {
        return returned;
      }
    }
    return DEFAULT;
  }
}
