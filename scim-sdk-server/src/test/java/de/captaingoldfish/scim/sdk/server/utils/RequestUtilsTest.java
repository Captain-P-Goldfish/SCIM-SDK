package de.captaingoldfish.scim.sdk.server.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.complex.FilterConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.PaginationConfig;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 13.10.2019 - 15:43 <br>
 * <br>
 */
@Slf4j
public class RequestUtilsTest
{

  @ParameterizedTest
  @ValueSource(strings = {"p1=v1", "p1=v1&p2=v2", "&p1=v1&&p2=v2&", "p1=&p2=", "p1&p2", "=v1&=v2"})
  public void testGetQueryParameters(String query)
  {
    Assertions.assertDoesNotThrow(() -> RequestUtils.getQueryParameters(query));
  }

  /**
   * RFC 9865: an absent cursor query parameter must yield {@link Optional#empty()}; an empty-string value must
   * round-trip as {@code Optional.of("")} so callers can detect the "first page" signal.
   */
  @Test
  @DisplayName("parseCursor distinguishes missing from empty (RFC 9865)")
  public void testParseCursorDistinguishesMissingFromEmpty()
  {
    Assertions.assertFalse(RequestUtils.parseCursor(null).isPresent());
    Assertions.assertEquals("", RequestUtils.parseCursor("").orElse(null));
    Assertions.assertEquals("abc", RequestUtils.parseCursor("abc").orElse(null));
  }

