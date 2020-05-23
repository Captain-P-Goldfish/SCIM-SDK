package de.captaingoldfish.scim.sdk.client.builder;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.Comparator;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.request.SearchRequest;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.filter.antlr.FilterRuleErrorListener;
import de.captaingoldfish.scim.sdk.server.filter.antlr.ScimFilterLexer;
import de.captaingoldfish.scim.sdk.server.filter.antlr.ScimFilterParser;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;


/**
 * author Pascal Knueppel <br>
 * created at: 16.12.2019 - 14:11 <br>
 * <br>
 */
public class ListBuilderTest extends HttpServerMockup
{

  /**
   * creates filter test parameters with strings
   */
  private static Stream<Arguments> filterBuilderParamsString()
  {
    return Stream.of(Arguments.of("nickname", Comparator.EQ, "1"),
                     Arguments.of("nickname", Comparator.NE, "4"),
                     Arguments.of("nickname", Comparator.CO, "8"),
                     Arguments.of("nickname", Comparator.EW, "2"),
                     Arguments.of("nickname", Comparator.SW, "3"),
                     Arguments.of("nickname", Comparator.GE, "4"),
                     Arguments.of("nickname", Comparator.GT, "50"),
                     Arguments.of("nickname", Comparator.LT, "40"),
                     Arguments.of("nickname", Comparator.LE, "5"),
                     Arguments.of("nickname", Comparator.PR, null));
  }

  /**
   * creates filter test parameters with date times
   */
  private static Stream<Arguments> filterBuilderParamsInstant()
  {
    return Stream.of(Arguments.of("nickname", Comparator.EQ, Instant.now()),
                     Arguments.of("nickname", Comparator.NE, Instant.now()),
                     Arguments.of("nickname", Comparator.CO, Instant.now()),
                     Arguments.of("nickname", Comparator.EW, Instant.now()),
                     Arguments.of("nickname", Comparator.SW, Instant.now()),
                     Arguments.of("nickname", Comparator.GE, Instant.now()),
                     Arguments.of("nickname", Comparator.GT, Instant.now()),
                     Arguments.of("nickname", Comparator.LT, Instant.now()),
                     Arguments.of("nickname", Comparator.LE, Instant.now()),
                     Arguments.of("nickname", Comparator.PR, null));
  }

  /**
   * creates filter test parameters with integers
   */
  private static Stream<Arguments> filterBuilderParamsInteger()
  {
    return Stream.of(Arguments.of("nickname", Comparator.EQ, 1),
                     Arguments.of("nickname", Comparator.NE, 4),
                     Arguments.of("nickname", Comparator.CO, 8),
                     Arguments.of("nickname", Comparator.EW, 2),
                     Arguments.of("nickname", Comparator.SW, 3),
                     Arguments.of("nickname", Comparator.GE, 4),
                     Arguments.of("nickname", Comparator.GT, 50),
                     Arguments.of("nickname", Comparator.LT, 40),
                     Arguments.of("nickname", Comparator.LE, 5),
                     Arguments.of("nickname", Comparator.PR, null));
  }

  /**
   * creates filter test parameters with longs
   */
  private static Stream<Arguments> filterBuilderParamsLong()
  {
    return Stream.of(Arguments.of("nickname", Comparator.EQ, 1L),
                     Arguments.of("nickname", Comparator.NE, 4L),
                     Arguments.of("nickname", Comparator.CO, 8L),
                     Arguments.of("nickname", Comparator.EW, 2L),
                     Arguments.of("nickname", Comparator.SW, 3L),
                     Arguments.of("nickname", Comparator.GE, 4L),
                     Arguments.of("nickname", Comparator.GT, 50L),
                     Arguments.of("nickname", Comparator.LT, 40L),
                     Arguments.of("nickname", Comparator.LE, 5L),
                     Arguments.of("nickname", Comparator.PR, null));
  }

  /**
   * creates filter test parameters with doubles
   */
  private static Stream<Arguments> filterBuilderParamsDouble()
  {
    return Stream.of(Arguments.of("nickname", Comparator.EQ, 1.0),
                     Arguments.of("nickname", Comparator.NE, 4.0),
                     Arguments.of("nickname", Comparator.CO, 8.0),
                     Arguments.of("nickname", Comparator.EW, 2.0),
                     Arguments.of("nickname", Comparator.SW, 3.0),
                     Arguments.of("nickname", Comparator.GE, 4.0),
                     Arguments.of("nickname", Comparator.GT, 50.0),
                     Arguments.of("nickname", Comparator.LT, 40.0),
                     Arguments.of("nickname", Comparator.LE, 5.0),
                     Arguments.of("nickname", Comparator.PR, null));
  }

