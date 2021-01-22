package de.captaingoldfish.scim.sdk.client.keys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.client.exceptions.KeyStoreCreationFailedException;
import de.captaingoldfish.scim.sdk.client.exceptions.KeyStoreEntryException;
import lombok.SneakyThrows;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
public class KeyStoreSupporterTest
{

  /**
   * This dn is used for certificate creation in the following tests
   */
  private final DistinguishedName distinguishedName = new DistinguishedName("test", null, null, null, null, null);

  /**
   * generates an asymmetric RSA key
   *
   * @param keyGenerationParameters The parameters to generate the key
   * @return an RSA keypair
   */
  public static KeyPair generateKey(KeyGenerationParameters keyGenerationParameters)
  {
    KeyPairGenerator keyPairGenerator = null;
    try
    {
      keyPairGenerator = KeyPairGenerator.getInstance("RSA", SecurityProvider.BOUNCY_CASTLE_PROVIDER);
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new IllegalStateException("RSA-Schluessel konnte nicht erzeugt werden", e);
    }
    keyPairGenerator.initialize(keyGenerationParameters.getStrength(), keyGenerationParameters.getRandom());
    return keyPairGenerator.generateKeyPair();
  }

  /**
   * this test shall show that a keystore of all 3 supported types can be created from a keypair.
   */
  @Test
  public void testCreateKeyStore() throws Exception
  {
    KeyPair keyPair = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate = CertificateCreator.createX509SelfSignedCertificate(keyPair,
                                                                                     distinguishedName,
                                                                                     new Date(),
                                                                                     new Date(System.currentTimeMillis()
                                                                                              + 1000L * 60 * 60 * 24));
    String alias = "alias";
    String password = "password";
    KeyStore keyStore = KeyStoreSupporter.toKeyStore(keyPair.getPrivate(),
                                                     certificate,
                                                     alias,
                                                     password,
                                                     KeyStoreSupporter.KeyStoreType.JKS);
    Assertions.assertNotNull(keyStore.getCertificate(alias));
    Assertions.assertNotNull(keyStore.getKey(alias, password.toCharArray()));

    keyStore = KeyStoreSupporter.toKeyStore(keyPair.getPrivate(),
                                            certificate,
                                            alias,
                                            password,
                                            KeyStoreSupporter.KeyStoreType.JCEKS);
    Assertions.assertNotNull(keyStore.getCertificate(alias));
    Assertions.assertNotNull(keyStore.getKey(alias, password.toCharArray()));
    keyStore = KeyStoreSupporter.toKeyStore(keyPair.getPrivate(),
                                            certificate,
                                            alias,
                                            password,
                                            KeyStoreSupporter.KeyStoreType.PKCS12);
    Assertions.assertNotNull(keyStore.getCertificate(alias));
    Assertions.assertNotNull(keyStore.getKey(alias, password.toCharArray()));

    byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
    byte[] certificateBytes = certificate.getEncoded();
    keyStore = KeyStoreSupporter.toKeyStore(privateKeyBytes,
                                            certificateBytes,
                                            alias,
                                            password,
                                            KeyStoreSupporter.KeyStoreType.JKS);
    Assertions.assertNotNull(keyStore.getCertificate(alias));
    Assertions.assertNotNull(keyStore.getKey(alias, password.toCharArray()));

    keyStore = KeyStoreSupporter.toKeyStore(privateKeyBytes,
                                            certificateBytes,
                                            alias,
                                            password,
                                            KeyStoreSupporter.KeyStoreType.JCEKS);
    Assertions.assertNotNull(keyStore.getCertificate(alias));
    Assertions.assertNotNull(keyStore.getKey(alias, password.toCharArray()));
    keyStore = KeyStoreSupporter.toKeyStore(privateKeyBytes,
                                            certificateBytes,
                                            alias,
                                            password,
                                            KeyStoreSupporter.KeyStoreType.PKCS12);
    Assertions.assertNotNull(keyStore.getCertificate(alias));
    Assertions.assertNotNull(keyStore.getKey(alias, password.toCharArray()));
  }

  /**
   * shall show that a appropriate exception is thrown if the certificate is null
   */
  @Test
  public void testCreateKeyStoreWithNullValues() throws Exception
  {
    KeyPair keyPair = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate = null;

    String alias = "alias";
    String password = "password";

    Assertions.assertThrows(KeyStoreEntryException.class,
                            () -> KeyStoreSupporter.toKeyStore(keyPair.getPrivate(),
                                                               certificate,
                                                               alias,
                                                               password,
                                                               KeyStoreSupporter.KeyStoreType.JKS));
  }

  /**
   * shall show that a appropriate exception is thrown if the private key is null
   */
  @Test
  public void testCreateKeyStoreWithNullValues2() throws Exception
  {
    PrivateKey privateKey = null;
    X509Certificate certificate = null;

    String alias = "alias";
    String password = "password";

    Assertions.assertThrows(KeyStoreEntryException.class,
                            () -> KeyStoreSupporter.toKeyStore(privateKey,
                                                               certificate,
                                                               alias,
                                                               password,
                                                               KeyStoreSupporter.KeyStoreType.JKS));
  }

