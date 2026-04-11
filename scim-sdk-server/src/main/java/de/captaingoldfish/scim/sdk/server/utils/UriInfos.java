package de.captaingoldfish.scim.sdk.server.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.utils.EncodingUtils;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 08.11.2019 - 22:28 <br>
 * <br>
 * represents the parsed uri infos of a request
 */
@Slf4j
@Getter
public class UriInfos
{

  /**
   * the resource endpoint reference e.g. "/Users" or "/Groups"
   */
  private final String resourceEndpoint;

  /**
   * if the given request is a query POST request
   */
  private final boolean searchRequest;

  /**
   * the base uri to this SCIM endpoint
   */
  private final String baseUri;

  /**
   * the get parameters or the uri
   */
  private final Map<String, String> queryParameters;

  /**
   * the resource type to which the url points
   */
  private final ResourceType resourceType;

  /**
   * the http method that was used for this request
   */
  private final HttpMethod httpMethod;

  /**
   * the id of the resource for PUT, DELETE, PATCH and GET requests
   */
  @Setter // setter is necessary for bulkId resolving. This is not specified in SCIM but is added as a feature
  private String resourceId;

  /**
   * contains the http request headers from the client that must be validated
   */
  private Map<String, String> httpHeaders;

  @Builder
  private UriInfos(String resourceEndpoint,
                   String resourceId,
                   boolean searchRequest,
                   String baseUri,
                   String queryParameters,
                   ResourceType resourceType,
                   HttpMethod httpMethod,
                   Map<String, String> httpHeaders,
                   boolean lenientContentTypeChecking)
  {
    this.resourceEndpoint = resourceEndpoint;
    this.resourceId = resourceId;
    this.searchRequest = searchRequest;
    this.baseUri = baseUri;
    this.queryParameters = queryParameters == null ? new HashMap<>() : RequestUtils.getQueryParameters(queryParameters);
    this.resourceType = resourceType;
    this.httpMethod = Objects.requireNonNull(httpMethod);
    validateUriInfos(resourceType);
    this.httpHeaders = validateHttpHeaders(httpHeaders, lenientContentTypeChecking);
  }

  /**
   * resolves the request uri to individual information's that are necessary to resolve the request
   *
   * @param requestUrl the fully qualified request url
   * @param httpHeaders the http request headers
   * @return the individual request information's
   */
  public static UriInfos getRequestUrlInfos(ResourceTypeFactory resourceTypeFactory,
                                            String requestUrl,
                                            HttpMethod httpMethod,
                                            Map<String, String> httpHeaders,
                                            boolean lenientContentTypeChecking)
  {
    final URL url = toUrl(requestUrl);
    final String[] pathParts = url.getPath().split("/");
    final ResourceType resourceType = getResourceType(resourceTypeFactory, pathParts);
    if (isBulkRequest(httpMethod, resourceType))
    {
      return UriInfos.builder()
                     .baseUri(StringUtils.substringBeforeLast(requestUrl, EndpointPaths.BULK))
                     .resourceEndpoint(EndpointPaths.BULK)
                     .httpMethod(httpMethod)
                     .httpHeaders(httpHeaders)
                     .lenientContentTypeChecking(lenientContentTypeChecking)
                     .build();
    }
    final boolean endsOfSearch = EndpointPaths.SEARCH.endsWith(pathParts[pathParts.length - 1]);
    final boolean endsOfResource = resourceType.getEndpoint().endsWith(pathParts[pathParts.length - 1]);
    final String resourceId = endsOfSearch ? null : (endsOfResource ? null : pathParts[pathParts.length - 1]);
    final String decodedResourceId = EncodingUtils.urlDecode(resourceId);
    final boolean searchRequest = endsOfSearch && HttpMethod.POST.equals(httpMethod)
                                  || HttpMethod.GET.equals(httpMethod) && resourceId == null;
    final String baseUri = StringUtils.substringBeforeLast(requestUrl, resourceType.getEndpoint());
    UriInfos uriInfos = UriInfos.builder()
                                .baseUri(baseUri)
                                .searchRequest(searchRequest)
                                .resourceEndpoint(resourceType.getEndpoint())
                                .resourceId(decodedResourceId)
                                .queryParameters(url.getQuery())
                                .resourceType(resourceType)
                                .httpMethod(httpMethod)
                                .httpHeaders(httpHeaders)
                                .lenientContentTypeChecking(lenientContentTypeChecking)
                                .build();
    return uriInfos;
  }

  /**
   * checks if we got a bulk request
   *
   * @param httpMethod the http method must be post for bulk requests
   * @param resourceType the resource type must be null. There are no resource types registered for bulk
   * @return true if this is a bulk request, false else
   */
  private static boolean isBulkRequest(HttpMethod httpMethod, ResourceType resourceType)
  {
    if (resourceType == null) // this is only null if the request is a bulk request
    {
      if (HttpMethod.POST.equals(httpMethod))
      {
        return true;
      }
      else
      {
        throw new BadRequestException("Bulk endpoint can only be reached with a HTTP-POST request", null, null);
      }
    }
    return false;
  }

