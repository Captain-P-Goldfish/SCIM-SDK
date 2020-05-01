package de.captaingoldfish.scim.sdk.client.builder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.Comparator;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.request.SearchRequest;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import lombok.AccessLevel;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 16.12.2019 - 13:00 <br>
 * <br>
 * a builder that can be used to build a list request
 */
public class ListBuilder<T extends ResourceNode>
{

  /**
   * the base url of the scim provider
   */
  private final String baseUrl;

  /**
   * the endpoint path of the resource e.g. /Users or /Groups
   */
  private final String endpoint;

  /**
   * the http client configuration to access the scim endpoint
   */
  private final ScimClientConfig scimClientConfig;

  /**
   * the entity type that should be returned
   */
  private final Class<T> responseEntityType;

  /**
   * the parameters that will be used for the list request
   */
  @Getter(AccessLevel.PROTECTED)
  private final Map<String, String> requestParameters = new HashMap<>();

  /**
   * an apache http client wrapper that offers some convenience methods
   */
  private final ScimHttpClient scimHttpClient;

  public ListBuilder(String baseUrl,
                     String endpoint,
                     ScimClientConfig scimClientConfig,
                     Class<T> responseEntityType,
                     ScimHttpClient scimHttpClient)
  {
    this.baseUrl = baseUrl;
    this.endpoint = endpoint;
    this.scimClientConfig = scimClientConfig;
    this.responseEntityType = responseEntityType;
    this.scimHttpClient = scimHttpClient;
  }

  /**
   * sets the count parameter for the maximum number of entries that should be returned
   *
   * @param count the maximum number of entries that should be returned
   */
  public ListBuilder<T> count(int count)
  {
    requestParameters.put(AttributeNames.RFC7643.COUNT, String.valueOf(count));
    return this;
  }

  /**
   * sets the startIndex parameter for the first entry that should be returned
   *
   * @param startIndex the start index from which the entries should be returned
   */
  public ListBuilder<T> startIndex(long startIndex)
  {
    requestParameters.put(AttributeNames.RFC7643.START_INDEX, String.valueOf(startIndex));
    return this;
  }

  /**
   * sets the attribute name that should be used for sorting the entries
   *
   * @param sortBy the attribute name that should be used for sorting the entries
   */
  public ListBuilder<T> sortBy(String sortBy)
  {
    requestParameters.put(AttributeNames.RFC7643.SORT_BY, sortBy);
    return this;
  }

  /**
   * sets the sorting order of the resources
   *
   * @param sortOrder the sorting order of the resources
   */
  public ListBuilder<T> sortOrder(SortOrder sortOrder)
  {
    requestParameters.put(AttributeNames.RFC7643.SORT_ORDER, sortOrder.name().toLowerCase());
    return this;
  }

  /**
   * adds the attributes that should be returned by the service provider
   *
   * @param attributeNames the names of the attributes that should be returned by the service provider
   */
  public ListBuilder<T> attributes(String... attributeNames)
  {
    if (attributeNames != null)
    {
      requestParameters.put(AttributeNames.RFC7643.ATTRIBUTES, String.join(",", attributeNames));
    }
    return this;
  }

  /**
   * adds the excluded attributes that should not be returned by the service provider
   *
   * @param attributeNames the names of the excluded attributes that should not be returned by the service
   *          provider
   */
  public ListBuilder<T> excludedAttributes(String... attributeNames)
  {
    if (attributeNames != null)
    {
      requestParameters.put(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES, String.join(",", attributeNames));
    }
    return this;
  }

  /**
   * creates a new filter-builder that can be used to create filter expressions
   */
  public FilterBuilder<T> filter(String attributeName, Comparator comparator, String value)
  {
    return filter(false, attributeName, comparator, value);
  }

  /**
   * creates a new filter-builder that can be used to create filter expressions
   */
  public FilterBuilder<T> filter(boolean openParanthesis, String attributeName, Comparator comparator, String value)
  {
    return new FilterBuilder<>(this, attributeName, comparator, value, openParanthesis);
  }

  /**
   * sets the given filter as attribute
   */
  public ListBuilder<T> filter(String filter)
  {
    requestParameters.put(AttributeNames.RFC7643.FILTER, filter);
    return this;
  }

