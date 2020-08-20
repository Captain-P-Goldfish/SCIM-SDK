package de.captaingoldfish.scim.sdk.server.etag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.exceptions.NotModifiedException;
import de.captaingoldfish.scim.sdk.common.exceptions.PreconditionFailedException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.ETagConfig;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;


/**
 * author Pascal Knueppel <br>
 * created at: 21.11.2019 - 17:03 <br>
 * <br>
 */
public class ETagHandlerTest
{

  /**
   * a unit test schema factory instance
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the user resource type declaration
   */
  private ResourceType userResourceType;

  /**
   * initializes the schema factory instance for unit tests
   */
  @BeforeEach
  public void initialize()
  {
    resourceTypeFactory = new ResourceTypeFactory();
    userResourceType = resourceTypeFactory.registerResourceType(null,
                                                                JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON),
                                                                JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON),
                                                                JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON));
    userResourceType.getFeatures().getETagFeature().setEnabled(true);
  }

  /**
   * verifies that no etag is build if the service provider does not support etags
   */
  @Test
  public void testReturnEmptyIfEtagSupportIsDisabled()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(false).build())
                                                     .build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").build();
    Assertions.assertFalse(ETagHandler.getResourceVersion(serviceProvider, userResourceType, user).isPresent());
  }

  /**
   * verifies that a base64 encoded SHA-1 hash is created if the given resource node has no version yet
   */
  @Test
  public void testCreateNewResourceVersion() throws NoSuchAlgorithmException
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(true).build())
                                                     .build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").build();
    Optional<ETag> version = ETagHandler.getResourceVersion(serviceProvider, userResourceType, user);
    Assertions.assertTrue(version.isPresent());
    ETag eTag = version.get();
    MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
    String userVersion = Base64.getEncoder()
                               .encodeToString(messageDigest.digest(user.toString().getBytes(StandardCharsets.UTF_8)));
    Assertions.assertEquals(userVersion, eTag.getTag(), "version must be a base64 encoded SHA-1 hash");
    Assertions.assertTrue(eTag.isWeak(), "such an etag must definitely be a weak entity tag");
  }

  /**
   * verifies that no version is automatically created if the developer has already set one
   */
  @Test
  public void testGetAlreadyExistingVersion()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(true).build())
                                                     .build();
    final String versionTag = "123456";
    Meta meta = Meta.builder().version(versionTag).build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").meta(meta).build();
    Optional<ETag> version = ETagHandler.getResourceVersion(serviceProvider, userResourceType, user);
    Assertions.assertTrue(version.isPresent());
    ETag eTag = version.get();
    Assertions.assertTrue(eTag.isWeak(), "such an etag must definitely be a weak entity tag");
    Assertions.assertEquals(versionTag, eTag.getTag());
  }

  /**
   * verifies that an exception is thrown if both headers are present within the request
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBothHeadersExistInRequest(boolean ifNot)
  {
    Map<String, String> httpHeaders = new HashMap<>();
    httpHeaders.put(HttpHeader.IF_MATCH_HEADER, ETag.builder().tag(UUID.randomUUID().toString()).build().toString());
    httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER,
                    ETag.builder().tag(UUID.randomUUID().toString()).build().toString());
    try
    {
      ETagHandler.getETagFromHeader(httpHeaders, ifNot);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals("the http header '" + HttpHeader.IF_MATCH_HEADER + "' and '"
                              + HttpHeader.IF_NONE_MATCH_HEADER + "' are mutually"
                              + " exclusive you should only send one of these headers per request",
                              ex.getDetail());
    }
  }

  /**
   * will show that the Etag value can successfully be extracted from the request header map
   */
  @ParameterizedTest
  @ValueSource(strings = {HttpHeader.IF_MATCH_HEADER, "if-match", "IF-MATCH", "iF-mAtCh"})
  public void testGetIfMatchEtagFromRequestHeader(String httpHeader)
  {
    Map<String, String> httpHeaders = new HashMap<>();
    String versionTag = UUID.randomUUID().toString();
    httpHeaders.put(httpHeader, ETag.builder().tag(versionTag).build().toString());
    Optional<ETag> eTag = ETagHandler.getETagFromHeader(httpHeaders, false);
    Assertions.assertTrue(eTag.isPresent());
    Assertions.assertEquals(versionTag, eTag.get().getTag());
    Assertions.assertTrue(eTag.get().isWeak());
  }

  /**
   * verifies that the header is not evaluated if an empty value is used
   */
  @Test
  public void testGetIfMatchEtagFromRequestHeaderButWithEmptyValue()
  {
    Map<String, String> httpHeaders = new HashMap<>();
    httpHeaders.put(HttpHeader.IF_MATCH_HEADER, ETag.builder().tag(null).build().toString());
    Optional<ETag> eTag = ETagHandler.getETagFromHeader(httpHeaders, false);
    Assertions.assertFalse(eTag.isPresent());
  }

  /**
   * verifies that the header is not evaluated if an empty value is used
   */
  @Test
  public void testGetIfMatchEtagFromRequestHeaderButWithNull()
  {
    Map<String, String> httpHeaders = new HashMap<>();
    httpHeaders.put(HttpHeader.IF_MATCH_HEADER, null);
    Optional<ETag> eTag = ETagHandler.getETagFromHeader(httpHeaders, false);
    Assertions.assertFalse(eTag.isPresent());
  }

  /**
   * verifies that the header is not evaluated if an empty value is used
   */
  @Test
  public void testGetIfNoneMatchEtagFromRequestHeaderButWithEmptyValue()
  {
    Map<String, String> httpHeaders = new HashMap<>();
    httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, ETag.builder().tag(null).build().toString());
    Optional<ETag> eTag = ETagHandler.getETagFromHeader(httpHeaders, true);
    Assertions.assertFalse(eTag.isPresent());
  }

  /**
   * verifies that the header is not evaluated if an empty value is used
   */
  @Test
  public void testGetIfNoneMatchEtagFromRequestHeaderButWithNull()
  {
    Map<String, String> httpHeaders = new HashMap<>();
    httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, null);
    Optional<ETag> eTag = ETagHandler.getETagFromHeader(httpHeaders, true);
    Assertions.assertFalse(eTag.isPresent());
  }

  /**
   * verifies that the If-None-Match header is correctly extracted from the http request headers
   */
  @ParameterizedTest
  @ValueSource(strings = {HttpHeader.IF_NONE_MATCH_HEADER, "if-none-match", "IF-NONE-MATCH", "iF-nOnE-mAtCh"})
  public void testGetIfNoneMatchEtagFromRequestHeader(String headerName)
  {
    Map<String, String> httpHeaders = new HashMap<>();
    final String tag = UUID.randomUUID().toString();
    httpHeaders.put(headerName, ETag.builder().tag(tag).build().toString());
    Optional<ETag> eTag = ETagHandler.getETagFromHeader(httpHeaders, true);
    Assertions.assertTrue(eTag.isPresent());
    Assertions.assertEquals(tag, eTag.get().getTag());
    Assertions.assertTrue(eTag.get().isWeak());
  }

  /**
   * verifies that the header is not evaluated if an empty value is used
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testGetEmptyIfRequestHeaderAreMissing(boolean ifNot)
  {
    Map<String, String> httpHeaders = new HashMap<>();
    Optional<ETag> eTag = ETagHandler.getETagFromHeader(httpHeaders, ifNot);
    Assertions.assertFalse(eTag.isPresent());
  }

  /**
   * verifies that nothing happens if the service provider does not support etags
   */
  @Test
  public void testValidateVersionWithoutETagSupport()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(false).build())
                                                     .build();
    Assertions.assertDoesNotThrow(() -> ETagHandler.validateVersion(serviceProvider, userResourceType, null, null));
  }

  /**
   * verifies that the validation does nothing in case of an If-Match header if the value of the request is
   * equal with the value of the resource
   */
  @Test
  public void testValidateVersionWithIfMatchHeader()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(true).build())
                                                     .build();
    Map<String, String> httpHeaders = new HashMap<>();
    ETag eTag = ETag.builder().tag(UUID.randomUUID().toString()).build();
    httpHeaders.put(HttpHeader.IF_MATCH_HEADER, eTag.toString());
    Meta meta = Meta.builder().version(eTag).build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").meta(meta).build();
    Assertions.assertDoesNotThrow(() -> ETagHandler.validateVersion(serviceProvider,
                                                                    userResourceType,
                                                                    () -> user,
                                                                    httpHeaders));
  }

  /**
   * verifies that the validation results in a precondition failed exception if the If-Match header does not
   * match
   */
  @Test
  public void testValidateVersionWithIfMatchHeaderAndMatchFails()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(true).build())
                                                     .build();
    Map<String, String> httpHeaders = new HashMap<>();
    ETag eTag = ETag.builder().tag(UUID.randomUUID().toString()).build();
    httpHeaders.put(HttpHeader.IF_MATCH_HEADER, eTag.toString());
    ETag metaETag = ETag.builder().tag(UUID.randomUUID().toString()).build();
    Meta meta = Meta.builder().version(metaETag).build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").meta(meta).build();
    try
    {
      ETagHandler.validateVersion(serviceProvider, userResourceType, () -> user, httpHeaders);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, ex.getStatus());
      Assertions.assertEquals("eTag status of resource has changed. Current value is: " + metaETag.getEntityTag(),
                              ex.getDetail());
      MatcherAssert.assertThat(ex.getClass(), Matchers.typeCompatibleWith(PreconditionFailedException.class));
    }
  }

  /**
   * verifies that the validation does nothing in case of an If-None-Match header if the value of the request is
   * NOT equal with the value of the resource
   */
  @Test
  public void testValidateVersionWithIfNoneMatchHeader()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(true).build())
                                                     .build();
    Map<String, String> httpHeaders = new HashMap<>();
    ETag eTag = ETag.builder().tag(UUID.randomUUID().toString()).build();
    httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, eTag.toString());
    ETag metaETag = ETag.builder().tag(UUID.randomUUID().toString()).build();
    Meta meta = Meta.builder().version(metaETag).build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").meta(meta).build();
    Assertions.assertDoesNotThrow(() -> ETagHandler.validateVersion(serviceProvider,
                                                                    userResourceType,
                                                                    () -> user,
                                                                    httpHeaders));
  }

  /**
   * verifies that the validation results in a not modified exception if the If-None-Match header has a match
   */
  @Test
  public void testValidateVersionWithIfNoneMatchHeaderAndMatchSucceeds()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(true).build())
                                                     .build();
    Map<String, String> httpHeaders = new HashMap<>();
    ETag eTag = ETag.builder().tag(UUID.randomUUID().toString()).build();
    httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, eTag.toString());
    Meta meta = Meta.builder().version(eTag).build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").meta(meta).build();
    try
    {
      ETagHandler.validateVersion(serviceProvider, userResourceType, () -> user, httpHeaders);
      Assertions.fail("this point must not be reached");
    }
    catch (ScimException ex)
    {
      Assertions.assertEquals(HttpStatus.NOT_MODIFIED, ex.getStatus());
      Assertions.assertNull(ex.getDetail());
      MatcherAssert.assertThat(ex.getClass(), Matchers.typeCompatibleWith(NotModifiedException.class));
    }
  }

  /**
   * verifies that nothing happens if the client did not sent any headers
   */
  @Test
  public void testNoHeadersUsed()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(true).build())
                                                     .build();
    Meta meta = Meta.builder().version(ETag.builder().tag(UUID.randomUUID().toString()).build()).build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").meta(meta).build();
    Assertions.assertDoesNotThrow(() -> ETagHandler.validateVersion(serviceProvider,
                                                                    userResourceType,
                                                                    () -> user,
                                                                    new HashMap<>()));
  }

  /**
   * verifies etag validation is not executed if disabled on the resource type
   */
  @Test
  public void testETagValidationDoesNotFailIfDisabledOnResourceType()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(true).build())
                                                     .build();
    userResourceType.getFeatures().getETagFeature().setEnabled(false);

    Map<String, String> httpHeaders = new HashMap<>();
    ETag eTag = ETag.builder().tag(UUID.randomUUID().toString()).build();
    httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, "something-wrong");

    Meta meta = Meta.builder().version(eTag).build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").meta(meta).build();
    Assertions.assertDoesNotThrow(() -> ETagHandler.validateVersion(serviceProvider,
                                                                    userResourceType,
                                                                    () -> user,
                                                                    httpHeaders));
  }

  /**
   * verifies etag validation is not executed if disabled on the resource type
   */
  @Test
  public void testETagGenerationNotExecutedIfDisabledOnResourceType()
  {
    ServiceProvider serviceProvider = ServiceProvider.builder()
                                                     .eTagConfig(ETagConfig.builder().supported(true).build())
                                                     .build();
    userResourceType.getFeatures().getETagFeature().setEnabled(false);
    ETag eTag = ETag.builder().tag(UUID.randomUUID().toString()).build();
    Meta meta = Meta.builder().version(eTag).build();
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").meta(meta).build();
    Assertions.assertFalse(ETagHandler.getResourceVersion(serviceProvider, userResourceType, user).isPresent());
  }
}
