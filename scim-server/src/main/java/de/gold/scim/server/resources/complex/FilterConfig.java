package de.gold.scim.server.resources.complex;

import java.util.Optional;

import de.gold.scim.server.constants.AttributeNames;
import de.gold.scim.server.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 10:56 <br>
 * <br>
 * A complex type that specifies FILTER options. REQUIRED. See Section 3.4.2.2 of [RFC7644].
 */
public class FilterConfig extends ScimObjectNode
{

  /**
   * the default value for the max results value. Default is 1. This will enforce the developer to modify the
   * service provider configuration to the applications requirements
   */
  protected static final Integer DEFAULT_MAX_RESULTS = 1;

  @Builder
  public FilterConfig(Boolean supported, Integer maxResults)
  {
    super(null);
    setSupported(supported);
    setMaxResults(maxResults);
  }

  /**
   * A Boolean value specifying whether or not the operation is supported. REQUIRED.
   */
  public boolean isSupported()
  {
    return getBooleanAttribute(AttributeNames.RFC7643.SUPPORTED).orElse(false);
  }

  /**
   * A Boolean value specifying whether or not the operation is supported. REQUIRED.
   */
  public void setSupported(Boolean supported)
  {
    setAttribute(AttributeNames.RFC7643.SUPPORTED, Optional.ofNullable(supported).orElse(false));
  }

  /**
   * An integer value specifying the maximum number of resources returned in a response. REQUIRED.
   */
  public Integer getMaxResults()
  {
    return getLongAttribute(AttributeNames.RFC7643.MAX_RESULTS).orElse(Long.valueOf(DEFAULT_MAX_RESULTS)).intValue();
  }

  /**
   * An integer value specifying the maximum number of resources returned in a response. REQUIRED.
   */
  public void setMaxResults(Integer maxResults)
  {
    Long results = maxResults == null ? null : Long.valueOf(maxResults);
    setAttribute(AttributeNames.RFC7643.MAX_RESULTS,
                 Optional.ofNullable(results).orElse(Long.valueOf(DEFAULT_MAX_RESULTS)));
  }
}
