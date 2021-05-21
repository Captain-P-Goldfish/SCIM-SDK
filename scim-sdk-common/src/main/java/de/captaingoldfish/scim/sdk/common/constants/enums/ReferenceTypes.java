package de.captaingoldfish.scim.sdk.common.constants.enums;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;


// @formatter:off
/**
 * author Pascal Knueppel <br>
 * created at: 29.09.2019 - 13:27 <br>
 * <br>
 * A multi-valued array of JSON strings that indicate
 * the SCIM resource types that may be referenced.  Valid values
 * are as follows:
 *
 * +  A SCIM resource type (e.g., "User" or "Group"),
 *
 * +  "external" - indicating that the resource is an external
 *    resource (e.g., a photo), or
 *
 * +  "uri" - indicating that the reference is to a service
 *    endpoint or an identifier (e.g., a schema URN).
 *
 * This attribute is only applicable for attributes that are of
 * type "reference" (Section 2.3.7).
 *  
 *
 * NOTE:
 * the type "URL" is a customized type that is not part of RFC7643
 */
// @formatter:on
public enum ReferenceTypes
{

  RESOURCE("resource"), EXTERNAL("external"), URI("uri"), URL("url");

  @Getter
  private String value;

  ReferenceTypes(String value)
  {
    this.value = value;
  }

  public static ReferenceTypes getByValue(String value)
  {
    for ( ReferenceTypes referenceTypes : ReferenceTypes.values() )
    {
      if (StringUtils.equalsIgnoreCase(value, referenceTypes.getValue()))
      {
        return referenceTypes;
      }
    }
    return EXTERNAL;
  }
}