  /**
   * this method shall show that all relevant keystore-types can be successfully created.
   */
  @Test
  public void createEmptyKeyStoreTest()
  {
    KeyStore ks = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JKS, "");
    Assertions.assertNotNull(ks);
    ks = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JCEKS, "");
    Assertions.assertNotNull(ks);
    ks = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.PKCS12, "");
    Assertions.assertNotNull(ks);
  }

  /**
   * This method will add a certificate-entry to a given keystore
   */
  @Test
  public void addCertificateEntryToKeyStore() throws KeyStoreException
  {
    KeyPair keyPair = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate = CertificateCreator.createX509SelfSignedCertificate(keyPair,
                                                                                     distinguishedName,
                                                                                     new Date(),
                                                                                     new Date(System.currentTimeMillis()
                                                                                              + 1000L * 60 * 60 * 24));
    String alias = "alias";

    KeyStore jksKeyStore = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JKS, "");
    Assertions.assertTrue(jksKeyStore.size() == 0);
    KeyStoreSupporter.addCertificateEntryToKeyStore(jksKeyStore, certificate, alias);
    Assertions.assertTrue(jksKeyStore.size() == 1);
    Assertions.assertTrue(jksKeyStore.containsAlias(alias));

    KeyStore jceksKeyStore = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JCEKS, "");
    Assertions.assertTrue(jceksKeyStore.size() == 0);
    KeyStoreSupporter.addCertificateEntryToKeyStore(jceksKeyStore, certificate, alias);
    Assertions.assertTrue(jceksKeyStore.size() == 1);
    Assertions.assertTrue(jceksKeyStore.containsAlias(alias));

    KeyStore pkcs12KeyStore = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.PKCS12, "");
    Assertions.assertTrue(pkcs12KeyStore.size() == 0);
    KeyStoreSupporter.addCertificateEntryToKeyStore(pkcs12KeyStore, certificate, alias);
    Assertions.assertTrue(pkcs12KeyStore.size() == 1);
    Assertions.assertTrue(pkcs12KeyStore.containsAlias(alias));
  }

  /**
   * this test shows that the addCertificate-method will prevent overriding a certificate-entry by accident by
   * throwing an exception if tried.
   */
  @Test
  public void addCertificateEntryToJksKeyStoreTwice() throws CertificateEncodingException
  {
    KeyPair keyPair1 = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate1 = CertificateCreator.createX509SelfSignedCertificate(keyPair1,
                                                                                      distinguishedName,
                                                                                      new Date(),
                                                                                      new Date(System.currentTimeMillis()
                                                                                               + 1000L * 60 * 60 * 24));

    KeyPair keyPair2 = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate2 = CertificateCreator.createX509SelfSignedCertificate(keyPair2,
                                                                                      distinguishedName,
                                                                                      new Date(),
                                                                                      new Date(System.currentTimeMillis()
                                                                                               + 1000L * 60 * 60 * 24));

    String alias = "alias";

    KeyStore jksKeyStore = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JKS, "");
    KeyStore keyStore = KeyStoreSupporter.addCertificateEntryToKeyStore(jksKeyStore, certificate1, alias);
    Assertions.assertArrayEquals(certificate1.getEncoded(),
                                 KeyStoreSupporter.getCertificate(keyStore, alias).get().getEncoded());
    Assertions.assertThrows(IllegalArgumentException.class,
                            () -> KeyStoreSupporter.addCertificateEntryToKeyStore(jksKeyStore, certificate2, alias));
  }

  @SneakyThrows
  @Test
  public void testAddCertificateEntry()
  {
    KeyPair keyPair1 = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate1 = CertificateCreator.createX509SelfSignedCertificate(keyPair1,
                                                                                      distinguishedName,
                                                                                      new Date(),
                                                                                      new Date(System.currentTimeMillis()
                                                                                               + 1000L * 60 * 60 * 24));

    String alias = "alias";

    KeyStore keyStore = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JKS, "123456");
    KeyStoreSupporter.addCertificateEntry(keyStore, alias, certificate1);
    Assertions.assertArrayEquals(certificate1.getEncoded(), keyStore.getCertificate(alias).getEncoded());
    Assertions.assertThrows(KeyStoreException.class,
                            () -> KeyStoreSupporter.addCertificateEntry(KeyStore.getInstance("JKS"), alias, null));
  }

  /**
   * This method shall show that the conversion of keystores works flawless between the supported types.
   */
  @Test
  public void testConvertKeyStore()
  {
    KeyPair keyPair = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate = CertificateCreator.createX509SelfSignedCertificate(keyPair,
                                                                                     distinguishedName,
                                                                                     new Date(),
                                                                                     new Date(System.currentTimeMillis()
                                                                                              + 1000L * 60 * 60 * 24));
    String alias = "alias";
    String password = "password";
    KeyStore jksKeyStore = KeyStoreSupporter.toKeyStore(keyPair.getPrivate(),
                                                        certificate,
                                                        alias,
                                                        password,
                                                        KeyStoreSupporter.KeyStoreType.JKS);
    KeyStore jceksKeyStore;
    KeyStore pkcs12KeyStore;

    convertFromJKS:
    {
      jceksKeyStore = KeyStoreSupporter.convertKeyStore(jksKeyStore, password, KeyStoreSupporter.KeyStoreType.JCEKS);
      Assertions.assertEquals(jksKeyStore.getType(), KeyStoreSupporter.KeyStoreType.JKS.name());
      Assertions.assertEquals(jceksKeyStore.getType(), KeyStoreSupporter.KeyStoreType.JCEKS.name());

      pkcs12KeyStore = KeyStoreSupporter.convertKeyStore(jksKeyStore, password, KeyStoreSupporter.KeyStoreType.PKCS12);
      Assertions.assertEquals(jksKeyStore.getType(), KeyStoreSupporter.KeyStoreType.JKS.name());
      Assertions.assertEquals(pkcs12KeyStore.getType(), KeyStoreSupporter.KeyStoreType.PKCS12.name());
    }

    convertFromJCEKS:
    {
      jksKeyStore = KeyStoreSupporter.convertKeyStore(jceksKeyStore, password, KeyStoreSupporter.KeyStoreType.JKS);
      Assertions.assertEquals(jceksKeyStore.getType(), KeyStoreSupporter.KeyStoreType.JCEKS.name());
      Assertions.assertEquals(jksKeyStore.getType(), KeyStoreSupporter.KeyStoreType.JKS.name());

      pkcs12KeyStore = KeyStoreSupporter.convertKeyStore(jceksKeyStore,
                                                         password,
                                                         KeyStoreSupporter.KeyStoreType.PKCS12);
      Assertions.assertEquals(jceksKeyStore.getType(), KeyStoreSupporter.KeyStoreType.JCEKS.name());
      Assertions.assertEquals(pkcs12KeyStore.getType(), KeyStoreSupporter.KeyStoreType.PKCS12.name());
    }

    convertFromPKCS12:
    {
      jksKeyStore = KeyStoreSupporter.convertKeyStore(pkcs12KeyStore, password, KeyStoreSupporter.KeyStoreType.JKS);
      Assertions.assertEquals(pkcs12KeyStore.getType(), KeyStoreSupporter.KeyStoreType.PKCS12.name());
      Assertions.assertEquals(jksKeyStore.getType(), KeyStoreSupporter.KeyStoreType.JKS.name());

      jceksKeyStore = KeyStoreSupporter.convertKeyStore(pkcs12KeyStore, password, KeyStoreSupporter.KeyStoreType.JCEKS);
      Assertions.assertEquals(pkcs12KeyStore.getType(), KeyStoreSupporter.KeyStoreType.PKCS12.name());
      Assertions.assertEquals(jceksKeyStore.getType(), KeyStoreSupporter.KeyStoreType.JCEKS.name());
    }
  }

  /**
   * This test will show that keystores can be read from files and byte-arrays
   */
  @TestFactory
  public List<DynamicTest> testReadKeyStore() throws KeyStoreException, IOException
  {
    KeyPair keyPair = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate x509Certificate = CertificateCreator.createX509SelfSignedCertificate(keyPair,
                                                                                         distinguishedName,
                                                                                         new Date(),
                                                                                         new Date(System.currentTimeMillis()
                                                                                                  + 1000L * 60 * 60
                                                                                                    * 24));
    String alias = "alias";
    String password = "password";
    KeyStore jksKeyStore = KeyStoreSupporter.toKeyStore(keyPair.getPrivate(),
                                                        x509Certificate,
                                                        alias,
                                                        password,
                                                        KeyStoreSupporter.KeyStoreType.JKS);

    File targetDirectory = new File(".");
    String filename = "testKeyStoreFile";

    List<DynamicTest> dynamicTestList = new ArrayList<>();

    dynamicTestList.add(DynamicTest.dynamicTest("create JKS keystore file", () -> {
      File targetFile = new File(targetDirectory.getAbsolutePath() + "/" + filename + ".jks");
      KeyStoreSupporter.keyStoreToFile(targetDirectory, filename, jksKeyStore, password);
      Assertions.assertTrue(targetFile.exists(), getErrorTextFileNotCreated(targetFile));

      KeyStore keyStore = KeyStoreSupporter.readKeyStore(targetFile, password);
      Certificate certificate = jksKeyStore.getCertificate(alias);
      String certificateAlias = keyStore.getCertificateAlias(certificate);
      Assertions.assertNotNull(certificateAlias);
      Assertions.assertEquals(alias, certificateAlias);

      byte[] keystoreBytes = FileUtils.readFileToByteArray(targetFile);
      keyStore = KeyStoreSupporter.readKeyStore(keystoreBytes, KeyStoreSupporter.KeyStoreType.JKS, password);
      keyStore = KeyStoreSupporter.readKeyStore(keystoreBytes, KeyStoreSupporter.KeyStoreType.JCEKS, password);
      certificateAlias = keyStore.getCertificateAlias(certificate);
      Assertions.assertNotNull(certificateAlias);
      Assertions.assertEquals(alias, certificateAlias);

      InputStream keystoreInputStream = FileUtils.openInputStream(targetFile);
      keyStore = KeyStoreSupporter.readKeyStore(keystoreInputStream, KeyStoreSupporter.KeyStoreType.JKS, password);
      certificateAlias = keyStore.getCertificateAlias(certificate);
      Assertions.assertNotNull(certificateAlias);
      Assertions.assertEquals(alias, certificateAlias);
      targetFile.delete();
    }));


    dynamicTestList.add(DynamicTest.dynamicTest("create JCEKS keystore file", () -> {
      KeyStore jceksKeyStore = KeyStoreSupporter.convertKeyStore(jksKeyStore,
                                                                 password,
                                                                 KeyStoreSupporter.KeyStoreType.JCEKS);
      File targetFile = new File(targetDirectory.getAbsolutePath() + "/" + filename + ".jceks");
      KeyStoreSupporter.keyStoreToFile(targetDirectory, filename, jceksKeyStore, password);
      Assertions.assertTrue(targetFile.exists(), getErrorTextFileNotCreated(targetFile));

      KeyStore keyStore = KeyStoreSupporter.readKeyStore(targetFile, password);
      Certificate certificate = jksKeyStore.getCertificate(alias);
      String certificateAlias = keyStore.getCertificateAlias(certificate);
      Assertions.assertNotNull(certificateAlias);
      Assertions.assertEquals(alias, certificateAlias);

      byte[] keystoreBytes = FileUtils.readFileToByteArray(targetFile);
      keyStore = KeyStoreSupporter.readKeyStore(keystoreBytes, KeyStoreSupporter.KeyStoreType.JCEKS, password);
      certificateAlias = keyStore.getCertificateAlias(certificate);
      Assertions.assertNotNull(certificateAlias);
      Assertions.assertEquals(alias, certificateAlias);

      InputStream keystoreInputStream = FileUtils.openInputStream(targetFile);
      keyStore = KeyStoreSupporter.readKeyStore(keystoreInputStream, KeyStoreSupporter.KeyStoreType.JCEKS, password);
      certificateAlias = keyStore.getCertificateAlias(certificate);
      Assertions.assertNotNull(certificateAlias);
      Assertions.assertEquals(alias, certificateAlias);
      targetFile.delete();
    }));

    dynamicTestList.add(DynamicTest.dynamicTest("create PKCS12 keystore file", () -> {
      KeyStore jceksKeyStore = KeyStoreSupporter.convertKeyStore(jksKeyStore,
                                                                 password,
                                                                 KeyStoreSupporter.KeyStoreType.PKCS12);
      File targetFile = new File(targetDirectory.getAbsolutePath() + "/" + filename + ".p12");
      KeyStoreSupporter.keyStoreToFile(targetDirectory, filename, jceksKeyStore, password);
      Assertions.assertTrue(targetFile.exists(), getErrorTextFileNotCreated(targetFile));

      KeyStore keyStore = KeyStoreSupporter.readKeyStore(targetFile, password);
      Certificate certificate = jksKeyStore.getCertificate(alias);
      String certificateAlias = keyStore.getCertificateAlias(certificate);
      Assertions.assertNotNull(certificateAlias);
      Assertions.assertEquals(alias, certificateAlias);

      byte[] keystoreBytes = FileUtils.readFileToByteArray(targetFile);
      keyStore = KeyStoreSupporter.readKeyStore(keystoreBytes, KeyStoreSupporter.KeyStoreType.PKCS12, password);
      certificateAlias = keyStore.getCertificateAlias(certificate);
      Assertions.assertNotNull(certificateAlias);
      Assertions.assertEquals(alias, certificateAlias);

      InputStream keystoreInputStream = FileUtils.openInputStream(targetFile);
      keyStore = KeyStoreSupporter.readKeyStore(keystoreInputStream, KeyStoreSupporter.KeyStoreType.PKCS12, password);
      certificateAlias = keyStore.getCertificateAlias(certificate);
      Assertions.assertNotNull(certificateAlias);
      Assertions.assertEquals(alias, certificateAlias);
      targetFile.delete();
    }));

    return dynamicTestList;
  }

  @Test
  public void testReadNoneExistingKeystoreFile()
  {
    Assertions.assertThrows(KeyStoreCreationFailedException.class,
                            () -> KeyStoreSupporter.readKeyStore(new File("file-does-not-exist.txt"), "123456"));
  }

  @Test
  public void testReadKeystoreFromNullByteArray()
  {
    Assertions.assertThrows(KeyStoreCreationFailedException.class,
                            () -> KeyStoreSupporter.readKeyStore((byte[])null,
                                                                 KeyStoreSupporter.KeyStoreType.JKS,
                                                                 "123456"));
  }

  @Test
  public void testReadKeystoreFromNullInpustream()
  {
    Assertions.assertThrows(KeyStoreCreationFailedException.class,
                            () -> KeyStoreSupporter.readKeyStore((InputStream)null,
                                                                 KeyStoreSupporter.KeyStoreType.JKS,
                                                                 "123456"));
  }

  /**
   * This test shows that merging of two keystore's will be successful.
   */
  @Test
  public void testMergeKeyStores() throws Exception
  {
    KeyPair keyPair1 = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate1 = CertificateCreator.createX509SelfSignedCertificate(keyPair1,
                                                                                      distinguishedName,
                                                                                      new Date(),
                                                                                      new Date(System.currentTimeMillis()
                                                                                               + 1000L * 60 * 60 * 24));
    String alias1 = "alias1";
    String password1 = "password1";
    KeyStore keyStore1 = KeyStoreSupporter.toKeyStore(keyPair1.getPrivate(),
                                                      certificate1,
                                                      alias1,
                                                      password1,
                                                      KeyStoreSupporter.KeyStoreType.JKS);

    KeyPair keyPair2 = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate2 = CertificateCreator.createX509SelfSignedCertificate(keyPair2,
                                                                                      distinguishedName,
                                                                                      new Date(),
                                                                                      new Date(System.currentTimeMillis()
                                                                                               + 1000L * 60 * 60 * 24));
    String alias2 = "alias2";
    String password2 = "password2";
    KeyStore keyStore2 = KeyStoreSupporter.toKeyStore(keyPair2.getPrivate(),
                                                      certificate2,
                                                      alias2,
                                                      password2,
                                                      KeyStoreSupporter.KeyStoreType.JKS);
    X509Certificate keystore2CertEntry = getTestCertificate();
    String alias3 = "keystore-2-cert-entry";
    KeyStoreSupporter.addEntryToKeystore(keyStore2, alias3, null, new X509Certificate[]{keystore2CertEntry}, password2);

    String mergedKeyStoreKeyPasswords = "newPassword";
    KeyStore mergedKeyStore = KeyStoreSupporter.mergeKeyStores(keyStore1,
                                                               password1,
                                                               keyStore2,
                                                               password2,
                                                               KeyStoreSupporter.KeyStoreType.PKCS12,
                                                               mergedKeyStoreKeyPasswords);
    Enumeration<String> aliasesEnumeration = mergedKeyStore.aliases();
    List<String> aliases = new ArrayList<>();
    while (aliasesEnumeration.hasMoreElements())
    {
      aliases.add(aliasesEnumeration.nextElement());
    }

    Assertions.assertEquals(3, aliases.size());
    Assertions.assertTrue(aliases.contains(alias1), getErrorWrongAlias(alias1));
    Assertions.assertTrue(aliases.contains(alias2), getErrorWrongAlias(alias2));

    Key key1 = mergedKeyStore.getKey(alias1, mergedKeyStoreKeyPasswords.toCharArray());
    Assertions.assertArrayEquals(keyPair1.getPrivate().getEncoded(), key1.getEncoded());

    Key key2 = mergedKeyStore.getKey(alias2, mergedKeyStoreKeyPasswords.toCharArray());
    Assertions.assertArrayEquals(keyPair2.getPrivate().getEncoded(), key2.getEncoded());

    Certificate cert1 = mergedKeyStore.getCertificate(alias1);
    Assertions.assertArrayEquals(certificate1.getEncoded(), cert1.getEncoded());

    Certificate cert2 = mergedKeyStore.getCertificate(alias2);
    Assertions.assertArrayEquals(certificate2.getEncoded(), cert2.getEncoded());

    Certificate cert3 = mergedKeyStore.getCertificate(alias3);
    Assertions.assertArrayEquals(keystore2CertEntry.getEncoded(), cert3.getEncoded());
  }

  /**
   * This test shows that merging of two keystore's will be successful even if keystore1 and keystore2 share the
   * same alias.
   */
  @Test
  public void testMergeKeyStores2() throws Exception
  {
    KeyPair keyPair1 = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate1 = CertificateCreator.createX509SelfSignedCertificate(keyPair1,
                                                                                      distinguishedName,
                                                                                      new Date(),
                                                                                      new Date(System.currentTimeMillis()
                                                                                               + 1000L * 60 * 60 * 24));
    String alias1 = "alias";
    String password1 = "password1";
    KeyStore keyStore1 = KeyStoreSupporter.toKeyStore(keyPair1.getPrivate(),
                                                      certificate1,
                                                      alias1,
                                                      password1,
                                                      KeyStoreSupporter.KeyStoreType.JKS);

    KeyPair keyPair2 = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate2 = CertificateCreator.createX509SelfSignedCertificate(keyPair2,
                                                                                      distinguishedName,
                                                                                      new Date(),
                                                                                      new Date(System.currentTimeMillis()
                                                                                               + 1000L * 60 * 60 * 24));
    String alias2 = "alias2";
    String password2 = "password2";
    KeyStore keyStore2 = KeyStoreSupporter.toKeyStore(keyPair2.getPrivate(),
                                                      certificate2,
                                                      alias2,
                                                      password2,
                                                      KeyStoreSupporter.KeyStoreType.JKS);

    String mergedKeyStoreKeyPasswords = "newPassword";
    KeyStore mergedKeyStore = KeyStoreSupporter.mergeKeyStores(keyStore1,
                                                               password1,
                                                               keyStore2,
                                                               password2,
                                                               KeyStoreSupporter.KeyStoreType.PKCS12,
                                                               mergedKeyStoreKeyPasswords);
    Enumeration<String> aliasesEnumeration = mergedKeyStore.aliases();
    List<String> aliases = new ArrayList<>();
    while (aliasesEnumeration.hasMoreElements())
    {
      aliases.add(aliasesEnumeration.nextElement());
    }

    Assertions.assertEquals(2, aliases.size());
    Assertions.assertTrue(aliases.contains(alias1), getErrorWrongAlias(alias1));
    Assertions.assertTrue(aliases.contains(alias2), getErrorWrongAlias(alias2));

    Key key1 = mergedKeyStore.getKey(alias1, mergedKeyStoreKeyPasswords.toCharArray());
    Assertions.assertArrayEquals(keyPair1.getPrivate().getEncoded(), key1.getEncoded());

    Key key2 = mergedKeyStore.getKey(alias2, mergedKeyStoreKeyPasswords.toCharArray());
    Assertions.assertArrayEquals(keyPair2.getPrivate().getEncoded(), key2.getEncoded());

    Certificate cert1 = mergedKeyStore.getCertificate(alias1);
    Assertions.assertArrayEquals(certificate1.getEncoded(), cert1.getEncoded());

    Certificate cert2 = mergedKeyStore.getCertificate(alias2);
    Assertions.assertNotEquals(cert1, cert2, "certificates must be different entries");
    Assertions.assertArrayEquals(certificate2.getEncoded(), cert2.getEncoded());
  }

  /**
   * this test will prove that the method
   * {@link KeyStoreSupporter#readFirstKeyPairEntryFromKeyStore(KeyStore, String)} works correctly
   */
  @Test
  public void testReadKeyFirstStoreEntry()
  {
    KeyPair keyPair = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate1 = CertificateCreator.createX509SelfSignedCertificate(keyPair,
                                                                                      distinguishedName,
                                                                                      new Date(),
                                                                                      new Date(System.currentTimeMillis()
                                                                                               + 1000L * 60 * 60 * 24));
    String alias = "alias";
    String password = "password1";
    KeyStore keyStore = KeyStoreSupporter.toKeyStore(keyPair.getPrivate(),
                                                     certificate1,
                                                     alias,
                                                     password,
                                                     KeyStoreSupporter.KeyStoreType.JKS);
    KeyPair readKeyPair = KeyStoreSupporter.readFirstKeyPairEntryFromKeyStore(keyStore, password);
    Assertions.assertNotNull(readKeyPair);
    Assertions.assertEquals(keyPair.getPrivate(), readKeyPair.getPrivate());
    Assertions.assertEquals(keyPair.getPublic(), readKeyPair.getPublic());
    Assertions.assertThrows(KeyStoreException.class,
                            () -> KeyStoreSupporter.readFirstKeyPairEntryFromKeyStore(KeyStore.getInstance("JKS"),
                                                                                      password));
  }

  /**
   * this test does show us, that a jks keystore can be opened to access the certificate entries without a
   * password.
   */
  @Test
  public void testReadTruststore()
  {
    KeyStore keyStore = KeyStoreSupporter.readTruststore(getClass().getResourceAsStream("/test-keys/test_truststore.jks"),
                                                         KeyStoreSupporter.KeyStoreType.JKS);
    Assertions.assertNotNull(keyStore);
  }

  /**
   * this test does show us, that a jks keystore can be opened to access the certificate entries without a
   * password.
   */
  @SneakyThrows
  @Test
  public void testReadTruststoreFromBytes()
  {
    byte[] truststoreBytes;
    try (InputStream inputStream = getClass().getResourceAsStream("/test-keys/test_truststore.jks"))
    {
      truststoreBytes = IOUtils.toByteArray(inputStream);
    }
    KeyStore keyStore = KeyStoreSupporter.readTruststore(truststoreBytes, KeyStoreSupporter.KeyStoreType.JKS);
    Assertions.assertNotNull(keyStore);
  }

  @SneakyThrows
  @Test
  public void testReadTruststoreFromBytesWithPassword()
  {
    byte[] truststoreBytes;
    try (InputStream inputStream = getClass().getResourceAsStream("/test-keys/test.p12"))
    {
      truststoreBytes = IOUtils.toByteArray(inputStream);
    }
    KeyStore keyStore = KeyStoreSupporter.readTruststore(truststoreBytes,
                                                         KeyStoreSupporter.KeyStoreType.PKCS12,
                                                         "123456");
    Assertions.assertNotNull(keyStore);
  }

  @SneakyThrows
  @Test
  public void testReadTruststoreWithNullBytesAndPassword()
  {
    Assertions.assertThrows(KeyStoreCreationFailedException.class,
                            () -> KeyStoreSupporter.readTruststore((byte[])null,
                                                                   KeyStoreSupporter.KeyStoreType.PKCS12,
                                                                   "123456"));
  }

  @Test
  public void testReadTruststoreWithNullBytes()
  {
    Assertions.assertThrows(KeyStoreCreationFailedException.class,
                            () -> KeyStoreSupporter.readTruststore((byte[])null, KeyStoreSupporter.KeyStoreType.JKS));
  }

  @Test
  public void testReadTruststoreWithNullInputstream()
  {
    Assertions.assertThrows(KeyStoreCreationFailedException.class,
                            () -> KeyStoreSupporter.readTruststore((InputStream)null,
                                                                   KeyStoreSupporter.KeyStoreType.JKS));
  }

  /**
   * this test does show us, that a pkcs12 keystore will successfully be opened if the password is entered
   */
  @Test
  public void testReadPkcs12TruststoreWithUsingPassword()
  {
    KeyStore keyStore = KeyStoreSupporter.readTruststore(getClass().getResourceAsStream("/test-keys/test.p12"),
                                                         KeyStoreSupporter.KeyStoreType.PKCS12,
                                                         "123456");
    Assertions.assertNotNull(keyStore);
  }

  /**
   * this test does show us, that a pkcs12 keystore cannot be opened up to access only the certificates without
   * giving the password.
   */
  @Test
  public void testReadPkcs12TruststoreWithoutUsingRequiredPassword()
  {
    InputStream keystoreStream = getClass().getResourceAsStream("/test-keys/test.p12");
    Assertions.assertThrows(IOException.class,
                            () -> KeyStoreSupporter.readTruststore(keystoreStream,
                                                                   KeyStoreSupporter.KeyStoreType.PKCS12));
  }

  /**
   * this test does show us, that a pkcs12 keystore cannot be opened up to access only the certificates even if
   * the password to open up the keystore is empty ""
   */
  @Test
  public void testReadPkcs12TruststoreWithEmptyPasswordButNotUsingPassword()
  {
    InputStream keystoreStream = getClass().getResourceAsStream("/test-keys/test_no_password.p12");
    KeyStore ks = KeyStoreSupporter.readTruststore(keystoreStream, KeyStoreSupporter.KeyStoreType.PKCS12);
    Assertions.assertNotNull(ks);
  }

  /**
   * this test shows us, that a pkcs12 keystore even with an empty password can only be read if the empty
   * password is given as parameter
   */
  @Test
  public void testReadPkcs12TruststoreWithEmptyPasswordAndGivingRequiredPassword()
  {
    KeyStore ks = KeyStoreSupporter.readTruststore(getClass().getResourceAsStream("/test-keys/test_no_password.p12"),
                                                   KeyStoreSupporter.KeyStoreType.PKCS12,
                                                   "");
    Assertions.assertNotNull(ks);
  }

  /**
   * this test does show us, that a jceks keystore can be opened to access the certificate entries without a
   * password.
   */
  @Test
  public void testReadJceksTruststoreWithoutGivingRequiredPassword()
  {
    KeyStore ks = KeyStoreSupporter.readTruststore(getClass().getResourceAsStream("/test-keys/test.jceks"),
                                                   KeyStoreSupporter.KeyStoreType.JCEKS);
    Assertions.assertNotNull(ks);
  }

  /**
   * asserts that the bytes of a keystore instance can be read without knowing its password
   */
  @Test
  public void testGetBytesOfKeystore() throws KeyStoreException
  {
    KeyStore keyStore = KeyStoreSupporter.readTruststore(getClass().getResourceAsStream("/test-keys/test.jceks"),
                                                         KeyStoreSupporter.KeyStoreType.JCEKS);
    byte[] keystoreBytes = KeyStoreSupporter.getBytes(keyStore, "123456");
    KeyStore newKeyStore = KeyStoreSupporter.readTruststore(keystoreBytes, KeyStoreSupporter.KeyStoreType.JCEKS);
    Assertions.assertEquals(keyStore.size(), newKeyStore.size());
    Assertions.assertThrows(KeyStoreException.class,
                            () -> KeyStoreSupporter.getBytes(KeyStore.getInstance("JKS"), "123456"));
  }

  /**
   * creates a keystore and adds a certificate as entry to it
   */
  @SneakyThrows
  @ParameterizedTest
  @ValueSource(strings = {"JKS", "JCEKS", "PKCS12"})
  public void testToKeystore(KeyStoreSupporter.KeyStoreType keyStoreType)
  {
    final String alias = "test";
    final String password = "123456";

    X509Certificate certificate = getTestCertificate();
    KeyStore keyStore = Assertions.assertDoesNotThrow(() -> KeyStoreSupporter.toKeyStore(certificate,
                                                                                         alias,
                                                                                         password,
                                                                                         keyStoreType));
    Assertions.assertNotNull(keyStore.getCertificate(alias));
  }

  /**
   * creates a keystore and adds a certificate as entry to it
   */
  @Test
  public void testToKeystoreWithNullCertificate()
  {
    final String alias = "test";
    final String password = "123456";

    Assertions.assertThrows(KeyStoreEntryException.class, () -> {
      KeyStoreSupporter.toKeyStore(null, alias, password, KeyStoreSupporter.KeyStoreType.JKS);
    });
  }



  /**
   * verifies that the exact same instance is returned if a keystore has been tried to be converted to the same
   * type that it already is
   */
  @Test
  public void testConvertKeyStoreToSameType()
  {
    final String alias = "test";
    final String password = "123456";
    final KeyStoreSupporter.KeyStoreType keyStoreType = KeyStoreSupporter.KeyStoreType.PKCS12;

    X509Certificate certificate = getTestCertificate();
    KeyStore keyStore = Assertions.assertDoesNotThrow(() -> KeyStoreSupporter.toKeyStore(certificate,
                                                                                         alias,
                                                                                         password,
                                                                                         keyStoreType));

    KeyStore convertedKeystore = KeyStoreSupporter.convertKeyStore(keyStore, password, keyStoreType);
    Assertions.assertEquals(keyStore, convertedKeystore);
  }

  @SneakyThrows
  @TestFactory
  public List<DynamicTest> testCopyKeystoreEntries()
  {
    KeyPair master = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    DistinguishedName masterDn = DistinguishedName.builder().commonName("master").build();
    X509Certificate masterCertificate = CertificateCreator.createX509SelfSignedCertificate(master,
                                                                                           masterDn,
                                                                                           new Date(),
                                                                                           new Date(System.currentTimeMillis()
                                                                                                    + 1000L * 60 * 60
                                                                                                      * 24));
    KeyPair child = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    DistinguishedName childDn = DistinguishedName.builder().commonName("child").build();
    X509Certificate childCertificate = CertificateCreator.createSignedX509Certificate(childDn,
                                                                                      masterDn,
                                                                                      new Date(),
                                                                                      new Date(System.currentTimeMillis()
                                                                                               + 1000L * 60 * 60 * 24),
                                                                                      master.getPrivate(),
                                                                                      child.getPublic());

    final X509Certificate[] certificateChain = new X509Certificate[]{childCertificate, masterCertificate};
    final String alias = "test";
    final String password = "123456";

    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(DynamicTest.dynamicTest("copy private key and certificate chain", () -> {
      KeyStore keyStore1 = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.PKCS12, password);
      keyStore1 = KeyStoreSupporter.addEntryToKeystore(keyStore1,
                                                       alias,
                                                       child.getPrivate(),
                                                       certificateChain,
                                                       password);

      KeyStore keyStore2 = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JKS, password);


      KeyStoreSupporter.tryCopyEntry(keyStore1,
                                     password,
                                     password,
                                     KeyStoreSupporter.KeyStoreType.PKCS12,
                                     keyStore2,
                                     alias);
      Assertions.assertEquals(keyStore1.size(), keyStore2.size());

      Certificate[] chain1 = keyStore1.getCertificateChain(alias);
      Certificate[] chain2 = keyStore2.getCertificateChain(alias);
      Assertions.assertEquals(chain1.length, chain2.length);
      Assertions.assertArrayEquals(keyStore1.getKey(alias, password.toCharArray()).getEncoded(),
                                   keyStore2.getKey(alias, password.toCharArray()).getEncoded());

      for ( int i = 0 ; i < certificateChain.length ; i++ )
      {
        Assertions.assertArrayEquals(certificateChain[i].getEncoded(), chain1[i].getEncoded());
        Assertions.assertArrayEquals(certificateChain[i].getEncoded(), chain2[i].getEncoded());
      }
    }));
    dynamicTests.add(DynamicTest.dynamicTest("add certificate chain to keystore", () -> {
      KeyStore keyStore1 = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.PKCS12, password);
      Assertions.assertThrows(IllegalArgumentException.class,
                              () -> KeyStoreSupporter.addEntryToKeystore(keyStore1,
                                                                         alias,
                                                                         null,
                                                                         certificateChain,
                                                                         null));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("add null values to keystore", () -> {
      KeyStore keyStore1 = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.PKCS12, password);
      Assertions.assertThrows(IllegalArgumentException.class,
                              () -> KeyStoreSupporter.addEntryToKeystore(keyStore1, alias, null, null, null));
    }));
    dynamicTests.add(DynamicTest.dynamicTest("copy single certificate entry", () -> {
      KeyStore keyStore1 = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JKS, password);
      keyStore1 = KeyStoreSupporter.addEntryToKeystore(keyStore1,
                                                       alias,
                                                       null,
                                                       new Certificate[]{masterCertificate},
                                                       password);

      KeyStore keyStore2 = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JKS, password);


      KeyStoreSupporter.tryCopyEntry(keyStore1, password, null, KeyStoreSupporter.KeyStoreType.JKS, keyStore2, alias);
      Assertions.assertEquals(keyStore1.size(), keyStore2.size());

      Assertions.assertNull(keyStore1.getKey(alias, password.toCharArray()));
      Assertions.assertNull(keyStore2.getKey(alias, password.toCharArray()));
      Assertions.assertArrayEquals(keyStore1.getCertificate(alias).getEncoded(),
                                   keyStore2.getCertificate(alias).getEncoded());
    }));
    dynamicTests.add(DynamicTest.dynamicTest("copy none existing alias", () -> {
      KeyStore keyStore1 = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JKS, password);
      KeyStore keyStore1_ = KeyStoreSupporter.addEntryToKeystore(keyStore1,
                                                                 alias,
                                                                 null,
                                                                 new Certificate[]{masterCertificate},
                                                                 password);

      KeyStore keyStore2 = KeyStoreSupporter.createEmptyKeyStore(KeyStoreSupporter.KeyStoreType.JKS, password);


      Assertions.assertDoesNotThrow(() -> KeyStoreSupporter.tryCopyEntry(keyStore1_,
                                                                         password,
                                                                         null,
                                                                         KeyStoreSupporter.KeyStoreType.JKS,
                                                                         keyStore2,
                                                                         "not-existing"));
      Assertions.assertEquals(0, keyStore2.size());
    }));
    return dynamicTests;
  }

  /**
   * verifies that an alias cannot be added twice to a keystore
   */
  @Test
  public void testAddKeystoreWithAlreadyExistingAlias()
  {
    KeyPair keyPair = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate = CertificateCreator.createX509SelfSignedCertificate(keyPair,
                                                                                     distinguishedName,
                                                                                     new Date(),
                                                                                     new Date(System.currentTimeMillis()
                                                                                              + 1000L * 60 * 60 * 24));
    String alias = "alias";
    String password = "password";
    KeyStore keyStore = KeyStoreSupporter.toKeyStore(keyPair.getPrivate(),
                                                     certificate,
                                                     alias,
                                                     password,
                                                     KeyStoreSupporter.KeyStoreType.JKS);

    KeyPair keyPair2 = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate2 = CertificateCreator.createX509SelfSignedCertificate(keyPair2,
                                                                                      distinguishedName,
                                                                                      new Date(),
                                                                                      new Date(System.currentTimeMillis()
                                                                                               + 1000L * 60 * 60 * 24));
    Assertions.assertThrows(IllegalArgumentException.class,
                            () -> KeyStoreSupporter.addEntryToKeystore(keyStore,
                                                                       alias,
                                                                       keyPair2.getPrivate(),
                                                                       new X509Certificate[]{certificate2},
                                                                       password));

  }

  @Test
  public void testGetKeyEntryWithWrongPassword()
  {
    KeyPair keyPair = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    X509Certificate certificate = CertificateCreator.createX509SelfSignedCertificate(keyPair,
                                                                                     distinguishedName,
                                                                                     new Date(),
                                                                                     new Date(System.currentTimeMillis()
                                                                                              + 1000L * 60 * 60 * 24));
    String alias = "alias";
    String password = "password";
    KeyStore keyStore = KeyStoreSupporter.toKeyStore(keyPair.getPrivate(),
                                                     certificate,
                                                     alias,
                                                     password,
                                                     KeyStoreSupporter.KeyStoreType.JKS);

    X509Certificate newCertEntry = getTestCertificate();
    Assertions.assertThrows(KeyStoreException.class,
                            () -> KeyStoreSupporter.addEntryToKeystore(keyStore,
                                                                       alias,
                                                                       null,
                                                                       new X509Certificate[]{newCertEntry},
                                                                       "wrong-password"));
  }

  @Test
  public void testGetKeyEntryWithUninitializedKeystore()
  {
    Assertions.assertThrows(KeyStoreException.class,
                            () -> KeyStoreSupporter.getKeyEntry(KeyStore.getInstance("JKS"), "alias", "123456"));
  }

  @Test
  public void testGetCertificateWithUninitializedKeystore()
  {
    Assertions.assertThrows(KeyStoreException.class,
                            () -> KeyStoreSupporter.getCertificate(KeyStore.getInstance("JKS"), "alias"));
  }

  @Test
  public void testGetCertificateChainWithUninitializedKeystore()
  {
    Assertions.assertThrows(KeyStoreException.class,
                            () -> KeyStoreSupporter.getCertificateChain(KeyStore.getInstance("JKS"), "alias"));
  }

  @Test
  public void testGetParseNullFile()
  {
    Assertions.assertFalse(KeyStoreSupporter.KeyStoreType.byFileExtension(null).isPresent());
  }

  @Test
  public void testGetParsePfxFileExtension()
  {
    Assertions.assertEquals(KeyStoreSupporter.KeyStoreType.PKCS12,
                            KeyStoreSupporter.KeyStoreType.byFileExtension("hello-world.pfx").get());
  }

  @Test
  public void testGetParseUnknownFileExtension()
  {
    Assertions.assertFalse(KeyStoreSupporter.KeyStoreType.byFileExtension("hello-world.txt").isPresent());
  }

  public X509Certificate getTestCertificate()
  {
    KeyPair keyPair1 = generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    return CertificateCreator.createX509SelfSignedCertificate(keyPair1,
                                                              distinguishedName,
                                                              new Date(),
                                                              new Date(System.currentTimeMillis()
                                                                       + 1000L * 60 * 60 * 24));
  }

  /**
   * returns an error message
   */
  private String getErrorTextFileNotCreated(File targetFile)
  {
    return "Die Datei '" + targetFile.getAbsolutePath() + "' wurde nicht erzeugt!";
  }

  /**
   * returns an error message
   */
  private String getErrorWrongAlias(String alias)
  {
    return "the merged keystore does not contain the alias '" + alias + "'";
  }
}
