package de.gold.scim.constants;

/**
 * author Pascal Knueppel <br>
 * created at: 04.10.2019 - 00:59 <br>
 * <br>
 */
public class ScimType
{

  /**
   * a required attribute is missing
   */
  public static final String REQUIRED = "required";

  /**
   * the specified resource is unknown
   */
  public static final String UNKNOWN_RESOURCE = "unknownResource";

  /**
   * a required extension is missing
   */
  public static final String MISSING_EXTENSION = "missingExtension";

  /**
   * some of the request parameters did not conform to the SCIM specification
   */
  public static final String INVALID_PARAMETERS = "invalidParameters";

  /**
   * the request was invalid and cannot be used for further processing
   */
  public static final String UNPARSABLE_REQUEST = "unparsableRequest";
}
