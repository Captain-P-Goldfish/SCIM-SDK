package de.gold.scim.server.utils;

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

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.common.constants.AttributeNames;
import de.gold.scim.common.constants.EndpointPaths;
import de.gold.scim.common.constants.ResourceTypeNames;
import de.gold.scim.common.constants.enums.HttpMethod;
import de.gold.scim.common.exceptions.BadRequestException;
import de.gold.scim.server.endpoints.ResourceEndpoint;
import de.gold.scim.server.endpoints.base.UserEndpointDefinition;
import de.gold.scim.server.endpoints.handler.UserHandlerImpl;
import de.gold.scim.server.schemas.ResourceTypeFactory;


/**
 * author Pascal Knueppel <br>
 * created at: 08.11.2019 - 22:44 <br>
 * <br>
 */
public class UriInfosTest
{

  /**
   * a simple basic uri used in these tests
   */
  private static final String BASE_URI = "https://localhost/scim/v2";

  private ResourceTypeFactory resourceTypeFactory;

  @BeforeEach
  public void initialize()
  {
    resourceTypeFactory = new ResourceTypeFactory();
    UserEndpointDefinition userEndpoint = new UserEndpointDefinition(new UserHandlerImpl());
    resourceTypeFactory.registerResourceType(null,
                                             userEndpoint.getResourceType(),
                                             userEndpoint.getResourceSchema(),
                                             userEndpoint.getResourceSchemaExtensions().toArray(new JsonNode[0]));
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
    UriInfos uriInfos = UriInfos.getRequestUrlInfos(resourceTypeFactory, resourceUrl, httpMethod);
    Assertions.assertEquals(baseUrl, uriInfos.getBaseUri());
    Assertions.assertEquals(ResourceTypeNames.USER, uriInfos.getResourceType().getName());
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
    final String url = BASE_URI + EndpointPaths.USERS + "?" + query;
    UriInfos uriInfos = UriInfos.getRequestUrlInfos(resourceTypeFactory, url, HttpMethod.GET);
    Assertions.assertEquals(BASE_URI, uriInfos.getBaseUri());
    Assertions.assertEquals(EndpointPaths.USERS, uriInfos.getResourceEndpoint());
    Assertions.assertNull(uriInfos.getResourceId());
    Assertions.assertEquals(ResourceTypeNames.USER, uriInfos.getResourceType().getName());

    Map<String, String> parameter = uriInfos.getQueryParameters();
    Assertions.assertEquals(startIndex, parameter.get(AttributeNames.RFC7643.START_INDEX.toLowerCase()));
    Assertions.assertEquals(count, parameter.get(AttributeNames.RFC7643.COUNT));
    Assertions.assertEquals(URLDecoder.decode(filter, StandardCharsets.UTF_8.name()),
                            parameter.get(AttributeNames.RFC7643.FILTER));
    Assertions.assertEquals(sortBy, parameter.get(AttributeNames.RFC7643.SORT_BY.toLowerCase()));
    Assertions.assertEquals(sortOrder, parameter.get(AttributeNames.RFC7643.SORT_ORDER.toLowerCase()));
    Assertions.assertEquals(attributes, parameter.get(AttributeNames.RFC7643.ATTRIBUTES));
    Assertions.assertEquals(excludedAttributes,
                            parameter.get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES.toLowerCase()));
  }

  /**
   * will verify that a {@link BadRequestException} is thrown if the resource endpoint is unknown
   */
  @Test
  public void testUnknownResourceType()
  {
    final String url = BASE_URI + "/Unknown";
    Assertions.assertThrows(BadRequestException.class,
                            () -> UriInfos.getRequestUrlInfos(resourceTypeFactory, url, HttpMethod.GET));
  }

  /**
   * will verify that invalid parameter combinations in the request will lead to {@link BadRequestException}s
   */
  @ParameterizedTest
  @CsvSource({"POST,123456", "PUT,", "PATCH,", "DELETE,"})
  public void testInvalidRequestParameter(HttpMethod httpMethod, String id)
  {
    final String url = BASE_URI + EndpointPaths.USERS + (id == null ? "" : "/" + id);
    Assertions.assertThrows(BadRequestException.class,
                            () -> UriInfos.getRequestUrlInfos(resourceTypeFactory, url, httpMethod));
  }

  /**
   * verifies that no exceptions are thrown if the endpoint path points to bulk endpoint
   */
  @Test
  public void testParseBulkRequestAsUriInfo()
  {
    final String url = BASE_URI + EndpointPaths.BULK;
    Assertions.assertDoesNotThrow(() -> UriInfos.getRequestUrlInfos(resourceTypeFactory, url, HttpMethod.POST));
  }

  /**
   * will verify that calling the bulk endpoint with another {@link HttpMethod} than POST causes
   * {@link BadRequestException}s
   */
  @ParameterizedTest
  @ValueSource(strings = {"GET", "PUT", "DELETE", "PATCH"})
  public void testParseBulkRequestWithInvalidHttpMethods(HttpMethod httpMethod)
  {
    final String url = BASE_URI + EndpointPaths.BULK;
    Assertions.assertThrows(BadRequestException.class,
                            () -> UriInfos.getRequestUrlInfos(resourceTypeFactory, url, httpMethod));
  }
}
