package de.captaingoldfish.scim.sdk.common.resources.complex;

import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 10:56 <br>
 * <br>
 * A complex type that specifies FILTER options. REQUIRED. See Section 3.4.2.2 of [RFC7644].
 */
@Slf4j
public class FilterConfig extends ScimObjectNode
{

  /**
   * the default value for the max results value. Default is 1. This will enforce the developer to modify the
   * service provider configuration to the applications requirements
   */
  public static final Integer DEFAULT_MAX_RESULTS = 1;

  /**
   * the default value for the max filter depth. Default is 1000.
   */
  public static final Integer DEFAULT_MAX_FILTER_DEPTH = 1000;

  public FilterConfig()
  {
    setSupported(false);
  }

  @Builder
  public FilterConfig(Boolean supported, Integer maxResults, Integer maxFilterDepth)
  {
    super(null);
    setSupported(Optional.ofNullable(supported).orElse(false));
    setMaxResults(maxResults);
    setMaxFilterDepth(maxFilterDepth);
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
    setAttribute(AttributeNames.RFC7643.MAX_RESULTS, Optional.ofNullable(results).orElseGet(() -> {
      log.warn("No value set for 'FilterConfig.maxResults'. Value is defaulting to: {}", DEFAULT_MAX_RESULTS);
      return Long.valueOf(DEFAULT_MAX_RESULTS);
    }));
  }

  /**
   * describes the maximum filter depth for filter expressions that must not be exceeded
   */
  public Integer getMaxFilterDepth()
  {
    return getIntegerAttribute(AttributeNames.Custom.MAX_FILTER_DEPTH).orElse(DEFAULT_MAX_FILTER_DEPTH);
  }

  /**
   * describes the maximum filter depth for filter expressions that must not be exceeded
   */
  public void setMaxFilterDepth(Integer maxFilterDepth)
  {
    if (maxFilterDepth != null && maxFilterDepth > 3000)
    {
      log.warn("Filter depth with more than 3000 expressions will most probably cause a StackOverflowError.");
    }
    setAttribute(AttributeNames.Custom.MAX_FILTER_DEPTH, Optional.ofNullable(maxFilterDepth).orElseGet(() -> {
      log.warn("No value set for 'FilterConfig.maxFilterDepth'. Value is defaulting to: {}", DEFAULT_MAX_FILTER_DEPTH);
      return DEFAULT_MAX_FILTER_DEPTH;
    }));
  }

  /**
   * override lombok builder with public constructor
   */
  public static class FilterConfigBuilder
  {

    public FilterConfigBuilder()
    {}
  }
}
