package de.captaingoldfish.scim.sdk.client.tests.setup;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;
import de.captaingoldfish.scim.sdk.client.tests.constants.Endpoints;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * <br>
 * <br>
 * created at: 05.05.2020
 *
 * @author Pascal Knueppel
 */
@Slf4j
public abstract class ServerTest
{

  /**
   * if spring boot test uses a random port the port will be injected into this variable
   */
  @Getter
  @LocalServerPort
  private int localServerPort;

  /**
   * contains the URL to which the requests must be sent
   */
  @Getter
  private String applicationUrl;

  /**
   * the scim resource endpoint
   */
  @Getter
  @Autowired
  private ResourceEndpoint resourceEndpoint;

  /**
   * will initialize the url under which the locally started tomcat can be reached
   */
  @BeforeEach
  public void initializeUrl()
  {
    applicationUrl = "https://localhost:" + localServerPort + Endpoints.SCIM_ENDPOINT_PATH;
  }

  /**
   * this method will create a request url with the given path
   *
   * @param path the context path to the method that should be used
   * @return the complete server-url with the given context path to the method that should be used
   */
  public String getRequestUrl(String path)
  {
    return applicationUrl + (path.charAt(0) == '/' ? path : "/" + path);
  }

  /**
   * @return the truststore that is needed to trust the application
   */
  public KeyStoreWrapper getTruststore()
  {
    return getKeystore("de/captaingoldfish/scim/sdk/client/tests/keys/truststore.jks");
  }

  /**
   * @return a keystore that is authorized with x509 authentication if configured
   */
  public KeyStoreWrapper getAuthorizedKey()
  {
    return getKeystore("de/captaingoldfish/scim/sdk/client/tests/keys/admin-auth.jks");
  }

  /**
   * @return a keystore that is authorized with x509 authentication if configured
   */
  public KeyStoreWrapper getUnauthorizedKey()
  {
    return getKeystore("de/captaingoldfish/scim/sdk/client/tests/keys/unauthorized-auth.jks");
  }

  /**
   * @param path
   * @return
   */
  public KeyStoreWrapper getKeystore(String path)
  {
    final String basePath = getClass().getResource("/")
                                      .toString()
                                      .replace("test-classes", "classes")
                                      .replace("file:", "");
    final String truststorePath = basePath + path;
    try (InputStream inputStream = new FileInputStream(truststorePath))
    {
      Assertions.assertNotNull(inputStream);
      return new KeyStoreWrapper(inputStream, "123456");
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }
}
