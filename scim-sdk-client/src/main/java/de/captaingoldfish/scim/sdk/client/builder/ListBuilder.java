package de.captaingoldfish.scim.sdk.client.builder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.HttpResponse;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames.RFC7643;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.Comparator;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.request.SearchRequest;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.utils.EncodingUtils;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 16.12.2019 - 13:00 <br>
 * <br>
 * a builder that can be used to build a list request
 */
@Slf4j
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
   * the entity type that should be returned. Has actually no usage but is only here to setup the generic type
   * of this instance
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

  /**
   * the fully qualified url to the required resource
   */
  private final String fullUrl;

  /**
   * if the resource should be retrieved by using the fully qualified url
   *
   * @param fullUrl the fully qualified url to the required resource
   * @param responseEntityType the type of the resource that should be returned
   * @param scimHttpClient the http client instance
   */
  public ListBuilder(String fullUrl, Class<T> responseEntityType, ScimHttpClient scimHttpClient)
  {
    this.baseUrl = null;
    this.endpoint = null;
    this.responseEntityType = responseEntityType;
    this.scimHttpClient = scimHttpClient;
    this.fullUrl = fullUrl;
  }

  public ListBuilder(String baseUrl, String endpoint, Class<T> responseEntityType, ScimHttpClient scimHttpClient)
  {
    this.baseUrl = baseUrl;
    this.endpoint = endpoint;
    this.responseEntityType = responseEntityType;
    this.scimHttpClient = scimHttpClient;
    this.fullUrl = null;
  }

  /**
   * sets the count parameter for the maximum number of entries that should be returned
   *
   * @param count the maximum number of entries that should be returned
   */
  public ListBuilder<T> count(int count)
  {
    requestParameters.put(RFC7643.COUNT, String.valueOf(count));
    return this;
  }

  /**
   * sets the startIndex parameter for the first entry that should be returned
   *
   * @param startIndex the start index from which the entries should be returned
   */
  public ListBuilder<T> startIndex(long startIndex)
  {
    requestParameters.put(RFC7643.START_INDEX, String.valueOf(startIndex));
    return this;
  }

  /**
   * sets the attribute name that should be used for sorting the entries
   *
   * @param sortBy the attribute name that should be used for sorting the entries
   */
  public ListBuilder<T> sortBy(String sortBy)
  {
    requestParameters.put(RFC7643.SORT_BY, sortBy);
    return this;
  }

  /**
   * sets the sorting order of the resources
   *
   * @param sortOrder the sorting order of the resources
   */
  public ListBuilder<T> sortOrder(SortOrder sortOrder)
  {
    requestParameters.put(RFC7643.SORT_ORDER, sortOrder.name().toLowerCase());
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
      requestParameters.put(RFC7643.ATTRIBUTES, String.join(",", attributeNames));
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
      requestParameters.put(RFC7643.EXCLUDED_ATTRIBUTES, String.join(",", attributeNames));
    }
    return this;
  }

  /**
   * creates a new filter-builder that can be used to create filter expressions
   */
  public FilterBuilder<T> filter()
  {
    return new FilterBuilder<>(this);
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
    requestParameters.put(RFC7643.FILTER, filter);
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

  private abstract static class TypedRequestBuilder<T extends ResourceNode> extends RequestBuilder<ListResponse<T>>
  {

    /**
     * the original list builder instance
     */
    protected ListBuilder<T> listBuilder;

    protected TypedRequestBuilder(ListBuilder<T> listBuilder)
    {
      super(listBuilder.baseUrl, listBuilder.endpoint, (Class<ListResponse<T>>)new ListResponse<T>().getClass(),
            listBuilder.scimHttpClient);
      this.listBuilder = listBuilder;
    }

    /**
     * this method can be used to retrieve all resources from the given startIndex of the given endpoint
     */
    public ServerResponse<ListResponse<T>> getAll()
    {
      List<ServerResponse<ListResponse<T>>> responseList = new ArrayList<>();
      boolean needsAdditionalRequest = false;
      long totalResults;

      final long originalStartIndex = Optional.ofNullable(listBuilder.getRequestParameters().get(RFC7643.START_INDEX))
                                              .map(Long::parseLong)
                                              .orElse(1L);
      final Integer originalCount = Optional.ofNullable(listBuilder.getRequestParameters().get(RFC7643.START_INDEX))
                                            .map(Integer::parseInt)
                                            .orElse(null);
      int iterations = 0;
      ArrayNode resources = new ArrayNode(JsonNodeFactory.instance);
      do
      {
        log.trace("Loading resources in iteration: {}", iterations++);
        ServerResponse<ListResponse<T>> response = sendRequest();
        if (!response.isSuccess())
        {
          log.warn("Failed to load next-resources in iteration. Ignoring previous responses: {}", iterations);
          return response;
        }
        responseList.add(response);
        ArrayNode nextResources = (ArrayNode)response.getResource().get(RFC7643.RESOURCES);
        if (nextResources != null)
        {
          resources.addAll(nextResources);
        }
        ListResponse<T> listResponse = response.getResource();
        totalResults = listResponse.getTotalResults();
        final int itemsPerPage = listResponse.getItemsPerPage();
        final long usedStartIndex = listResponse.getStartIndex();
        needsAdditionalRequest = (originalCount == null || originalCount > 0)
                                 && (usedStartIndex - 1) + itemsPerPage < totalResults;
        if (needsAdditionalRequest)
        {
          listBuilder.startIndex(usedStartIndex + itemsPerPage);
        }
      }
      while (needsAdditionalRequest);

      ListResponse<T> rebuildListResponse = new ListResponse<>();
      rebuildListResponse.setTotalResults(totalResults);
      // we have merged all requests into a single response
      rebuildListResponse.setItemsPerPage((int)(totalResults - (originalStartIndex - 1)));
      rebuildListResponse.setStartIndex(originalStartIndex);
      rebuildListResponse.set(AttributeNames.RFC7643.RESOURCES, resources);

      HttpResponse httpResponse = HttpResponse.builder().httpStatusCode(HttpStatus.OK).build();
      return new ServerResponse<>(httpResponse, true, rebuildListResponse);
    }
  }

  /**
   * a request builder that builds the list-request as a http-get request
   */
  public static class GetRequestBuilder<T extends ResourceNode> extends TypedRequestBuilder<T>
  {

    public GetRequestBuilder(ListBuilder<T> listBuilder)
    {
      super(listBuilder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GetRequestBuilder<T> setExpectedResponseHeaders(Map<String, String> requiredResponseHeaders)
    {
      return (GetRequestBuilder<T>)super.setExpectedResponseHeaders(requiredResponseHeaders);
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
        if (StringUtils.contains(listBuilder.fullUrl, "?"))
        {
          queryBuilder.append("&");
        }
        else
        {
          queryBuilder.append("?");
        }
        List<String> pairs = new ArrayList<>();
        listBuilder.requestParameters.forEach((key, value) -> {
          pairs.add(key + "=" + EncodingUtils.urlEncode(value));
        });
        queryBuilder.append(String.join("&", pairs));
      }
      HttpGet httpGet;
      if (StringUtils.isBlank(listBuilder.fullUrl))
      {
        httpGet = new HttpGet(getBaseUrl() + getEndpoint() + queryBuilder);
      }
      else
      {
        httpGet = new HttpGet(listBuilder.fullUrl + queryBuilder);
      }
      return httpGet;
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

    /**
     * uses a custom response type that overrides the translation of the returned resource
     */
    @Override
    protected ServerResponse<ListResponse<T>> toResponse(HttpResponse response)
    {
      return new ListServerResponse<>(response, isExpectedResponseCode(response.getHttpStatusCode()),
                                      getResponseEntityType(), listBuilder.responseEntityType, isResponseParseable(),
                                      getRequiredResponseHeaders());
    }
  }

  /**
   * a request builder that builds the list-request as a http-post request
   */
  public static class PostRequestBuilder<T extends ResourceNode> extends TypedRequestBuilder<T>
  {

    public PostRequestBuilder(ListBuilder<T> listBuilder)
    {
      super(listBuilder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PostRequestBuilder<T> setExpectedResponseHeaders(Map<String, String> requiredResponseHeaders)
    {
      return (PostRequestBuilder<T>)super.setExpectedResponseHeaders(requiredResponseHeaders);
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
      HttpPost httpPost;
      if (StringUtils.isBlank(listBuilder.fullUrl))
      {
        httpPost = new HttpPost(getBaseUrl() + getEndpoint() + "/.search");
      }
      else
      {
        String url = listBuilder.fullUrl;
        if (url.endsWith("/.search"))
        {
          httpPost = new HttpPost(listBuilder.fullUrl);
        }
        else
        {
          httpPost = new HttpPost(listBuilder.fullUrl + "/.search");
        }
      }
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

    /**
     * uses a custom response type that overrides the translation of the returned resource
     */
    @Override
    protected ServerResponse<ListResponse<T>> toResponse(HttpResponse response)
    {
      return new ListServerResponse<>(response, isExpectedResponseCode(response.getHttpStatusCode()),
                                      getResponseEntityType(), listBuilder.responseEntityType, isResponseParseable(),
                                      getRequiredResponseHeaders());
    }
  }

  /**
   * overrides the translation of the returned resource from the server
   */
  public static class ListServerResponse<T extends ResourceNode> extends ServerResponse<ListResponse<T>>
  {

    /**
     * the generic type of the resources within the list response
     */
    private Class<T> responseEntityType;

    public ListServerResponse(HttpResponse httpResponse,
                              boolean expectedResponseCode,
                              Class<ListResponse<T>> type,
                              Class<T> responseEntityType,
                              Function<HttpResponse, Boolean> isResponseParseable,
                              Map<String, String> requiredResponseHeaders)
    {
      super(httpResponse, expectedResponseCode, type, isResponseParseable, requiredResponseHeaders);
      this.responseEntityType = responseEntityType;
    }

    /**
     * translates the response body into a list response and parses then all json nodes within the resource into
     * objects of the given resource node type
     *
     * @param responseType the type of the node which might be of type
     *          {@link de.captaingoldfish.scim.sdk.common.resources.User},
     *          {@link de.captaingoldfish.scim.sdk.common.resources.Group}
     * @return a list response with resources of type R
     */
    @Override
    public <R extends ScimObjectNode> R getResource(Class<R> responseType)
    {
      ListResponse<ScimObjectNode> listResponse = JsonHelper.readJsonDocument(getResponseBody(), ListResponse.class);
      List<T> typedResources = listResponse.getListedResources().parallelStream().map(scimObjectNode -> {
        return JsonHelper.readJsonDocument(scimObjectNode.toString(), responseEntityType);
      }).collect(Collectors.toList());
      ListResponse typedListResponse = new ListResponse<>(responseEntityType);
      typedListResponse.setItemsPerPage(listResponse.getItemsPerPage());
      typedListResponse.setStartIndex(listResponse.getStartIndex());
      typedListResponse.setTotalResults(listResponse.getTotalResults());
      typedListResponse.setListedResources(typedResources);
      return (R)typedListResponse;
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

    public FilterBuilder(ListBuilder<T> listBuilder)
    {
      this.listBuilder = listBuilder;
    }

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
      if (filterString.length() != 0)
      {
        filterString.append(" and ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" or ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" and ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" or ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" and ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" or ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" and ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" or ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" and ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" or ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" and ");
      }
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
      if (filterString.length() != 0)
      {
        filterString.append(" or ");
      }
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
      ScimClientConfig scimClientConfig = scimHttpClient.getScimClientConfig();
      String comparatorString = scimClientConfig.isUseLowerCaseInFilterComparators() ? comparator.name().toLowerCase()
        : comparator.name();
      filterString.append(attributeName).append(" ").append(comparatorString);
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
                                        + filterString);
      }
      listBuilder.requestParameters.put(RFC7643.FILTER, filterString.toString());
      return listBuilder;
    }
  }
}