  /**
   * creates filter test parameters with boolean
   */
  private static Stream<Arguments> filterBuilderParamsBoolean()
  {
    return Stream.of(Arguments.of("nickname", Comparator.EQ, true),
                     Arguments.of("nickname", Comparator.NE, true),
                     Arguments.of("nickname", Comparator.EQ, false),
                     Arguments.of("nickname", Comparator.NE, false),
                     Arguments.of("nickname", Comparator.PR, null));
  }

  @ParameterizedTest
  @MethodSource("filterBuilderParamsString")
  public void testBuildFilterWithStrings(String attributeName, Comparator comparator, String value)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder<User> listBuilder = new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class, scimHttpClient);
    listBuilder.filter("username", Comparator.SW, "hello_world")
               .or(attributeName, comparator, value)
               .or(true, attributeName, comparator, value)
               .closeParenthesis()
               .build();
    final String filter = listBuilder.getRequestParameters().get(AttributeNames.RFC7643.FILTER);
    String val = Comparator.PR.equals(comparator) ? "" : " \"" + value + "\"";
    Assertions.assertEquals("username SW \"hello_world\" or " + attributeName + " " + comparator + val + " or ("
                            + attributeName + " " + comparator + val + ")",
                            filter);
    parseFilterWithAntlr(filter);
  }

  /**
   * verifies that using the full url does also work with get
   */
  @ParameterizedTest
  @MethodSource("filterBuilderParamsString")
  public void testBuildFilterWithStringsWithFullUrl(String attributeName, Comparator comparator, String value)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder<User> listBuilder = new ListBuilder<>(getServerUrl() + EndpointPaths.USERS, User.class, scimHttpClient);
    listBuilder.filter("username", Comparator.SW, "hello_world")
               .or(attributeName, comparator, value)
               .or(true, attributeName, comparator, value)
               .closeParenthesis()
               .build();
    final String filter = listBuilder.getRequestParameters().get(AttributeNames.RFC7643.FILTER);
    String val = Comparator.PR.equals(comparator) ? "" : " \"" + value + "\"";
    Assertions.assertEquals("username SW \"hello_world\" or " + attributeName + " " + comparator + val + " or ("
                            + attributeName + " " + comparator + val + ")",
                            filter);
    parseFilterWithAntlr(filter);
  }

  @ParameterizedTest
  @MethodSource("filterBuilderParamsInteger")
  public void testBuildFilterWithIntegers(String attributeName, Comparator comparator, Integer value)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder<User> listBuilder = new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class, scimHttpClient);
    listBuilder.filter("username", Comparator.SW, "hello_world")
               .or(attributeName, comparator, value)
               .or(true, attributeName, comparator, value)
               .closeParenthesis()
               .build();
    final String filter = listBuilder.getRequestParameters().get(AttributeNames.RFC7643.FILTER);
    String val = Comparator.PR.equals(comparator) ? "" : " " + value;
    Assertions.assertEquals("username SW \"hello_world\" or " + attributeName + " " + comparator + val + " or ("
                            + attributeName + " " + comparator + val + ")",
                            filter);

    parseFilterWithAntlr(filter);
  }


  @ParameterizedTest
  @MethodSource("filterBuilderParamsLong")
  public void testBuildFilterWithLongs(String attributeName, Comparator comparator, Long value)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder<User> listBuilder = new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class, scimHttpClient);
    listBuilder.filter("username", Comparator.SW, "hello_world")
               .or(attributeName, comparator, value)
               .or(true, attributeName, comparator, value)
               .closeParenthesis()
               .build();
    final String filter = listBuilder.getRequestParameters().get(AttributeNames.RFC7643.FILTER);
    String val = Comparator.PR.equals(comparator) ? "" : " " + value;
    Assertions.assertEquals("username SW \"hello_world\" or " + attributeName + " " + comparator + val + " or ("
                            + attributeName + " " + comparator + val + ")",
                            filter);
    parseFilterWithAntlr(filter);
  }


  @ParameterizedTest
  @MethodSource("filterBuilderParamsDouble")
  public void testBuildFilterWithDoubles(String attributeName, Comparator comparator, Double value)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder<User> listBuilder = new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class, scimHttpClient);
    listBuilder.filter("username", Comparator.SW, "hello_world")
               .or(attributeName, comparator, value)
               .or(true, attributeName, comparator, value)
               .closeParenthesis()
               .build();
    final String filter = listBuilder.getRequestParameters().get(AttributeNames.RFC7643.FILTER);
    String val = Comparator.PR.equals(comparator) ? "" : " " + value;
    Assertions.assertEquals("username SW \"hello_world\" or " + attributeName + " " + comparator + val + " or ("
                            + attributeName + " " + comparator + val + ")",
                            filter);
    parseFilterWithAntlr(filter);
  }


  @ParameterizedTest
  @MethodSource("filterBuilderParamsBoolean")
  public void testBuildFilterWithBooleans(String attributeName, Comparator comparator, Boolean value)
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder<User> listBuilder = new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class, scimHttpClient);
    listBuilder.filter("username", Comparator.SW, "hello_world")
               .or(attributeName, comparator, value)
               .or(true, attributeName, comparator, value)
               .closeParenthesis()
               .build();
    final String filter = listBuilder.getRequestParameters().get(AttributeNames.RFC7643.FILTER);
    String val = Comparator.PR.equals(comparator) ? "" : " " + value;
    Assertions.assertEquals("username SW \"hello_world\" or " + attributeName + " " + comparator + val + " or ("
                            + attributeName + " " + comparator + val + ")",
                            filter);
    parseFilterWithAntlr(filter);
  }

  /**
   * builds a complex filter and shows that the filter is setup correctly
   */
  @Test
  public void testBuildComplexFilter()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder listBuilder = new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class, scimHttpClient);
    Instant instant = Instant.now();
    listBuilder.filter(true, "username", Comparator.SW, "hello_world")
               .or(true, "nickname", Comparator.CO, "hello")
               .and(true, "locale", Comparator.EQ, "DE")
               .and("name.givenname", Comparator.CO, "world", true)
               .closeParenthesis()
               .and("meta.created", Comparator.GE, "2019-05-01T13:21:54.157894+02:00", true)
               .and("meta.created", Comparator.GE, instant)
               .build();
    Assertions.assertEquals("(username SW \"hello_world\" or (nickname CO \"hello\" and "
                            + "(locale EQ \"DE\" and name.givenname CO \"world\")) and "
                            + "meta.created GE \"2019-05-01T13:21:54.157894+02:00\") and meta.created GE " + "\""
                            + instant.toString() + "\"",
                            listBuilder.getRequestParameters().get(AttributeNames.RFC7643.FILTER));
  }

  /**
   * verifies that a list request can successfully be sent with get to the server with all parameters
   */
  @Test
  public void testSendListGetRequest()
  {
    final String sortBy = "username";
    final Integer count = 15;
    final Long startIndex = 15L;
    final SortOrder sortOrder = SortOrder.DESCENDING;
    final String[] attributes = new String[]{"username", "meta.created"};

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder<User> listBuilder = new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class,
                                                      scimHttpClient).sortBy(sortBy)
                                                                     .count(count)
                                                                     .startIndex(startIndex)
                                                                     .sortOrder(sortOrder)
                                                                     .excludedAttributes(attributes)
                                                                     .filter("username", Comparator.SW, "a")
                                                                     .build();

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    super.setVerifyRequestAttributes((httpExchange, requestBody) -> {
      Map<String, String> parameters = RequestUtils.getQueryParameters(httpExchange.getRequestURI()
                                                                                   .toString()
                                                                                   .split("\\?")[1]);
      Assertions.assertEquals(sortBy, parameters.get(AttributeNames.RFC7643.SORT_BY.toLowerCase()));
      Assertions.assertEquals(String.valueOf(count), parameters.get(AttributeNames.RFC7643.COUNT.toLowerCase()));
      Assertions.assertEquals(String.valueOf(startIndex),
                              parameters.get(AttributeNames.RFC7643.START_INDEX.toLowerCase()));
      Assertions.assertEquals("username SW \"a\"", parameters.get(AttributeNames.RFC7643.FILTER.toLowerCase()));
      Assertions.assertEquals(sortOrder.name().toLowerCase(),
                              parameters.get(AttributeNames.RFC7643.SORT_ORDER.toLowerCase()));
      Assertions.assertEquals(String.join(",", attributes),
                              parameters.get(AttributeNames.RFC7643.EXCLUDED_ATTRIBUTES.toLowerCase()));
      wasCalled.set(true);
    });

    ServerResponse<ListResponse<User>> response = listBuilder.get().sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());

    ListResponse<User> listResponse = response.getResource();
    Assertions.assertEquals(count, listResponse.getItemsPerPage());
    Assertions.assertEquals(startIndex, listResponse.getStartIndex());
    Assertions.assertEquals(count, listResponse.getListedResources().size());

    Assertions.assertTrue(wasCalled.get());
  }

  /**
   * verifies that a list request can successfully be sent with post to the server with all parameters
   */
  @Test
  public void testSendListPostRequest()
  {
    final String sortBy = "username";
    final Integer count = 15;
    final Long startIndex = 15L;
    final SortOrder sortOrder = SortOrder.DESCENDING;
    final String[] attributes = new String[]{"username", "meta.created"};

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder<User> listBuilder = new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class,
                                                      scimHttpClient).sortBy(sortBy)
                                                                     .count(count)
                                                                     .startIndex(startIndex)
                                                                     .sortOrder(sortOrder)
                                                                     .attributes(attributes)
                                                                     .filter("username", Comparator.SW, "a")
                                                                     .build();

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    super.setVerifyRequestAttributes((httpExchange, requestBody) -> {
      SearchRequest searchRequest = JsonHelper.readJsonDocument(requestBody, SearchRequest.class);
      Assertions.assertEquals(sortBy, searchRequest.getSortBy().get());
      Assertions.assertEquals(count, searchRequest.getCount().get());
      Assertions.assertEquals(startIndex, searchRequest.getStartIndex().get());
      Assertions.assertEquals("username SW \"a\"", searchRequest.getFilter().get());
      Assertions.assertEquals(sortOrder.name().toLowerCase(), searchRequest.getSortOrder().get());
      Assertions.assertEquals(String.join(",", attributes), searchRequest.getAttributes().get());
      wasCalled.set(true);
    });

    ServerResponse<ListResponse<User>> response = listBuilder.post().sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    ListResponse<User> listResponse = response.getResource();
    Assertions.assertEquals(count, listResponse.getItemsPerPage());
    Assertions.assertEquals(startIndex, listResponse.getStartIndex());
    Assertions.assertEquals(count, listResponse.getListedResources().size());

    Assertions.assertTrue(wasCalled.get());

    Assertions.assertDoesNotThrow(() -> response.getResource().getListedResources().get(0));
  }

  /**
   * verifies that a list request does also work if the fully qualified url is used
   */
  @Test
  public void testSendListPostRequestWithFullUrl()
  {
    final String sortBy = "username";
    final Integer count = 15;
    final Long startIndex = 15L;
    final SortOrder sortOrder = SortOrder.DESCENDING;
    final String[] attributes = new String[]{"username", "meta.created"};

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder<User> listBuilder = new ListBuilder<>(getServerUrl() + EndpointPaths.USERS + "/.search", User.class,
                                                      scimHttpClient).sortBy(sortBy)
                                                                     .count(count)
                                                                     .startIndex(startIndex)
                                                                     .sortOrder(sortOrder)
                                                                     .attributes(attributes)
                                                                     .filter("username", Comparator.SW, "a")
                                                                     .build();

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    super.setVerifyRequestAttributes((httpExchange, requestBody) -> {
      SearchRequest searchRequest = JsonHelper.readJsonDocument(requestBody, SearchRequest.class);
      Assertions.assertEquals(sortBy, searchRequest.getSortBy().get());
      Assertions.assertEquals(count, searchRequest.getCount().get());
      Assertions.assertEquals(startIndex, searchRequest.getStartIndex().get());
      Assertions.assertEquals("username SW \"a\"", searchRequest.getFilter().get());
      Assertions.assertEquals(sortOrder.name().toLowerCase(), searchRequest.getSortOrder().get());
      Assertions.assertEquals(String.join(",", attributes), searchRequest.getAttributes().get());
      wasCalled.set(true);
    });

    ServerResponse<ListResponse<User>> response = listBuilder.post().sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    ListResponse<User> listResponse = response.getResource();
    Assertions.assertEquals(count, listResponse.getItemsPerPage());
    Assertions.assertEquals(startIndex, listResponse.getStartIndex());
    Assertions.assertEquals(count, listResponse.getListedResources().size());

    Assertions.assertTrue(wasCalled.get());

    Assertions.assertDoesNotThrow(() -> response.getResource().getListedResources().get(0));
  }

  /**
   * verifies that a list request does also work if the fully qualified url is used that is still missing the "/
   * .search" path at the end of the url
   */
  @Test
  public void testSendListPostRequestWithFullUrlWithoutSearchValue()
  {
    final String sortBy = "username";
    final Integer count = 15;
    final Long startIndex = 15L;
    final SortOrder sortOrder = SortOrder.DESCENDING;
    final String[] attributes = new String[]{"username", "meta.created"};

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder<User> listBuilder = new ListBuilder<>(getServerUrl() + EndpointPaths.USERS, User.class,
                                                      scimHttpClient).sortBy(sortBy)
                                                                     .count(count)
                                                                     .startIndex(startIndex)
                                                                     .sortOrder(sortOrder)
                                                                     .attributes(attributes)
                                                                     .filter("username", Comparator.SW, "a")
                                                                     .build();

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    super.setVerifyRequestAttributes((httpExchange, requestBody) -> {
      SearchRequest searchRequest = JsonHelper.readJsonDocument(requestBody, SearchRequest.class);
      Assertions.assertEquals(sortBy, searchRequest.getSortBy().get());
      Assertions.assertEquals(count, searchRequest.getCount().get());
      Assertions.assertEquals(startIndex, searchRequest.getStartIndex().get());
      Assertions.assertEquals("username SW \"a\"", searchRequest.getFilter().get());
      Assertions.assertEquals(sortOrder.name().toLowerCase(), searchRequest.getSortOrder().get());
      Assertions.assertEquals(String.join(",", attributes), searchRequest.getAttributes().get());
      wasCalled.set(true);
    });

    ServerResponse<ListResponse<User>> response = listBuilder.post().sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getResource());
    Assertions.assertNull(response.getErrorResponse());
    ListResponse<User> listResponse = response.getResource();
    Assertions.assertEquals(count, listResponse.getItemsPerPage());
    Assertions.assertEquals(startIndex, listResponse.getStartIndex());
    Assertions.assertEquals(count, listResponse.getListedResources().size());

    Assertions.assertTrue(wasCalled.get());

    Assertions.assertDoesNotThrow(() -> response.getResource().getListedResources().get(0));
  }

  /**
   * builds a filter that has only a single closed parenthesis
   */
  @Test
  public void testBuildFilterWithErroneousClosedParenthesis()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder listBuilder = new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class, scimHttpClient);
    try
    {
      listBuilder.filter("username", Comparator.SW, "hello_world").closeParenthesis().build();
      Assertions.fail("this point must not be reached");
    }
    catch (IllegalStateException ex)
    {
      Assertions.assertEquals("error within filter expression\n\topened parentheses: 0\n\t"
                              + "closed parentheses: 1\n\tfilter: username SW \"hello_world\")",
                              ex.getMessage());
    }
  }

  /**
   * builds a filter that has only a single opened parenthesis
   */
  @Test
  public void testBuildFilterWithErroneousOpenedParenthesis()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ListBuilder listBuilder = new ListBuilder<>(getServerUrl(), EndpointPaths.USERS, User.class, scimHttpClient);
    try
    {
      listBuilder.filter(true, "username", Comparator.SW, "hello_world").build();
      Assertions.fail("this point must not be reached");
    }
    catch (IllegalStateException ex)
    {
      Assertions.assertEquals("error within filter expression\n\topened parentheses: 1"
                              + "\n\tclosed parentheses: 0\n\tfilter: (username SW \"hello_world\"",
                              ex.getMessage());
    }
  }

  private void parseFilterWithAntlr(String filter)
  {
    FilterRuleErrorListener filterRuleErrorListener = new FilterRuleErrorListener();
    ScimFilterLexer lexer = new ScimFilterLexer(CharStreams.fromString(filter));
    lexer.removeErrorListeners();
    lexer.addErrorListener(filterRuleErrorListener);
    CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
    ScimFilterParser scimFilterParser = new ScimFilterParser(commonTokenStream);
    scimFilterParser.removeErrorListeners();
    scimFilterParser.addErrorListener(filterRuleErrorListener);
    Assertions.assertDoesNotThrow(scimFilterParser::filter);
  }
}
