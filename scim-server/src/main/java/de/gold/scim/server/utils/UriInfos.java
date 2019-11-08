package de.gold.scim.server.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.gold.scim.common.constants.EndpointPaths;
import de.gold.scim.common.constants.ScimType;
import de.gold.scim.common.constants.enums.HttpMethod;
import de.gold.scim.common.exceptions.BadRequestException;
import de.gold.scim.common.exceptions.InternalServerException;
import de.gold.scim.server.schemas.ResourceType;
import de.gold.scim.server.schemas.ResourceTypeFactory;
import lombok.Builder;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 08.11.2019 - 22:28 <br>
 * <br>
 * represents the parsed uri infos of a request
 */
@Getter
public class UriInfos
{

  /**
   * the resource endpoint reference e.g. "/Users" or "/Groups"
   */
  private final String resourceEndpoint;

  /**
   * the id of the resource for PUT, DELETE, PATCH and GET requests
   */
  private final String resourceId;

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

  @Builder
  private UriInfos(String resourceEndpoint,
                   String resourceId,
                   boolean searchRequest,
                   String baseUri,
                   String queryParameters,
                   ResourceType resourceType,
                   HttpMethod httpMethod)
  {
    this.resourceEndpoint = resourceEndpoint;
    this.resourceId = resourceId;
    this.searchRequest = searchRequest;
    this.baseUri = baseUri;
    this.queryParameters = queryParameters == null ? new HashMap<>() : RequestUtils.getQueryParameters(queryParameters);
    this.resourceType = resourceType;
    this.httpMethod = Objects.requireNonNull(httpMethod);
    validateUriInfos();
  }

  /**
   * resolves the request uri to individual information's that are necessary to resolve the request
   *
   * @param requestUrl the fully qualified request url
   * @return the individual request information's
   */
  public static UriInfos getRequestUrlInfos(ResourceTypeFactory resourceTypeFactory,
                                            String requestUrl,
                                            HttpMethod httpMethod)
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
                     .build();
    }
    final boolean endsOfSearch = EndpointPaths.SEARCH.endsWith(pathParts[pathParts.length - 1]);
    final boolean endsOfResource = resourceType.getEndpoint().endsWith(pathParts[pathParts.length - 1]);
    final String resourceId = endsOfSearch ? null : (endsOfResource ? null : pathParts[pathParts.length - 1]);
    final boolean searchRequest = endsOfSearch && HttpMethod.POST.equals(httpMethod)
                                  || HttpMethod.GET.equals(httpMethod) && resourceId == null;
    final String baseUri = StringUtils.substringBeforeLast(requestUrl, resourceType.getEndpoint());
    UriInfos uriInfos = UriInfos.builder()
                                .baseUri(baseUri)
                                .searchRequest(searchRequest)
                                .resourceEndpoint(resourceType.getEndpoint())
                                .resourceId(resourceId)
                                .queryParameters(url.getQuery())
                                .resourceType(resourceType)
                                .httpMethod(httpMethod)
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
    for ( ResourceType resourceType : resourceTypeFactory.getAllResourceTypes() )
    {
      if (StringUtils.endsWith(resourceType.getEndpoint(), urlParts[urlParts.length - 1])
          || (urlParts.length > 1 && StringUtils.endsWith(resourceType.getEndpoint(), urlParts[urlParts.length - 2])))
      {
        return resourceType;
      }
    }
    throw new BadRequestException("the request url does not point to a registered resource type. Registered resource "
                                  + "types are: ["
                                  + resourceTypeFactory.getAllResourceTypes()
                                                       .stream()
                                                       .map(ResourceType::getEndpoint)
                                                       .collect(Collectors.joining(","))
                                  + "]", null, ScimType.Custom.INVALID_PARAMETERS);
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
   * this method will verify that the parsed data of the request is valid for accessing SCIM endpoint
   */
  private void validateUriInfos()
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
        if (StringUtils.isBlank(getResourceId()))
        {
          throw new BadRequestException("missing ID value in request path for method '" + httpMethod + "'", null,
                                        ScimType.Custom.INVALID_PARAMETERS);
        }
    }
  }
}
