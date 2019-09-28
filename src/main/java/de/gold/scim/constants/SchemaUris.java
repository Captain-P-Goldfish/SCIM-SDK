package de.gold.scim.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 12:29 <br>
 * <br>
 * this class holds the constants defined by RFC7643 and RFC7644 that must be present within the
 * "schemas"-attributes of resource representations
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchemaUris
{

  public static final String SCIM_CORE_URI = "urn:ietf:params:scim:schemas:core:2.0:";

  public static final String SCIM_MESSAGES_URI = "urn:ietf:params:scim:api:messages:2.0:";

  public static final String SCHEMA_URI = SCIM_CORE_URI + "Schema";

  public static final String RESOURCE_TYPE_URI = SCIM_CORE_URI + "ResourceType";

  public static final String SERVICE_PROVIDER_CONFIG_URI = SCIM_CORE_URI + "ServiceProviderConfig";

  public static final String ENTERPRISE_USER_URI = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

  public static final String USER_URI = SCIM_CORE_URI + "User";

  public static final String GROUP_URI = SCIM_CORE_URI + "Group";

  public static final String ERROR_URI = SCIM_MESSAGES_URI + "Error";

  public static final String LIST_RESPONSE_URI = SCIM_MESSAGES_URI + "ListResponse";

  public static final String BULK_REQUEST_URI = SCIM_MESSAGES_URI + "BulkRequest";

  public static final String BULK_RESPONSE_URI = SCIM_MESSAGES_URI + "BulkResponse";


}
