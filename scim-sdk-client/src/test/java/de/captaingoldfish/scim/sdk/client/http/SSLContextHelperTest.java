package de.captaingoldfish.scim.sdk.client.http;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.keys.KeyStoreSupporter;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class SSLContextHelperTest
{

  /**
   * to open the keystores from the test-resources
   */
  private static final String KEYSTORE_MASTER_PASSWORD = "123456";

  /**
   * this method asserts that we do not get the SSL default context of the JVM if we give him an appropriate
   * truststore and keystore
   */
  @Test
  public void createSslContextHelperInstance() throws NoSuchAlgorithmException
  {
    String keystoreLocation = "/test-keys/test.jks";
    KeyStoreWrapper keyStore = new KeyStoreWrapper(getClass().getResourceAsStream(keystoreLocation),
                                                   KeyStoreSupporter.KeyStoreType.JKS, KEYSTORE_MASTER_PASSWORD);
    KeyStoreWrapper truststore = new KeyStoreWrapper(getClass().getResourceAsStream(keystoreLocation),
                                                     KEYSTORE_MASTER_PASSWORD);
    SSLContext sslContext = SSLContextHelper.getSslContext(keyStore, truststore);
    Assertions.assertNotNull(sslContext);
    Assertions.assertNotEquals(SSLContext.getDefault(), sslContext);
  }

  /**
   * this method asserts that we do not get the SSL default context of the JVM if we give him an appropriate
   * keystore
   */
  @Test
  public void createSslContextHelperInstanceWithoutTruststore() throws NoSuchAlgorithmException
  {
    String keystoreLocation = "/test-keys/test.jks";
    KeyStoreWrapper keyStore = new KeyStoreWrapper(getClass().getResourceAsStream(keystoreLocation),
                                                   KeyStoreSupporter.KeyStoreType.JKS, KEYSTORE_MASTER_PASSWORD);
    KeyStoreWrapper truststore = null;
    SSLContext sslContext = SSLContextHelper.getSslContext(keyStore, truststore);
    Assertions.assertNotNull(sslContext);
    Assertions.assertNotEquals(SSLContext.getDefault(), sslContext);
  }

  /**
   * this method asserts that we do not get the SSL default context of the JVM if we give him an appropriate
   * truststore
   */
  @Test
  public void createSslContextHelperInstanceWithoutKeystore() throws NoSuchAlgorithmException
  {
    String keystoreLocation = "/test-keys/test.jks";
    KeyStoreWrapper keyStore = null;
    KeyStoreWrapper truststore = new KeyStoreWrapper(getClass().getResourceAsStream(keystoreLocation),
                                                     KEYSTORE_MASTER_PASSWORD);
    SSLContext sslContext = SSLContextHelper.getSslContext(keyStore, truststore);
    Assertions.assertNotNull(sslContext);
    Assertions.assertNotEquals(SSLContext.getDefault(), sslContext);
  }

  /**
   * this method shall show that we get the SSL default JVM context if we put on empty parameters to the method
   */
  @Test
  public void createSslContextHelperInstanceWithJavaDefaultSSLContext() throws NoSuchAlgorithmException
  {
    KeyStoreWrapper keyStore = null;
    KeyStoreWrapper truststore = null;
    SSLContext sslContext = SSLContextHelper.getSslContext(keyStore, truststore);
    Assertions.assertNotNull(sslContext);
    Assertions.assertEquals(SSLContext.getDefault(), sslContext);
  }
}
