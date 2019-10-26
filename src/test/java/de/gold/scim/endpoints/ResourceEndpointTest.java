package de.gold.scim.endpoints;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.EndpointPaths;
import de.gold.scim.constants.enums.HttpMethod;
import de.gold.scim.endpoints.base.UserEndpointDefinition;
import de.gold.scim.endpoints.handler.UserHandlerImpl;
import de.gold.scim.exceptions.BadRequestException;
import de.gold.scim.resources.ServiceProvider;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 26.10.2019 - 00:32 <br>
 * <br>
 */
@Slf4j
public class ResourceEndpointTest
{

  /**
   * the resource endpoint under test
   */
  private ResourceEndpoint resourceEndpoint;

  /**
   * the service provider configuration
   */
  private ServiceProvider serviceProvider;

  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    serviceProvider = ServiceProvider.builder().build();
    resourceEndpoint = new ResourceEndpoint(serviceProvider, new UserEndpointDefinition(new UserHandlerImpl()));
  }

  /**
   * tests that urls are correctly resolved by the {@link ResourceEndpoint}
   */
  @ParameterizedTest
  @CsvSource({"/Users,,,POST", "/Users/.search,,,POST", "/Users,123456,,GET", "/Users,,startIndex=1&count=50,GET",
              "/Users,123456,,PUT", "/Users,123456,,DELETE", "/Users,123456,,PATCH"})
  public void testParseUri(String resourcePath, String resourceId, String query, HttpMethod httpMethod)
  {
    final String baseUrl = "https://localhost/management/Users/scim/v2";
    final String resourceUrl = baseUrl + resourcePath + (resourceId == null ? "" : "/" + resourceId)
                               + (query == null ? "" : "?" + query);
    ResourceEndpoint.UriInfos uriInfos = resourceEndpoint.getRequestUrlInfos(resourceUrl, httpMethod);
    Assertions.assertEquals(baseUrl, uriInfos.getBaseUri());
    Assertions.assertEquals(resourcePath.replaceFirst(EndpointPaths.SEARCH, ""), uriInfos.getResourceEndpoint());
    Assertions.assertEquals(resourceId, uriInfos.getResourceId());
    Assertions.assertEquals(resourcePath.endsWith(EndpointPaths.SEARCH) && HttpMethod.POST.equals(httpMethod)
                            || HttpMethod.GET.equals(httpMethod) && query != null,
                            uriInfos.isSearchRequest());
  }

  /**
   * will verify that the query for a filter request is correctly parsed
   */
  @Test
  public void testParseQuery() throws UnsupportedEncodingException
  {
    final String startIndex = "1";
    final String count = "50";
    final String filter = "=username+eq+%5C%22chuck%5C%22";
    final String sortBy = "userName";
    final String sortOrder = "ascending";
    final String attributes = "name";
    final String excludedAttributes = "nickName";
    final String query = String.format("startIndex=%s&count=%s&filter=%s&sortBy=%s&sortOrder=%s&attributes=%s"
                                       + "&excludedAttributes=%s",
                                       startIndex,
                                       count,
                                       filter,
                                       sortBy,
                                       sortOrder,
                                       attributes,
                                       excludedAttributes);
    final String baseUrl = "https://localhost/scim/v2";
    final String url = baseUrl + EndpointPaths.USERS + "?" + query;
    ResourceEndpoint.UriInfos uriInfos = resourceEndpoint.getRequestUrlInfos(url, HttpMethod.GET);
    Assertions.assertEquals(baseUrl, uriInfos.getBaseUri());
    Assertions.assertEquals(EndpointPaths.USERS, uriInfos.getResourceEndpoint());
    Assertions.assertNull(uriInfos.getResourceId());

    Map<String, String> parameter = uriInfos.getQueryParameters();
    Assertions.assertEquals(startIndex, parameter.get(AttributeNames.RFC7643.START_INDEX));
    Assertions.assertEquals(count, parameter.get(AttributeNames.RFC7643.COUNT));
    Assertions.assertEquals(URLDecoder.decode(filter, StandardCharsets.UTF_8.name()),
                            parameter.get(AttributeNames.RFC7643.FILTER));
    Assertions.assertEquals(sortBy, parameter.get(AttributeNames.RFC7643.SORT_BY));
    Assertions.assertEquals(sortOrder, parameter.get(AttributeNames.RFC7643.SORT_ORDER));
    Assertions.assertEquals(attributes, parameter.get(AttributeNames.RFC7643.ATTRIBUTES));
    Assertions.assertEquals(excludedAttributes, parameter.get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES));
  }

  /**
   * will verify that a {@link de.gold.scim.exceptions.BadRequestException} is thrown if the resource endpoint
   * is unknown
   */
  @Test
  public void testUnknownResourceType()
  {
    final String url = "https://localhost/scim/v2/Unknown";
    Assertions.assertThrows(BadRequestException.class, () -> resourceEndpoint.getRequestUrlInfos(url, HttpMethod.GET));
  }

  /**
   * will verify that invalid parameter combinations in the request will lead to {@link BadRequestException}s
   */
  @ParameterizedTest
  @CsvSource({"POST,123456", "PUT,", "PATCH,", "DELETE,"})
  public void testInvalidRequestParameter(HttpMethod httpMethod, String id)
  {
    final String url = "https://localhost/scim/v2" + EndpointPaths.USERS + (id == null ? "" : "/" + id);
    Assertions.assertThrows(BadRequestException.class, () -> resourceEndpoint.getRequestUrlInfos(url, httpMethod));
  }

  /**
   * verifies that no exceptions are thrown if the endpoint path points to bulk endpoint
   */
  @Test
  public void testParseBulkRequestAsUriInfo()
  {
    final String url = "https://localhost/scim/v2" + EndpointPaths.BULK;
    Assertions.assertDoesNotThrow(() -> resourceEndpoint.getRequestUrlInfos(url, HttpMethod.POST));
  }

  /**
   * will verify that calling the bulk endpoint with another {@link HttpMethod} than POST causes
   * {@link BadRequestException}s
   */
  @ParameterizedTest
  @ValueSource(strings = {"GET", "PUT", "DELETE", "PATCH"})
  public void testParseBulkRequestWithInvalidHttpMethods(HttpMethod httpMethod)
  {
    final String url = "https://localhost/scim/v2" + EndpointPaths.BULK;
    Assertions.assertThrows(BadRequestException.class, () -> resourceEndpoint.getRequestUrlInfos(url, httpMethod));
  }
}
