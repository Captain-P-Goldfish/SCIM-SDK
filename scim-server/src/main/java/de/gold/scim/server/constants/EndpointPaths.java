package de.gold.scim.server.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 16:17 <br>
 * <br>
 * this is just a helper class that knows the endpoint paths of the common endpoints.<br>
 * Of course this class is obsolete as soon as the endpoint urls are overwritten within the resource types
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EndpointPaths
{

  public static final String SERVICE_PROVIDER_CONFIG = "/ServiceProviderConfig";

  public static final String RESOURCE_TYPES = "/ResourceTypes";

  public static final String SCHEMAS = "/Schemas";

  public static final String USERS = "/Users";

  public static final String GROUPS = "/Groups";

  public static final String ME = "/Me";

  public static final String BULK = "/Bulk";

  public static final String SEARCH = "/.search";
}
