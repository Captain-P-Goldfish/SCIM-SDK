package de.captaingoldfish.scim.sdk.client.builder;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.User;


/**
 * author Pascal Knueppel <br>
 * created at: 13.12.2019 - 09:21 <br>
 * <br>
 */
public class ETagRequestBuilderTest
{

  /**
   * verifies simply that the request is setup correctly for simple cases
   */
  @Test
  public void testSetETagsForBothHeaders()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    try
    {
      new MyRequestBuilder<>("http://locahost", EndpointPaths.USERS, User.class,
                             scimHttpClient).setETagForIfMatch("123456").setETagForIfNoneMatch("123456");
      Assertions.fail("this point must not be reached");
    }
    catch (IllegalStateException ex)
    {
      Assertions.assertEquals("cannot use both headers '" + HttpHeader.IF_MATCH_HEADER + "' and '"
                              + HttpHeader.IF_NONE_MATCH_HEADER + "' in a single request",
                              ex.getMessage());
    }
  }

  /**
   * verifies simply that the request is setup correctly for simple cases
   */
  @Test
  public void testSetETagsForBothHeaders2()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    try
    {
      new MyRequestBuilder<>("http://locahost", EndpointPaths.USERS, User.class,
                             scimHttpClient).setETagForIfNoneMatch("123456").setETagForIfMatch("123456");
      Assertions.fail("this point must not be reached");
    }
    catch (IllegalStateException ex)
    {
      Assertions.assertEquals("cannot use both headers '" + HttpHeader.IF_MATCH_HEADER + "' and '"
                              + HttpHeader.IF_NONE_MATCH_HEADER + "' in a single request",
                              ex.getMessage());
    }
  }

  /**
   * verifies simply that the request is setup correctly for simple cases
   */
  @Test
  public void testSetETagsForBothHeaders3()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    try
    {
      new MyRequestBuilder<>("http://locahost", EndpointPaths.USERS, User.class,
                             scimHttpClient).setETagForIfNoneMatch(ETag.builder().weak(false).tag("123456").build())
                                            .setETagForIfMatch(ETag.builder().weak(false).tag("123456").build());
      Assertions.fail("this point must not be reached");
    }
    catch (IllegalStateException ex)
    {
      Assertions.assertEquals("cannot use both headers '" + HttpHeader.IF_MATCH_HEADER + "' and '"
                              + HttpHeader.IF_NONE_MATCH_HEADER + "' in a single request",
                              ex.getMessage());
    }
  }

  /**
   * verifies simply that the request is setup correctly for simple cases
   */
  @Test
  public void testSetETagsForBothHeaders4()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    try
    {
      new MyRequestBuilder<>("http://locahost", EndpointPaths.USERS, User.class,
                             scimHttpClient).setETagForIfMatch(ETag.builder().weak(false).tag("123456").build())
                                            .setETagForIfNoneMatch(ETag.builder().weak(false).tag("123456").build());
      Assertions.fail("this point must not be reached");
    }
    catch (IllegalStateException ex)
    {
      Assertions.assertEquals("cannot use both headers '" + HttpHeader.IF_MATCH_HEADER + "' and '"
                              + HttpHeader.IF_NONE_MATCH_HEADER + "' in a single request",
                              ex.getMessage());
    }
  }

  /**
   * verifies that the parameters are correctly set
   */
  @Test
  public void testGetETagWithUserIfMatch()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    MyRequestBuilder builder = new MyRequestBuilder<>("http://locahost", EndpointPaths.USERS, User.class,
                                                      scimHttpClient).setETagForIfMatch(ETag.builder().weak(false).tag("123456").build());
    Assertions.assertEquals("123456", builder.getVersion().getTag());
    Assertions.assertTrue(builder.isUseIfMatch());
    Assertions.assertFalse(builder.isUseIfNoneMatch());
  }

  /**
   * verifies that the parameters are correctly set
   */
  @Test
  public void testGetETagWithUserIfMatch2()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    MyRequestBuilder builder = new MyRequestBuilder<>("http://locahost", EndpointPaths.USERS, User.class,
                                                      scimHttpClient).setETagForIfMatch("123456");
    Assertions.assertEquals("123456", builder.getVersion().getTag());
    Assertions.assertTrue(builder.isUseIfMatch());
    Assertions.assertFalse(builder.isUseIfNoneMatch());
  }

  /**
   * verifies that the parameters are correctly set
   */
  @Test
  public void testGetETagWithUserIfNoneMatch()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    MyRequestBuilder builder = new MyRequestBuilder<>("http://locahost", EndpointPaths.USERS, User.class,
                                                      scimHttpClient).setETagForIfNoneMatch(ETag.builder().weak(false).tag("123456").build());
    Assertions.assertEquals("123456", builder.getVersion().getTag());
    Assertions.assertTrue(builder.isUseIfNoneMatch());
    Assertions.assertFalse(builder.isUseIfMatch());
  }

  /**
   * verifies that the parameters are correctly set
   */
  @Test
  public void testGetETagWithUserIfNoneMatch2()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    MyRequestBuilder builder = new MyRequestBuilder<>("http://locahost", EndpointPaths.USERS, User.class,
                                                      scimHttpClient).setETagForIfNoneMatch("123456");
    Assertions.assertEquals("123456", builder.getVersion().getTag());
    Assertions.assertTrue(builder.isUseIfNoneMatch());
    Assertions.assertFalse(builder.isUseIfMatch());
  }

  /**
   * pseudo implementation of the abstract {@link ETagRequestBuilder} class for testing
   */
  public static class MyRequestBuilder<T extends ResourceNode> extends ETagRequestBuilder<T>
  {

    public MyRequestBuilder(String baseUrl, String endpoint, Class<T> responseEntityType, ScimHttpClient scimHttpClient)
    {
      super(baseUrl, endpoint, responseEntityType, scimHttpClient);
    }

    @Override
    protected boolean isExpectedResponseCode(int httpStatus)
    {
      return HttpStatus.OK == httpStatus;
    }

    @Override
    public MyRequestBuilder<T> setETagForIfMatch(String version)
    {
      return (MyRequestBuilder<T>)super.setETagForIfMatch(version);
    }

    @Override
    public MyRequestBuilder<T> setETagForIfNoneMatch(String version)
    {
      return (MyRequestBuilder<T>)super.setETagForIfNoneMatch(version);
    }

    @Override
    public MyRequestBuilder<T> setETagForIfMatch(ETag version)
    {
      return (MyRequestBuilder<T>)super.setETagForIfMatch(version);
    }

    @Override
    public MyRequestBuilder<T> setETagForIfNoneMatch(ETag version)
    {
      return (MyRequestBuilder<T>)super.setETagForIfNoneMatch(version);
    }



    @Override
    protected HttpUriRequest getHttpUriRequest()
    {
      return null;
    }
  }
}
