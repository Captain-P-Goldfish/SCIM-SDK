package de.captaingoldfish.scim.sdk.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 16:17 <br>
 * <br>
 * this is just a helper class that knows the names of the common resource types.<br>
 * Of course this class is obsolete as soon as the name elements are overwritten within the resource types
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceTypeNames
{

  public static final String SERVICE_PROVIDER_CONFIG = "ServiceProviderConfig";

  public static final String RESOURCE_TYPE = "ResourceType";

  public static final String SCHEMA = "Schema";

  public static final String USER = "User";

  public static final String GROUPS = "Group";

  public static final String ME = "Me";

}
