package de.gold.scim.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 15:09 <br>
 * <br>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttributeNames
{

  /**
   * this class holds all additional custom attributes defined by this implementation
   */
  public static class Custom
  {

    /**
     * a custom attribute introduced for the {@link de.gold.scim.resources.ServiceProviderUrlExtension}
     */
    public static final String BASE_URL = "baseUrl";
  }

  /**
   * this class holds the attributes defined by RFC7643
   */
  public static class RFC7643
  {

    public static final String EXCLUDED_ATTRIBUTES = "excludedAttributes";

    public static final String SORT_ORDER = "sortOrder";

    public static final String SORT_BY = "sortBy";

    public static final String COUNT = "count";

    public static final String AUTHENTICATION_SCHEMES = "authenticationSchemes";

    public static final String ETAG = "etag";

    public static final String SORT = "sort";

    public static final String CHANGE_PASSWORD = "changePassword";

    public static final String FILTER = "filter";

    public static final String BULK = "bulk";

    public static final String PATCH = "patch";

    public static final String MAX_RESULTS = "maxResults";

    public static final String DOCUMENTATION_URI = "documentationUri";

    public static final String SPEC_URI = "specUri";

    public static final String MAX_PAYLOAD_SIZE = "maxPayloadSize";

    public static final String MAX_OPERATIONS = "maxOperations";

    public static final String SUPPORTED = "supported";

    public static final String RESOURCES = "Resources";

    public static final String START_INDEX = "startIndex";

    public static final String ITEMS_PER_PAGE = "itemsPerPage";

    public static final String TOTAL_RESULTS = "totalResults";

    public static final String MANAGER = "manager";

    public static final String DEPARTMENT = "department";

    public static final String DIVISION = "division";

    public static final String ORGANIZATION = "organization";

    public static final String COST_CENTER = "costCenter";

    public static final String EMPLOYEE_NUMBER = "employeeNumber";

    public static final String MEMBERS = "members";

    public static final String X509_CERTIFICATES = "x509Certificates";

    public static final String ROLES = "roles";

    public static final String ENTITLEMENTS = "entitlements";

    public static final String ADDRESSES = "addresses";

    public static final String PHOTOS = "photos";

    public static final String IMS = "ims";

    public static final String PHONE_NUMBERS = "phoneNumbers";

    public static final String REF = "$ref";

    public static final String VALUE = "value";

    public static final String COUNTRY = "country";

    public static final String POSTAL_CODE = "postalCode";

    public static final String REGION = "region";

    public static final String STREET_ADDRESS = "streetAddress";

    public static final String ID = "id";

    public static final String NAME = "name";

    public static final String DESCRIPTION = "description";

    public static final String ATTRIBUTES = "attributes";

    public static final String TYPE = "type";

    public static final String MULTI_VALUED = "multiValued";

    public static final String REQUIRED = "required";

    public static final String CASE_EXACT = "caseExact";

    public static final String MUTABILITY = "mutability";

    public static final String RETURNED = "returned";

    public static final String UNIQUENESS = "uniqueness";

    public static final String CANONICAL_VALUES = "canonicalValues";

    public static final String SUB_ATTRIBUTES = "subAttributes";

    public static final String SCHEMAS = "schemas";

    public static final String REFERENCE_TYPES = "referenceTypes";

    public static final String SCHEMA = "schema";

    public static final String SCHEMA_EXTENSIONS = "schemaExtensions";

    public static final String ENDPOINT = "endpoint";

    public static final String META = "meta";

    public static final String PASSWORD = "password";

    public static final String DISPLAY = "display";

    public static final String GROUPS = "groups";

    public static final String USER_NAME = "userName";

    public static final String EMAILS = "emails";

    public static final String PRIMARY = "primary";

    public static final String EXTERNAL_ID = "externalId";

    public static final String RESOURCE_TYPE = "resourceType";

    public static final String CREATED = "created";

    public static final String LAST_MODIFIED = "lastModified";

    public static final String LOCATION = "location";

    public static final String VERSION = "version";

    public static final String FORMATTED = "formatted";

    public static final String FAMILY_NAME = "familyName";

    public static final String GIVEN_NAME = "givenName";

    public static final String MIDDLE_NAME = "middleName";

    public static final String HONORIFIC_PREFIX = "honorificPrefix";

    public static final String HONORIFIC_SUFFIX = "honorificSuffix";

    public static final String DISPLAY_NAME = "displayName";

    public static final String NICK_NAME = "nickName";

    public static final String PROFILE_URL = "profileUrl";

    public static final String TITLE = "title";

    public static final String USER_TYPE = "userType";

    public static final String PREFERRED_LANGUAGE = "preferredLanguage";

    public static final String LOCALE = "locale";

    public static final String TIMEZONE = "timezone";

    public static final String ACTIVE = "active";

    public static final String LOCALITY = "locality";
  }

}


