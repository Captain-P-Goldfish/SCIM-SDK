package de.gold.scim.constants;

/**
 * author Pascal Knueppel <br>
 * created at: 04.10.2019 - 00:59 <br>
 * <br>
 */
public class ScimType
{

  /**
   * these are the scim types that have been defined by this application
   */
  public static class Custom
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
    public static final String UNPARSEABLE_REQUEST = "unparseableRequest";
  }


  /**
   * these are the scim types defined by RFC7644
   */
  public static class RFC7644
  {

    /**
     * The specified filter syntax was invalid (does not comply with Figure 1), or the specified attribute and
     * filter comparison combination is not supported.
     */
    public static final String INVALID_FILTER = "invalidFilter";

    /**
     * The specified filter yields many more results than the server is willing to calculate or process. For
     * example, a filter such as "(userName pr)" by itself would return all entries with a "userName" and MAY not
     * be acceptable to the service provider.
     */
    public static final String TOO_MANY = "tooMany";

    /**
     * One or more of the attribute values are already in use or are reserved.
     */
    public static final String UNIQUENESS = "uniqueness";

    /**
     * The attempted modification is not compatible with the target attribute's mutability or current state (e.g.,
     * modification of an "immutable" attribute with an existing value).
     */
    public static final String MUTABILITY = "mutability";

    /**
     * The request body message structure was invalid or did not conform to the request schema.
     */
    public static final String INVALID_SYNTAX = "invalidSyntax";

    /**
     * The "path" attribute was invalid or malformed (see Figure 7).<br>
     *
     * <pre>
     *     PATH = attrPath / valuePath [subAttr]
     *
     *              Figure 7: SCIM PATCH PATH Rule
     * </pre>
     *
     * <pre>
     *     Valid examples of "path" are as follows:
     *
     *        "path":"members"
     *
     *        "path":"name.familyName"
     *
     *        "path":"addresses[type eq \"work\"]"
     *
     *        "path":"members[value eq
     *               \"2819c223-7f76-453a-919d-413861904646\"]"
     *
     *        "path":"members[value eq
     *               \"2819c223-7f76-453a-919d-413861904646\"].displayName"
     *
     *                        Figure 8: Example Path Values
     * </pre>
     */
    public static final String INVALID_PATH = "invalidPath";

    /**
     * The specified "path" did not yield an attribute or attribute value that could be operated on. This occurs
     * when the specified "path" value contains a filter that yields no match.
     */
    public static final String NO_TARGET = "noTarget";

    /**
     * A required value was missing, or the value specified was not compatible with the operation or attribute
     * type (see Section 2.2 of [RFC7643]), or resource schema (see Section 4 of [RFC7643]).
     */
    public static final String INVALID_VALUE = "invalidValue";

    /**
     * The specified SCIM protocol version is not supported (see Section 3.13).
     */
    public static final String INVALID_VERSION = "invalidVers";

    /**
     * The specified request cannot be completed, due to the passing of sensitive (e.g., personal) information in
     * a request URI. For example, personal information SHALL NOT be transmitted over request URIs. See Section
     * 7.5.2.
     */
    public static final String SENSITIVE = "sensitive";
  }
}
