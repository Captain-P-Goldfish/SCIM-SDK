package de.captaingoldfish.scim.sdk.client.builder;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.builder.config.MetaConfigRequestDetails;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.resources.MetaConfiguration;
import de.captaingoldfish.scim.sdk.client.resources.ResourceType;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 14.10.2023
 */
@Slf4j
public class MetaConfigLoaderBuilderTest extends HttpServerMockup
{

  @AfterEach
  public void rollback()
  {
    scimConfig.getServiceProvider().getFilterConfig().setSupported(true);
  }

  /**
   * verifies that the metadata from a SCIM provider can be successfully loaded with the default configuration
   */
  @DisplayName("Load meta-data from SCIM provider with default config")
  @Test
  public void testLoadMetaConfigurationWithDefaultConfig()
  {
    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    ServerResponse<MetaConfiguration> response = new MetaConfigLoaderBuilder(getServerUrl(), scimHttpClient,
                                                                             new MetaConfigRequestDetails()).sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());

    MetaConfiguration metaConfiguration = response.getResource();
    Assertions.assertNotNull(metaConfiguration.getServiceProvider());
    Assertions.assertEquals(6, metaConfiguration.getResourceTypes().size());
    Assertions.assertEquals(8, metaConfiguration.getSchemas().size());
  }

  /**
   * verifies that the metadata from a SCIM provider can be successfully loaded with the default configuration
   */
  @DisplayName("Load meta-data from SCIM provider with excluded meta-schemas")
  @ParameterizedTest(name = "filterSupported: {0}")
  @ValueSource(booleans = {true, false})
  public void testLoadMetaConfigurationExcludeMetaSchemas(boolean filterSupported)
  {
    scimConfig.getServiceProvider().getFilterConfig().setSupported(filterSupported);

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    MetaConfigRequestDetails metaConfigRequestDetails = MetaConfigRequestDetails.builder()
                                                                                .excludeMetaSchemas(true)
                                                                                .build();
    ServerResponse<MetaConfiguration> response = new MetaConfigLoaderBuilder(getServerUrl(), scimHttpClient,
                                                                             metaConfigRequestDetails).sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());

    MetaConfiguration metaConfiguration = response.getResource();
    Assertions.assertNotNull(metaConfiguration.getServiceProvider());
    Assertions.assertEquals(6, metaConfiguration.getResourceTypes().size());
    Assertions.assertEquals(4, metaConfiguration.getSchemas().size());
  }

  /**
   * verifies that the metadata from a SCIM provider can be successfully loaded with the default configuration
   */
  @DisplayName("Load meta-data from SCIM provider with excluded meta-resource-types")
  @ParameterizedTest(name = "filterSupported: {0}")
  @ValueSource(booleans = {true, false})
  public void testLoadMetaConfigurationExcludeMetaResourceTypes(boolean filterSupported)
  {
    scimConfig.getServiceProvider().getFilterConfig().setSupported(filterSupported);

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    MetaConfigRequestDetails metaConfigRequestDetails = MetaConfigRequestDetails.builder()
                                                                                .excludeMetaResourceTypes(true)
                                                                                .build();
    ServerResponse<MetaConfiguration> response = new MetaConfigLoaderBuilder(getServerUrl(), scimHttpClient,
                                                                             metaConfigRequestDetails).sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());

    MetaConfiguration metaConfiguration = response.getResource();
    Assertions.assertNotNull(metaConfiguration.getServiceProvider());
    Assertions.assertEquals(3, metaConfiguration.getResourceTypes().size());
    Assertions.assertEquals(8, metaConfiguration.getSchemas().size());
  }

  /**
   * verifies that an error is returned if the /ServiceProviderConfig request fails
   */
  @DisplayName("ServiceProviderConfig request fails")
  @Test
  public void testServiceProviderConfigRequestFails()
  {
    setGetResponseBody(() -> {
      return new ErrorResponse(new BadRequestException("Illegal request data")).toString();
    });
    setGetResponseStatus(() -> HttpStatus.BAD_REQUEST);
    setManipulateResponse(s -> {
      setGetResponseBody(null);
      setGetResponseStatus(null);
      setManipulateResponse(null);
      return s;
    });

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    MetaConfigRequestDetails metaConfigRequestDetails = MetaConfigRequestDetails.builder().build();
    ServerResponse<MetaConfiguration> response = new MetaConfigLoaderBuilder(getServerUrl(), scimHttpClient,
                                                                             metaConfigRequestDetails).sendRequest();
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());

    MetaConfiguration metaConfiguration = response.getResource();
    Assertions.assertNull(metaConfiguration.getServiceProvider());
    Assertions.assertEquals(6, metaConfiguration.getResourceTypes().size());
    Assertions.assertEquals(8, metaConfiguration.getSchemas().size());
  }

  /**
   * verifies that an error is returned if the /ResourceTypes request fails
   */
  @DisplayName("ResourceType request fails")
  @Test
  public void testResourceTypeRequestFails()
  {
    setManipulateResponse(s -> {
      setGetResponseBody(() -> {
        return new ErrorResponse(new BadRequestException("Illegal request data")).toString();
      });
      setGetResponseStatus(() -> HttpStatus.BAD_REQUEST);
      setManipulateResponse(s1 -> {
        setGetResponseBody(null);
        setGetResponseStatus(null);
        setManipulateResponse(null);
        return s1;
      });
      return s;
    });

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    MetaConfigRequestDetails metaConfigRequestDetails = MetaConfigRequestDetails.builder().build();
    ServerResponse<MetaConfiguration> response = new MetaConfigLoaderBuilder(getServerUrl(), scimHttpClient,
                                                                             metaConfigRequestDetails).sendRequest();
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());

    MetaConfiguration metaConfiguration = response.getResource();
    Assertions.assertNotNull(metaConfiguration.getServiceProvider());
    Assertions.assertEquals(0, metaConfiguration.getResourceTypes().size());
    Assertions.assertEquals(8, metaConfiguration.getSchemas().size());
  }

  /**
   * verifies that an error is returned if the /Schemas request fails
   */
  @DisplayName("Schema request fails")
  @Test
  public void testSchemaRequestFails()
  {
    setManipulateResponse(s -> {
      setManipulateResponse(s1 -> {
        setGetResponseBody(() -> {
          return new ErrorResponse(new BadRequestException("Illegal request data")).toString();
        });
        setGetResponseStatus(() -> HttpStatus.BAD_REQUEST);
        return s1;
      });
      return s;
    });

    ScimClientConfig scimClientConfig = new ScimClientConfig();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);
    MetaConfigRequestDetails metaConfigRequestDetails = MetaConfigRequestDetails.builder().build();
    ServerResponse<MetaConfiguration> response = new MetaConfigLoaderBuilder(getServerUrl(), scimHttpClient,
                                                                             metaConfigRequestDetails).sendRequest();
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());

    MetaConfiguration metaConfiguration = response.getResource();
    Assertions.assertNotNull(metaConfiguration.getServiceProvider());
    Assertions.assertEquals(6, metaConfiguration.getResourceTypes().size());
    Assertions.assertEquals(0, metaConfiguration.getSchemas().size());
  }
}