  /**
   * RFC 9865: cursor-mode count must NOT silently clamp negative values; it must raise {@code invalidCount}.
   */
  @Test
  @DisplayName("getEffectiveCursorCount rejects negative count with invalidCount (RFC 9865)")
  public void testGetEffectiveCursorCountRejectsNegative()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .filterConfig(FilterConfig.builder().maxResults(100).build())
                                                     .paginationConfig(PaginationConfig.builder().cursor(true).build())
                                                     .build();
    BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                     () -> RequestUtils.getEffectiveCursorCount(serviceProvider, -1));
    Assertions.assertEquals(ScimType.RFC9865.INVALID_COUNT, ex.getScimType());
  }

  /**
   * RFC 9865 permits both clamping and rejecting a count that exceeds {@code maxPageSize}. The SDK clamps —
   * matching how index mode behaves — so clients that have not consulted {@code ServiceProviderConfig} still
   * get useful results.
   */
  @Test
  @DisplayName("getEffectiveCursorCount clamps values above maxPageSize to maxPageSize (RFC 9865)")
  public void testGetEffectiveCursorCountClampsAboveMaxPageSize()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .filterConfig(FilterConfig.builder().maxResults(50).build())
                                                     .paginationConfig(PaginationConfig.builder()
                                                                                       .cursor(true)
                                                                                       .maxPageSize(20)
                                                                                       .build())
                                                     .build();
    Assertions.assertEquals(20, RequestUtils.getEffectiveCursorCount(serviceProvider, 25));
  }

  /**
   * {@code count=0} is a documented SCIM discovery pattern (RFC 7644 §3.4.2.4 — count is a non-negative
   * integer): clients use it to fetch only {@code totalResults} without paying for the resource bodies. Cursor
   * mode must accept it for the same reason index mode does.
   */
  @Test
  @DisplayName("getEffectiveCursorCount accepts count=0 (RFC 7644 discovery pattern)")
  public void testGetEffectiveCursorCountAcceptsZero()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .filterConfig(FilterConfig.builder().maxResults(50).build())
                                                     .paginationConfig(PaginationConfig.builder().cursor(true).build())
                                                     .build();
    Assertions.assertEquals(0, RequestUtils.getEffectiveCursorCount(serviceProvider, 0));
  }

  /**
   * A {@code null} cursor-mode count falls back to {@code PaginationConfig.defaultPageSize} when configured.
   */
  @Test
  @DisplayName("getEffectiveCursorCount falls back to defaultPageSize when count is null")
  public void testGetEffectiveCursorCountUsesDefaultPageSize()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .filterConfig(FilterConfig.builder().maxResults(100).build())
                                                     .paginationConfig(PaginationConfig.builder()
                                                                                       .cursor(true)
                                                                                       .defaultPageSize(25)
                                                                                       .build())
                                                     .build();
    Assertions.assertEquals(25, RequestUtils.getEffectiveCursorCount(serviceProvider, null));
  }

  /**
   * When {@code PaginationConfig.defaultPageSize} is not configured, the fallback is the effective max page
   * size — and when {@code maxPageSize} is also unset, that resolves to {@code FilterConfig.maxResults} so
   * deployments that never opted in to RFC 9865's pagination config still get a sensible default.
   */
  @Test
  @DisplayName("getEffectiveCursorCount falls back to FilterConfig.maxResults when neither defaultPageSize nor maxPageSize is set")
  public void testGetEffectiveCursorCountFallsBackToFilterMaxResults()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .filterConfig(FilterConfig.builder().maxResults(40).build())
                                                     .paginationConfig(PaginationConfig.builder().cursor(true).build())
                                                     .build();
    Assertions.assertEquals(40, RequestUtils.getEffectiveCursorCount(serviceProvider, null));
  }

  /**
   * When {@code PaginationConfig.defaultPageSize} is not configured but {@code maxPageSize} is, the
   * {@code null} count must fall back to {@code maxPageSize} — never higher than what the service provider is
   * willing to return on a single page.
   */
  @Test
  @DisplayName("getEffectiveCursorCount falls back to maxPageSize when defaultPageSize is unset")
  public void testGetEffectiveCursorCountFallsBackToMaxPageSize()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .filterConfig(FilterConfig.builder().maxResults(100).build())
                                                     .paginationConfig(PaginationConfig.builder()
                                                                                       .cursor(true)
                                                                                       .maxPageSize(25)
                                                                                       .build())
                                                     .build();
    Assertions.assertEquals(25, RequestUtils.getEffectiveCursorCount(serviceProvider, null));
  }

  /**
   * Round-trip for the SDK's offset-cursor codec: {@code encodeOffsetCursor} should produce a value that
   * {@code decodeOffsetCursor} restores to the original offset, and the empty cursor must mean offset {@code 0}
   * (RFC 9865 §2.1 first-page signal).
   */
  @Test
  @DisplayName("offset cursor codec round-trips and treats the empty cursor as first page")
  public void testOffsetCursorRoundTrip()
  {
    Assertions.assertEquals(1, RequestUtils.decodeCursorStartIndex(""));
    Assertions.assertEquals(1, RequestUtils.decodeCursorStartIndex(null));
    Assertions.assertEquals(1, RequestUtils.decodeCursorStartIndex(RequestUtils.encodeStartIndexCursor(0)));
    for ( int startIndex : new int[]{1, 5, 100, 1234, Integer.MAX_VALUE} )
    {
      String encodedStartIndex = RequestUtils.encodeStartIndexCursor(startIndex);
      Assertions.assertEquals(startIndex,
                              RequestUtils.decodeCursorStartIndex(encodedStartIndex),
                              "round-trip failed for " + startIndex);
    }
  }

  /**
   * A cursor the SDK cannot decode must NOT throw here: a cursor is opaque to the SDK, so an undecodable value
   * is assumed to be a cursor the resource handler generated itself (e.g. a keyset cursor) and is reported with
   * the sentinel {@code -1}. It is only rejected with {@code invalidCursor} (RFC 9865 §3) when it reaches the
   * index-based auto-bridge, i.e. when the handler does not implement cursor pagination (covered by
   * {@code ResourceEndpointHandlerTest.testCursorAutoBridgeRejectsMalformedCursor}).
   */
  @Test
  @DisplayName("decodeCursorStartIndex returns the -1 sentinel for cursors it cannot decode")
  public void testDecodeCursorStartIndexReturnsSentinelForCustomCursor()
  {
    for ( String custom : new String[]{"not-base64!@#", "bm9uLW51bWVyaWM"/* "non-numeric" */, "LTU"/* "-5" */} )
    {
      Assertions.assertEquals(-1,
                              RequestUtils.decodeCursorStartIndex(custom),
                              "expected the -1 sentinel for '" + custom + "'");
    }
  }

  @Nested
  class TestsWithResourceType
  {

    private ResourceType userResourceType;

    @BeforeEach
    public void beforeEach()
    {
      ResourceTypeFactory resourceTypeFactory = new ResourceTypeFactory();
      UserEndpointDefinition userEndpoint = new UserEndpointDefinition(new UserHandlerImpl(true));
      userResourceType = resourceTypeFactory.registerResourceType(null,
                                                                  userEndpoint.getResourceType(),
                                                                  userEndpoint.getResourceSchema(),
                                                                  userEndpoint.getResourceSchemaExtensions()
                                                                              .toArray(new JsonNode[0]));
    }

    @DisplayName("All user attributes can be added to 'attributes'/'excludedAttributes'-paramater")
    @TestFactory
    public List<DynamicNode> testGetAttributesListWorksAsExpected()
    {
      List<DynamicNode> dynamicNodeList = new ArrayList<>();

      Map<SchemaAttribute, List<DynamicTest>> testGroups = new HashMap<>();


      for ( SchemaAttribute schemaAttribute : userResourceType.getAttributeRegister().values() )
      {
        SchemaAttribute testMappingAttribute = Optional.ofNullable(schemaAttribute.getParent()).orElse(schemaAttribute);
        List<DynamicTest> dynamicTests = testGroups.computeIfAbsent(testMappingAttribute, k -> new ArrayList<>());
        dynamicTests.add(DynamicTest.dynamicTest(schemaAttribute.getScimNodeName(), () -> {
          Assertions.assertDoesNotThrow(() -> RequestUtils.getAttributes(userResourceType,
                                                                         schemaAttribute.getScimNodeName()));
        }));
      }

      // now add the dynamic test-structure
      for ( SchemaAttribute schemaAttribute : testGroups.keySet() )
      {
        SchemaAttribute testMappingAttribute = Optional.ofNullable(schemaAttribute.getParent()).orElse(schemaAttribute);
        List<DynamicTest> attributeTests = testGroups.get(testMappingAttribute);
        if (attributeTests.size() == 1)
        {
          dynamicNodeList.add(attributeTests.get(0));
        }
        else
        {
          DynamicContainer complexContainer = DynamicContainer.dynamicContainer(schemaAttribute.getScimNodeName(),
                                                                                attributeTests);
          dynamicNodeList.add(complexContainer);
        }
      }
      return dynamicNodeList;
    }

    @DisplayName("Throw bad request if limited filter-depth is exceeded")
    @Test
    public void testFilterDepthIsLimited()
    {
      final int maxFilterDepth = 10;
      ServiceProvider serviceProvider = ServiceProvider.builder()
                                                       .filterConfig(FilterConfig.builder()
                                                                                 .maxResults(100)
                                                                                 .maxFilterDepth(maxFilterDepth)
                                                                                 .build())
                                                       .build();
      UserHandlerImpl userHandler = new UserHandlerImpl(false)
      {

        @Override
        public ServiceProvider getServiceProvider()
        {
          return serviceProvider;
        }
      };
      userResourceType.setResourceHandlerImpl(userHandler);

      Random random = new Random();
      StringBuilder stringBuilder = new StringBuilder();
      for ( int i = 0 ; i < maxFilterDepth + 1 ; i++ )
      {
        if (i > 0)
        {
          stringBuilder.append(" ").append(random.nextBoolean() ? "or" : "and").append(" ");
        }
        stringBuilder.append("userName eq \"test" + 1 + "\"");
      }
      BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                       () -> RequestUtils.parseFilter(userResourceType,
                                                                                      stringBuilder.toString()));
      Assertions.assertEquals("Filter depth exceeded maximum allowed depth is '10'", ex.getMessage());
    }

    @DisplayName("Throw bad request on filter that is too large")
    @Test
    public void testParseTooLargeFilter()
    {
      final int maxFilterDepth = 3500;
      ServiceProvider serviceProvider = ServiceProvider.builder()
                                                       .filterConfig(FilterConfig.builder()
                                                                                 .maxResults(100)
                                                                                 .maxFilterDepth(maxFilterDepth)
                                                                                 .build())
                                                       .build();
      UserHandlerImpl userHandler = new UserHandlerImpl(false)
      {

        @Override
        public ServiceProvider getServiceProvider()
        {
          return serviceProvider;
        }
      };
      userResourceType.setResourceHandlerImpl(userHandler);

      StringBuilder stringBuilder = new StringBuilder();
      for ( int i = 0 ; i < maxFilterDepth + 1 ; i++ )
      {
        if (i > 0)
        {
          stringBuilder.append(" or ");
        }
        stringBuilder.append("userName eq \"test" + 1 + "\"");
      }
      BadRequestException ex = Assertions.assertThrows(BadRequestException.class,
                                                       () -> RequestUtils.parseFilter(userResourceType,
                                                                                      stringBuilder.toString()));
      Assertions.assertEquals("Failed to parse patch-filter expression. Filter is too large", ex.getMessage());
    }
  }


}
