package de.captaingoldfish.scim.sdk.client.http;

import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreSupporter;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;
import de.captaingoldfish.scim.sdk.client.springboot.AbstractSpringBootWebTest;
import de.captaingoldfish.scim.sdk.client.springboot.SecurityConstants;
import de.captaingoldfish.scim.sdk.client.springboot.SpringBootInitializer;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
@ActiveProfiles(SecurityConstants.X509_PROFILE)
@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {SpringBootInitializer.class})
public class ScimHttpClientTest extends AbstractSpringBootWebTest
{

  /**
   * a custom truststore that will be used instead of the default JVM cacert truststore if not null
   */
  private KeyStoreWrapper tlsTruststore;

  /**
   * will initialize the members of this class
   */
  @BeforeEach
  public void initialize()
  {
    KeyStore truststore = KeyStoreSupporter.readTruststore(getClass().getResourceAsStream("/test-keys/test.jks"),
                                                           KeyStoreSupporter.KeyStoreType.JKS);
    tlsTruststore = new KeyStoreWrapper(truststore, null);
  }

  /**
   * assures that each method must define its own request validation if some is wanted
   */
  @AfterEach
  public void destroy()
  {
    TestController.validateRequest = null;
  }

  /**
   * verifies that the basic authentication in the {@link ScimClientConfig} is respected if the
   * {@link ScimHttpClient} is used directly
   */
  @Test
  public void testBasicAuthenticationWithScimHttpClient()
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .basic("goldfish", "123456")
                                                        .truststore(tlsTruststore)
                                                        .hostnameVerifier((s, sslSession) -> true)
                                                        .build();

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    TestController.validateRequest = (httpServletRequest, s) -> {
      String authorization = httpServletRequest.getHeader(HttpHeader.AUTHORIZATION);
      Assertions.assertNotNull(authorization);
      Assertions.assertEquals(scimClientConfig.getBasicAuth().getAuthorizationHeaderValue(), authorization);
      wasCalled.set(true);
    };

    ScimHttpClient httpClient = new ScimHttpClient(scimClientConfig);

    HttpGet httpGet = new HttpGet(getRequestUrl(TestController.GET_ENDPOINT_PATH));
    httpClient.sendRequest(httpGet);
    Assertions.assertTrue(wasCalled.get());
  }

}