  /**
   * will get the resource type to which the request url points
   *
   * @param urlParts the request url parts separated by "/"
   * @return the found resource type or null if the request points to the bulk-endpoint
   * @throws BadRequestException if the request does neither point to the bulk endpoint nor a registered
   *           resource type
   */
  private static ResourceType getResourceType(ResourceTypeFactory resourceTypeFactory, String[] urlParts)
  {
    if (EndpointPaths.BULK.endsWith(urlParts[urlParts.length - 1]))
    {
      return null;
    }
    final String nextToLastPathPart = "/" + urlParts[urlParts.length - 2];
    final String lastPathPart = "/" + urlParts[urlParts.length - 1];
    ResourceType resourceType = Optional.ofNullable(resourceTypeFactory.getResourceType(nextToLastPathPart))
                                        .orElse(resourceTypeFactory.getResourceType(lastPathPart));

    if (resourceType != null)
    {
      return resourceType;
    }
    throw new ResourceNotFoundException(String.format("the request url '%s' does not point to a registered resource type. "
                                                      + "Registered resource types are: [%s]",
                                                      String.join("/", urlParts),
                                                      resourceTypeFactory.getAllResourceTypes()
                                                                         .stream()
                                                                         .map(ResourceType::getEndpoint)
                                                                         .collect(Collectors.joining(","))),
                                        null, ScimType.Custom.INVALID_PARAMETERS);
  }

  /**
   * turns the given string into an {@link URL} object
   *
   * @param url the url string
   * @return the {@link URL} instance of the given string
   */
  private static URL toUrl(String url)
  {
    try
    {
      return new URL(url);
    }
    catch (MalformedURLException e)
    {
      throw new InternalServerException(e.getMessage(), e, null);
    }
  }

  /**
   * this method will validate the request headers sent by the client. These headers may also be used in the
   * following processing if the service provider supports entity tags
   *
   * @param httpHeaders the http headers sent by the client
   * @return the validated map
   */
  private Map<String, String> validateHttpHeaders(Map<String, String> httpHeaders, boolean lenientContentTypeChecking)
  {
    if (httpHeaders == null)
    {
      throw new InternalServerException("missing http headers. This is not a client error!", null, null);
    }
    if (httpHeaders.get(EndpointPaths.BULK) != null && httpHeaders.size() <= 2)
    {
      // in this case this method was called from the bulk endpoint and further validation is skipped
      // this is done because the original http headers have already been validated and the sub-operations of the
      // bulk-request do not need to be validated
      httpHeaders.remove(EndpointPaths.BULK);
      return httpHeaders;
    }

    String contentType = httpHeaders.keySet()
                                    .stream()
                                    .filter(header -> Strings.CI.equals(header, HttpHeader.CONTENT_TYPE_HEADER))
                                    .findAny()
                                    .map(httpHeaders::get)
                                    .orElse(null);

    boolean shouldCheckContentType = HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod)
                                     || HttpMethod.PATCH.equals(httpMethod);
    if (shouldCheckContentType)
    {
      if (contentType == null)
      {
        throw new BadRequestException(String.format("Invalid content type. Was '%s' but should be %s",
                                                    contentType,
                                                    HttpHeader.SCIM_CONTENT_TYPE),
                                      null, null);
      }
      else
      {
        boolean hasScimContentType = Strings.CS.startsWith(contentType, HttpHeader.SCIM_CONTENT_TYPE);
        boolean hasApplicationJsonContentType = Strings.CS.startsWith(contentType,
                                                                      HttpHeader.APPLICATION_JSON_CONTENT_TYPE);
        if (hasScimContentType)
        {
          // happy case
        }
        else if (lenientContentTypeChecking && hasApplicationJsonContentType)
        {
          // accepting applicationJson
          log.debug("Accepting Content-Type: 'application/json' as specified by to 'lenientContentTypeChecking' setting.");
        }
        else
        {
          throw new BadRequestException(String.format("Invalid content type. Was '%s' but should be %s",
                                                      contentType,
                                                      HttpHeader.SCIM_CONTENT_TYPE),
                                        null, null);
        }
      }
    }
    // other headers do not need to be validated currently
    return httpHeaders;
  }

  /**
   * this method will verify that the parsed data of the request is valid for accessing SCIM endpoint
   *
   * @param resourceType used to allow empty path ids on singleton endpoints
   */
  private void validateUriInfos(ResourceType resourceType)
  {
    switch (httpMethod)
    {
      case POST:
        if (StringUtils.isNotBlank(getResourceId()))
        {
          throw new BadRequestException("ID values in the path are not allowed on '" + httpMethod + "' requests", null,
                                        ScimType.Custom.INVALID_PARAMETERS);
        }
        break;
      case PUT:
      case PATCH:
      case DELETE:
        if (!resourceType.getFeatures().isSingletonEndpoint() && StringUtils.isBlank(getResourceId()))
        {
          throw new BadRequestException("missing ID value in request path for method '" + httpMethod + "'", null,
                                        ScimType.Custom.INVALID_PARAMETERS);
        }
    }
  }

  @Override
  public String toString()
  {
    return baseUri + resourceEndpoint + (StringUtils.isBlank(resourceId) ? "" : "/" + resourceId);
  }
}
