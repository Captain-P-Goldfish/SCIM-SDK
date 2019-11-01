package de.gold.scim.server.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 00:10 <br>
 * <br>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClassPathReferences
{

  public static final String META_RESOURCE_SCHEMA_JSON = "/de/gold/scim/server/meta/schema.schema.json";

  public static final String META_RESOURCE_TYPES_JSON = "/de/gold/scim/server/meta/resource-type.schema.json";

  public static final String RESOURCE_TYPES_FILTER_EXT_JSON = "/de/gold/scim/server/meta/resource-type-filter-ext.json";

  public static final String META_SERVICE_PROVIDER_JSON = "/de/gold/scim/server/meta/service-provider.schema.json";

  public static final String USER_RESOURCE_TYPE_JSON = "/de/gold/scim/server/resourcetypes/user.json";

  public static final String GROUP_RESOURCE_TYPE_JSON = "/de/gold/scim/server/resourcetypes/group.json";

  public static final String ME_RESOURCE_TYPE_JSON = "/de/gold/scim/server/resourcetypes/me.json";

  public static final String RESOURCE_TYPE_RESOURCE_TYPE_JSON = "/de/gold/scim/server/resourcetypes/resource-type.json";

  public static final String USER_SCHEMA_JSON = "/de/gold/scim/server/schemas/users.json";

  public static final String ENTERPRISE_USER_SCHEMA_JSON = "/de/gold/scim/server/schemas/enterprise-user.json";

  public static final String GROUP_SCHEMA_JSON = "/de/gold/scim/server/schemas/groups.json";

  public static final String META_SCHEMA_JSON = "/de/gold/scim/server/meta/meta.schema.json";

  public static final String SERVICE_PROVIDER_RESOURCE_TYPE_JSON = "/de/gold/scim/server/resourcetypes/service-provider.json";

  public static final String SCHEMA_RESOURCE_TYPE_JSON = "/de/gold/scim/server/resourcetypes/schema.json";

  public static final String BULK_REQUEST_SCHEMA = "/de/gold/scim/server/request/bulk-request.json";

  public static final String BULK_RESPONSE_SCHEMA = "/de/gold/scim/server/response/bulk-response.json";

  public static final String PATCH_REQUEST_SCHEMA = "/de/gold/scim/server/request/patch-request.json";
}