  /**
   * adds additional custom parameters to the request that are unknown by the SCIM specification
   *
   * @param attributeName the name of the attribute to add
   * @param attribute the value of the attribute to add
   */
  public ListBuilder<T> custom(String attributeName, String attribute)
  {
    requestParameters.put(attributeName, attribute);
    return this;
  }

  /**
   * list requests can be either send with a get-request or a post request
   *
   * @return a get-request builder
   */
  public GetRequestBuilder<T> get()
  {
    return new GetRequestBuilder<>(this);
  }

  /**
   * list requests can be either send with a get-request or a post request
   *
   * @return a post-request builder
   */
  public PostRequestBuilder<T> post()
  {
    return new PostRequestBuilder<>(this);
  }

  /**
   * a request builder that builds the list-request as a http-get request
   */
  public static class GetRequestBuilder<T extends ResourceNode> extends RequestBuilder<ListResponse<T>>
  {

    /**
     * the original list builder instance
     */
    private ListBuilder<T> listBuilder;

    public GetRequestBuilder(ListBuilder<T> listBuilder)
    {
      super(listBuilder.baseUrl, listBuilder.endpoint, listBuilder.scimClientConfig,
            (Class<ListResponse<T>>)new ListResponse<T>().getClass(), listBuilder.scimHttpClient);
      this.listBuilder = listBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isExpectedResponseCode(int httpStatus)
    {
      return HttpStatus.OK == httpStatus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HttpUriRequest getHttpUriRequest()
    {
      StringBuilder queryBuilder = new StringBuilder();
      if (!listBuilder.requestParameters.isEmpty())
      {
        queryBuilder.append("?");
        List<String> pairs = new ArrayList<>();
        listBuilder.requestParameters.forEach((key, value) -> {
          try
          {
            pairs.add(key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
          }
          catch (UnsupportedEncodingException e)
          {
            throw new IllegalStateException(e.getMessage(), e);
          }
        });
        queryBuilder.append(String.join("&", pairs));
      }
      return new HttpGet(getBaseUrl() + getEndpoint() + queryBuilder.toString());
    }

    /**
     * checks if the response contains a schema-uri that matches the value of
     * {@link de.captaingoldfish.scim.sdk.common.constants.SchemaUris#LIST_RESPONSE_URI}
     */
    @Override
    protected Function<HttpResponse, Boolean> isResponseParseable()
    {
      return httpResponse -> {
        String responseBody = httpResponse.getResponseBody();
        if (StringUtils.isNotBlank(responseBody) && responseBody.contains(SchemaUris.LIST_RESPONSE_URI))
        {
          return true;
        }
        return false;
      };
    }
  }

  /**
   * a request builder that builds the list-request as a http-post request
   */
  public static class PostRequestBuilder<T extends ResourceNode> extends RequestBuilder<ListResponse<T>>
  {

    /**
     * the original list builder instance
     */
    private ListBuilder<T> listBuilder;

    public PostRequestBuilder(ListBuilder<T> listBuilder)
    {
      super(listBuilder.baseUrl, listBuilder.endpoint, listBuilder.scimClientConfig,
            (Class<ListResponse<T>>)new ListResponse<T>().getClass(), listBuilder.scimHttpClient);
      this.listBuilder = listBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isExpectedResponseCode(int httpStatus)
    {
      return HttpStatus.OK == httpStatus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HttpUriRequest getHttpUriRequest()
    {
      HttpPost httpPost = new HttpPost(getBaseUrl() + getEndpoint() + "/.search");
      if (!listBuilder.requestParameters.isEmpty())
      {
        SearchRequest searchRequest = SearchRequest.builder().build();
        listBuilder.requestParameters.forEach(searchRequest::put);
        super.setResource(searchRequest);
        StringEntity stringEntity = new StringEntity(getResource(), StandardCharsets.UTF_8);
        httpPost.setEntity(stringEntity);
      }
      return httpPost;
    }

    /**
     * checks if the response contains a schema-uri that matches the value of
     * {@link de.captaingoldfish.scim.sdk.common.constants.SchemaUris#LIST_RESPONSE_URI}
     */
    @Override
    protected Function<HttpResponse, Boolean> isResponseParseable()
    {
      return httpResponse -> {
        String responseBody = httpResponse.getResponseBody();
        if (StringUtils.isNotBlank(responseBody) && responseBody.contains(SchemaUris.LIST_RESPONSE_URI))
        {
          return true;
        }
        return false;
      };
    }
  }

  /**
   * used to build a filter expression
   */
  public class FilterBuilder<T extends ResourceNode>
  {

    /**
     * the original builder to which this builder will return if building the filter is finished
     */
    private final ListBuilder<T> listBuilder;

    /**
     * this builder will represent the built filter expression
     */
    private final StringBuilder filterString = new StringBuilder();

    /**
     * tells us how many parenthesis' have been opened so far
     */
    private int openedParenthesis = 0;

    /**
     * tells us how many parenthesis' have been closed so far
     */
    private int closedParenthesis = 0;

    public FilterBuilder(ListBuilder<T> listBuilder,
                         String attributeName,
                         Comparator comparator,
                         String value,
                         boolean openParenthesis)
    {
      this.listBuilder = listBuilder;
      this.openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
    }

    /**
     * opens a parenthesis for the current filter expression
     */
    private FilterBuilder<T> openParenthesis(boolean openParenthesis)
    {
      if (openParenthesis)
      {
        openedParenthesis++;
        filterString.append("(");
      }
      return this;
    }

    /**
     * closes a parenthesis for the current filter expression
     */
    public FilterBuilder<T> closeParenthesis()
    {
      closedParenthesis++;
      filterString.append(")");
      return this;
    }

    /**
     * closes a parenthesis for the current filter expression
     */
    private FilterBuilder<T> closeParenthesis(boolean closeParenthesis)
    {
      if (closeParenthesis)
      {
        return closeParenthesis();
      }
      return this;
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, String value)
    {
      return and(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, String value)
    {
      return or(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, Boolean value)
    {
      return and(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, Boolean value)
    {
      return or(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, Integer value)
    {
      return and(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, Integer value)
    {
      return or(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, Long value)
    {
      return and(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, Long value)
    {
      return or(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, Double value)
    {
      return and(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, Double value)
    {
      return or(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, Instant value)
    {
      return and(false, attributeName, comparator, value);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, Instant value)
    {
      return or(false, attributeName, comparator, value);
    }




    public FilterBuilder<T> and(boolean openParenthesis, String attributeName, Comparator comparator, String value)
    {
      return and(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> or(boolean openParenthesis, String attributeName, Comparator comparator, String value)
    {
      return or(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> and(boolean openParenthesis, String attributeName, Comparator comparator, Boolean value)
    {
      return and(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> or(boolean openParenthesis, String attributeName, Comparator comparator, Boolean value)
    {
      return or(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> and(boolean openParenthesis, String attributeName, Comparator comparator, Integer value)
    {
      return and(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> or(boolean openParenthesis, String attributeName, Comparator comparator, Integer value)
    {
      return or(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> and(boolean openParenthesis, String attributeName, Comparator comparator, Long value)
    {
      return and(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> or(boolean openParenthesis, String attributeName, Comparator comparator, Long value)
    {
      return or(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> and(boolean openParenthesis, String attributeName, Comparator comparator, Double value)
    {
      return and(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> or(boolean openParenthesis, String attributeName, Comparator comparator, Double value)
    {
      return or(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> and(boolean openParenthesis, String attributeName, Comparator comparator, Instant value)
    {
      return and(openParenthesis, attributeName, comparator, value, false);
    }

    public FilterBuilder<T> or(boolean openParenthesis, String attributeName, Comparator comparator, Instant value)
    {
      return or(openParenthesis, attributeName, comparator, value, false);
    }





    public FilterBuilder<T> and(String attributeName, Comparator comparator, String value, boolean closeParenthesis)
    {
      return and(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, String value, boolean closeParenthesis)
    {
      return or(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, Boolean value, boolean closeParenthesis)
    {
      return and(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, Boolean value, boolean closeParenthesis)
    {
      return or(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, Integer value, boolean closeParenthesis)
    {
      return and(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, Integer value, boolean closeParenthesis)
    {
      return or(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, Long value, boolean closeParenthesis)
    {
      return and(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, Long value, boolean closeParenthesis)
    {
      return or(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, Double value, boolean closeParenthesis)
    {
      return and(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, Double value, boolean closeParenthesis)
    {
      return or(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> and(String attributeName, Comparator comparator, Instant value, boolean closeParenthesis)
    {
      return and(false, attributeName, comparator, value, closeParenthesis);
    }

    public FilterBuilder<T> or(String attributeName, Comparator comparator, Instant value, boolean closeParenthesis)
    {
      return or(false, attributeName, comparator, value, closeParenthesis);
    }



    public FilterBuilder<T> and(boolean openParenthesis,
                                String attributeName,
                                Comparator comparator,
                                String value,
                                boolean closeParenthesis)
    {
      filterString.append(" and ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> or(boolean openParenthesis,
                               String attributeName,
                               Comparator comparator,
                               String value,
                               boolean closeParenthesis)
    {
      filterString.append(" or ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> and(boolean openParenthesis,
                                String attributeName,
                                Comparator comparator,
                                Boolean value,
                                boolean closeParenthesis)
    {
      filterString.append(" and ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> or(boolean openParenthesis,
                               String attributeName,
                               Comparator comparator,
                               Boolean value,
                               boolean closeParenthesis)
    {
      filterString.append(" or ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> and(boolean openParenthesis,
                                String attributeName,
                                Comparator comparator,
                                Integer value,
                                boolean closeParenthesis)
    {
      filterString.append(" and ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> or(boolean openParenthesis,
                               String attributeName,
                               Comparator comparator,
                               Integer value,
                               boolean closeParenthesis)
    {
      filterString.append(" or ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> and(boolean openParenthesis,
                                String attributeName,
                                Comparator comparator,
                                Long value,
                                boolean closeParenthesis)
    {
      filterString.append(" and ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> or(boolean openParenthesis,
                               String attributeName,
                               Comparator comparator,
                               Long value,
                               boolean closeParenthesis)
    {
      filterString.append(" or ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> and(boolean openParenthesis,
                                String attributeName,
                                Comparator comparator,
                                Double value,
                                boolean closeParenthesis)
    {
      filterString.append(" and ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> or(boolean openParenthesis,
                               String attributeName,
                               Comparator comparator,
                               Double value,
                               boolean closeParenthesis)
    {
      filterString.append(" or ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value);
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> and(boolean openParenthesis,
                                String attributeName,
                                Comparator comparator,
                                Instant value,
                                boolean closeParenthesis)
    {
      filterString.append(" and ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value.toString());
      closeParenthesis(closeParenthesis);
      return this;
    }

    public FilterBuilder<T> or(boolean openParenthesis,
                               String attributeName,
                               Comparator comparator,
                               Instant value,
                               boolean closeParenthesis)
    {
      filterString.append(" or ");
      openParenthesis(openParenthesis);
      setExpression(attributeName, comparator, value.toString());
      closeParenthesis(closeParenthesis);
      return this;
    }

    /**
     * adds an expression into the filter
     *
     * @param attributeName the attribute name of the expression
     * @param comparator the comparator to use
     * @param value the value of the expression
     */
    private void setExpression(String attributeName, Comparator comparator, Object value)
    {
      filterString.append(attributeName).append(" ").append(comparator.name());
      if (value instanceof String)
      {
        filterString.append(value == null ? "" : " ").append("\"").append(value == null ? "" : value).append("\"");
      }
      else
      {
        filterString.append(value == null ? "" : " " + value);
      }
    }

    /**
     * builds the filter string and puts it into the parameter map of the list builder instance
     */
    public ListBuilder<T> build()
    {
      if (openedParenthesis != closedParenthesis)
      {
        throw new IllegalStateException("error within filter expression\n\topened parentheses: " + openedParenthesis
                                        + "\n\tclosed parentheses: " + closedParenthesis + "\n\tfilter: "
                                        + filterString.toString());
      }
      listBuilder.requestParameters.put(AttributeNames.RFC7643.FILTER, filterString.toString());
      return listBuilder;
    }
  }
}
