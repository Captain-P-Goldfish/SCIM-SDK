package de.captaingoldfish.scim.sdk.client.http;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import de.captaingoldfish.scim.sdk.client.exceptions.SslContextCreationFailedException;
import de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper;



/**
 * author: Pascal Knueppel<br>
 * created at: 09.12.2019 - 12:26 <br>
 * <br>
 * a builder for creating {@link SSLContext}s with help of {@link KeyStoreWrapper}
 */
public final class SSLContextHelper
{

  /**
   * shut up PMD
   */
  private SSLContextHelper()
  {
    // shut up checkstyle
  }

  /**
   * this method will build the {@link SSLContext} that will be used to access the eid-webservice. This
   * {@link SSLContext} must hold all important informations like the keystore for mutual client authentication
   *
   * @return the {@link SSLContext} that configured the TLS connection
   */
  public static SSLContext getSslContext(KeyStoreWrapper mutualClientAuthenticationKeystoreList,
                                         KeyStoreWrapper truststore)
  {
    if ((mutualClientAuthenticationKeystoreList == null || mutualClientAuthenticationKeystoreList.getKeyStore() == null)
        && (truststore == null || truststore.getKeyStore() == null))
    {
      try
      {
        return SSLContext.getDefault();
      }
      catch (NoSuchAlgorithmException e)
      {
        throw new IllegalStateException("problem with default SSLContext. Has probably been tampered with.", e);
      }
    }
    SSLContext sslContext;
    try
    {
      sslContext = SSLContext.getInstance("TLS");
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new SslContextCreationFailedException(e);
    }
    try
    {
      sslContext.init(getKeyManagers(mutualClientAuthenticationKeystoreList), getTrustmanager(truststore), null);
    }
    catch (KeyManagementException e)
    {
      throw new SslContextCreationFailedException(e);
    }
    return sslContext;
  }

  /**
   * will get the trustmanagers for the services that are used by this serviceaccount instance
   *
   * @param truststore the truststore for trusting external services on TLS connections (will use JVM default
   *          cacerts if null)
   * @return null if {@code truststore} is null or the explicit truststore configuration
   */
  private static TrustManager[] getTrustmanager(KeyStoreWrapper truststore)
  {
    if (truststore == null || truststore.getKeyStore() == null)
    {
      return null;
    }
    TrustManagerFactory trustManagerFactory;
    try
    {
      trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(truststore.getKeyStore());
    }
    catch (NoSuchAlgorithmException | KeyStoreException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
    return trustManagerFactory.getTrustManagers();
  }

  /**
   * will load the key material from this configuration that will be put into the {@link SSLContext} to enable
   * mutual client authentication
   */
  private static KeyManager[] getKeyManagers(KeyStoreWrapper keyStoreAccessor)
  {
    KeyStoreWrapper mutualClientAuthKeystore = keyStoreAccessor;
    if (mutualClientAuthKeystore == null || mutualClientAuthKeystore.getKeyStore() == null)
    {
      return null;
    }
    KeyManagerFactory keyManagerFactory;
    try
    {
      keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new SslContextCreationFailedException(e);
    }
    try
    {
      keyManagerFactory.init(mutualClientAuthKeystore.getKeyStore(),
                             mutualClientAuthKeystore.getKeystorePassword().toCharArray());
    }
    catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e)
    {
      throw new SslContextCreationFailedException("keystore could not be accessed", e);
    }
    return keyManagerFactory.getKeyManagers();
  }


}
