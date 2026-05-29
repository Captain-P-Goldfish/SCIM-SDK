package de.captaingoldfish.scim.sdk.common.resources.complex;

import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * A complex type that indicates pagination configuration options. OPTIONAL. See
 * <a href="https://www.rfc-editor.org/rfc/rfc9865.html">RFC 9865</a>.
 * <p>
 * The default values are chosen to keep the SDK backwards compatible with deployments that pre-date RFC 9865:
 * cursor-based pagination defaults to disabled, index-based pagination defaults to enabled and the default
 * pagination method defaults to {@code "index"}.
 */
public class PaginationConfig extends ScimObjectNode
{

  /**
   * the value of {@link #getDefaultPaginationMethod()} that selects cursor-based pagination. See RFC 9865.
   */
  public static final String METHOD_CURSOR = "cursor";

  /**
   * the value of {@link #getDefaultPaginationMethod()} that selects index-based pagination. See RFC 9865.
   */
  public static final String METHOD_INDEX = "index";

  public PaginationConfig()
  {
    setCursor(false);
    setIndex(true);
  }

  @Builder
  public PaginationConfig(Boolean cursor,
                          Boolean index,
                          String defaultPaginationMethod,
                          Integer defaultPageSize,
                          Integer maxPageSize,
                          Integer cursorTimeout)
  {
    super(null);
    setCursor(Optional.ofNullable(cursor).orElse(false));
    setIndex(Optional.ofNullable(index).orElse(true));
    setDefaultPaginationMethod(defaultPaginationMethod);
    setDefaultPageSize(defaultPageSize);
    setMaxPageSize(maxPageSize);
    setCursorTimeout(cursorTimeout);
  }

  /**
   * A Boolean value specifying support of cursor-based pagination. REQUIRED. See RFC 9865.
   */
  public boolean isCursor()
  {
    return getBooleanAttribute(AttributeNames.RFC7643.CURSOR).orElse(false);
  }

  /**
   * A Boolean value specifying support of cursor-based pagination. REQUIRED. See RFC 9865.
   */
  public void setCursor(Boolean cursor)
  {
    setAttribute(AttributeNames.RFC7643.CURSOR, Optional.ofNullable(cursor).orElse(false));
  }

  /**
   * A Boolean value specifying support of index-based pagination. REQUIRED. See RFC 9865.
   */
  public boolean isIndex()
  {
    return getBooleanAttribute(AttributeNames.RFC7643.INDEX).orElse(true);
  }

  /**
   * A Boolean value specifying support of index-based pagination. REQUIRED. See RFC 9865.
   */
  public void setIndex(Boolean index)
  {
    setAttribute(AttributeNames.RFC7643.INDEX, Optional.ofNullable(index).orElse(true));
  }

  /**
   * A string value specifying the type of pagination that the service provider defaults to when the client has
   * not specified which method it wishes to use. Possible values are {@code "cursor"} and {@code "index"}.
   * OPTIONAL. See RFC 9865.
   */
  public Optional<String> getDefaultPaginationMethod()
  {
    return getStringAttribute(AttributeNames.RFC7643.DEFAULT_PAGINATION_METHOD);
  }

  /**
   * A string value specifying the type of pagination that the service provider defaults to when the client has
   * not specified which method it wishes to use. Possible values are {@code "cursor"} and {@code "index"}.
   * OPTIONAL. See RFC 9865.
   */
  public void setDefaultPaginationMethod(String defaultPaginationMethod)
  {
    setAttribute(AttributeNames.RFC7643.DEFAULT_PAGINATION_METHOD, defaultPaginationMethod);
  }

  /**
   * Positive integer value specifying the default number of results returned in a page when a count is not
   * specified in the query. OPTIONAL. See RFC 9865.
   */
  public Optional<Integer> getDefaultPageSize()
  {
    return getLongAttribute(AttributeNames.RFC7643.DEFAULT_PAGE_SIZE).map(Long::intValue);
  }

  /**
   * Positive integer value specifying the default number of results returned in a page when a count is not
   * specified in the query. OPTIONAL. See RFC 9865.
   */
  public void setDefaultPageSize(Integer defaultPageSize)
  {
    setAttribute(AttributeNames.RFC7643.DEFAULT_PAGE_SIZE,
                 defaultPageSize == null ? null : Long.valueOf(defaultPageSize));
  }

  /**
   * Positive integer specifying the maximum number of results returned in a page regardless of what is
   * specified for the count in a query. OPTIONAL. See RFC 9865.
   */
  public Optional<Integer> getMaxPageSize()
  {
    return getLongAttribute(AttributeNames.RFC7643.MAX_PAGE_SIZE).map(Long::intValue);
  }

  /**
   * Positive integer specifying the maximum number of results returned in a page regardless of what is
   * specified for the count in a query. OPTIONAL. See RFC 9865.
   */
  public void setMaxPageSize(Integer maxPageSize)
  {
    setAttribute(AttributeNames.RFC7643.MAX_PAGE_SIZE, maxPageSize == null ? null : Long.valueOf(maxPageSize));
  }

  /**
   * Positive integer specifying the minimum number of seconds that a cursor is valid between page requests.
   * OPTIONAL. See RFC 9865.
   */
  public Optional<Integer> getCursorTimeout()
  {
    return getLongAttribute(AttributeNames.RFC7643.CURSOR_TIMEOUT).map(Long::intValue);
  }

  /**
   * Positive integer specifying the minimum number of seconds that a cursor is valid between page requests.
   * OPTIONAL. See RFC 9865.
   */
  public void setCursorTimeout(Integer cursorTimeout)
  {
    setAttribute(AttributeNames.RFC7643.CURSOR_TIMEOUT, cursorTimeout == null ? null : Long.valueOf(cursorTimeout));
  }

  /**
   * override lombok builder with public constructor
   */
  public static class PaginationConfigBuilder
  {

    public PaginationConfigBuilder()
    {}
  }
}
