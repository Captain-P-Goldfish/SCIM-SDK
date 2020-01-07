package de.captaingoldfish.scim.sdk.client.keys;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class KeyStoreWrapperTest
{

  /**
   * the master password for the keystores within the test-resources
   */
  private static final String KEYSTORE_MASTER_PASSWORD = "123456";

  /**
   * the alias that can be used for the keystores "test.jceks", "test.p12" and "test_no_password.p12"
   */
  private static final String KEYSTORE_ALIAS = "test";

  /**
   * test that a jks file can be read without problems
   */
  @Test
  public void testKeyStoreWrapperWithJKS()
  {
    File keystoreFile = new File(getClass().getResource("/test-keys/test.jks").getFile());
    KeyStoreWrapper keyStoreWrapper = new KeyStoreWrapper(keystoreFile, KEYSTORE_MASTER_PASSWORD,
                                                          new KeyStoreWrapper.AliasPasswordPair(KEYSTORE_ALIAS,
                                                                                                KEYSTORE_MASTER_PASSWORD));
    checkKeystoreWrapper(keyStoreWrapper);
  }

  /**
   * test that a pkcs12 file can be read without problems
   */
  @Test
  public void testKeyStoreWrapperWithPKCS12()
  {
    File keystoreFile = new File(getClass().getResource("/test-keys/test.p12").getFile());
    KeyStoreWrapper keyStoreWrapper = new KeyStoreWrapper(keystoreFile, KEYSTORE_MASTER_PASSWORD,
                                                          new KeyStoreWrapper.AliasPasswordPair(KEYSTORE_ALIAS,
                                                                                                KEYSTORE_MASTER_PASSWORD));
    checkKeystoreWrapper(keyStoreWrapper);
  }

  /**
   * test that a jceks file can be read without problems
   */
  @Test
  public void testKeyStoreWrapperWithJCEKS()
  {
    File keystoreFile = new File(getClass().getResource("/test-keys/test.jceks").getFile());
    KeyStoreWrapper keyStoreWrapper = new KeyStoreWrapper(keystoreFile, KEYSTORE_MASTER_PASSWORD,
                                                          new KeyStoreWrapper.AliasPasswordPair(KEYSTORE_ALIAS,
                                                                                                KEYSTORE_MASTER_PASSWORD));
    checkKeystoreWrapper(keyStoreWrapper);
  }

  /**
   * test that a jks byte[] can be read without problems
   */
  @Test
  public void testKeyStoreWrapperWithJKSByteArray() throws URISyntaxException, IOException
  {
    byte[] keystoreBytes = IOUtils.toByteArray(getClass().getResource("/test-keys/test.jks").toURI());
    KeyStoreWrapper keyStoreWrapper = new KeyStoreWrapper(keystoreBytes, KeyStoreSupporter.KeyStoreType.JKS,
                                                          KEYSTORE_MASTER_PASSWORD,
                                                          new KeyStoreWrapper.AliasPasswordPair(KEYSTORE_ALIAS,
                                                                                                KEYSTORE_MASTER_PASSWORD));
    checkKeystoreWrapper(keyStoreWrapper);
  }

  /**
   * test that a pkcs12 byte[] can be read without problems
   */
  @Test
  public void testKeyStoreWrapperWithPKCS12ByteArray() throws URISyntaxException
  {
    byte[] keystoreBytes;
    try
    {
      keystoreBytes = IOUtils.toByteArray(getClass().getResource("/test-keys/test.p12").toURI());
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e);
    }
    KeyStoreWrapper keyStoreWrapper = new KeyStoreWrapper(keystoreBytes, KeyStoreSupporter.KeyStoreType.PKCS12,
                                                          KEYSTORE_MASTER_PASSWORD,
                                                          new KeyStoreWrapper.AliasPasswordPair(KEYSTORE_ALIAS,
                                                                                                KEYSTORE_MASTER_PASSWORD));
    checkKeystoreWrapper(keyStoreWrapper);
  }

  /**
   * test that a jceks byte[] can be read without problems
   */
  @Test
  public void testKeyStoreWrapperWithJCEKSByteArray() throws URISyntaxException, IOException
  {
    byte[] keystoreBytes = IOUtils.toByteArray(getClass().getResource("/test-keys/test.jceks").toURI());
    KeyStoreWrapper keyStoreWrapper = new KeyStoreWrapper(keystoreBytes, KeyStoreSupporter.KeyStoreType.JCEKS,
                                                          KEYSTORE_MASTER_PASSWORD,
                                                          new KeyStoreWrapper.AliasPasswordPair(KEYSTORE_ALIAS,
                                                                                                KEYSTORE_MASTER_PASSWORD));
    checkKeystoreWrapper(keyStoreWrapper);
  }

  /**
   * checks that the private key can be accessed with the keystore password if no
   * {@link de.captaingoldfish.scim.sdk.client.keys.KeyStoreWrapper.AliasPasswordPair} pair is entered
   */
  @Test
  public void testReadPrivateKeyByAliasAndPassword() throws URISyntaxException, IOException
  {
    byte[] keystoreBytes = IOUtils.toByteArray(getClass().getResource("/test-keys/test.jceks").toURI());
    KeyStoreWrapper keyStoreWrapper = new KeyStoreWrapper(keystoreBytes, KeyStoreSupporter.KeyStoreType.JCEKS,
                                                          KEYSTORE_MASTER_PASSWORD);
    checkKeystoreWrapper(keyStoreWrapper);
  }

  /**
   * does the JUnit assertions for the given keystoreWrapper
   *
   * @param keyStoreWrapper the keystore wrapper to check
   */
  private void checkKeystoreWrapper(KeyStoreWrapper keyStoreWrapper)
  {
    Assertions.assertTrue(keyStoreWrapper.getPrivateKey(KEYSTORE_ALIAS).isPresent());
    Assertions.assertTrue(keyStoreWrapper.getPrivateKey(KEYSTORE_ALIAS, KEYSTORE_MASTER_PASSWORD).isPresent());
    Assertions.assertFalse(keyStoreWrapper.getPrivateKey("false-alias", KEYSTORE_MASTER_PASSWORD).isPresent());
    Assertions.assertTrue(keyStoreWrapper.getCertificate(KEYSTORE_ALIAS).isPresent());
    Assertions.assertEquals(1, keyStoreWrapper.getAliasesAsList().size());
    Assertions.assertTrue(keyStoreWrapper.getAliasesAsList().contains(KEYSTORE_ALIAS));
  }
}
