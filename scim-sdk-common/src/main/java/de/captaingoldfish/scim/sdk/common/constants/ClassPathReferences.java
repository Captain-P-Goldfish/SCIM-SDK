package de.captaingoldfish.scim.sdk.common.constants;

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

  public static final String META_RESOURCE_SCHEMA_JSON = "/de/captaingoldfish/scim/sdk/common/meta/schema.schema.json";

  public static final String META_RESOURCE_TYPES_JSON = "/de/captaingoldfish/scim/sdk/common/meta/resource-type.schema.json";

  public static final String RESOURCE_TYPES_FEATURE_EXT_JSON = "/de/captaingoldfish/scim/sdk/common/meta/resource-type-feature-ext.json";

  public static final String META_SERVICE_PROVIDER_JSON = "/de/captaingoldfish/scim/sdk/common/meta/service-provider.schema.json";

  public static final String USER_RESOURCE_TYPE_JSON = "/de/captaingoldfish/scim/sdk/common/resourcetypes/user.json";

  public static final String GROUP_RESOURCE_TYPE_JSON = "/de/captaingoldfish/scim/sdk/common/resourcetypes/group.json";

  public static final String ME_RESOURCE_TYPE_JSON = "/de/captaingoldfish/scim/sdk/common/resourcetypes/me.json";

  public static final String RESOURCE_TYPE_RESOURCE_TYPE_JSON = "/de/captaingoldfish/scim/sdk/common/resourcetypes/resource-type.json";

  public static final String USER_SCHEMA_JSON = "/de/captaingoldfish/scim/sdk/common/schemas/users.json";

  public static final String ENTERPRISE_USER_SCHEMA_JSON = "/de/captaingoldfish/scim/sdk/common/schemas/enterprise-user.json";

  public static final String GROUP_SCHEMA_JSON = "/de/captaingoldfish/scim/sdk/common/schemas/groups.json";

  public static final String META_SCHEMA_JSON = "/de/captaingoldfish/scim/sdk/common/meta/meta.schema.json";

  public static final String SERVICE_PROVIDER_RESOURCE_TYPE_JSON = "/de/captaingoldfish/scim/sdk/common/resourcetypes/service-provider.json";

  public static final String SCHEMA_RESOURCE_TYPE_JSON = "/de/captaingoldfish/scim/sdk/common/resourcetypes/schema.json";

  public static final String BULK_REQUEST_SCHEMA = "/de/captaingoldfish/scim/sdk/common/request/bulk-request.json";

  public static final String BULK_RESPONSE_SCHEMA = "/de/captaingoldfish/scim/sdk/common/response/bulk-response.json";

  public static final String PATCH_REQUEST_SCHEMA = "/de/captaingoldfish/scim/sdk/common/request/patch-request.json";

  public static final String SCHEMAS_ATTRIBUTE_DEFINITION = "/de/captaingoldfish/scim/sdk/common/meta/schemas-attribute.json";
}
