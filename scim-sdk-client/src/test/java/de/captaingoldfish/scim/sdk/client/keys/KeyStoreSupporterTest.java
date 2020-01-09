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
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.exceptions.KeyGenerationException;
import de.captaingoldfish.scim.sdk.client.exceptions.KeyStoreCreationFailedException;
import de.captaingoldfish.scim.sdk.client.exceptions.KeyStoreEntryException;


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
      throw new KeyGenerationException("RSA-Schluessel konnte nicht erzeugt werden", e);
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
    keyStore = KeyStoreSupporter.addCertificateEntryToKeyStore(jksKeyStore, certificate2, alias);
    Assertions.assertArrayEquals(certificate2.getEncoded(),
                                 KeyStoreSupporter.getCertificate(keyStore, alias + "_").get().getEncoded());
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
   * this test shall show that a keystore can be saved within a file with the correct file extensions.
   */
  @Test
  public void testKeyStoreToFile()
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

    File targetDirectory = new File(".");
    String filename = "testKeyStoreFile";

    saveJksKeyStore:
    {
      File targetFile = new File(targetDirectory.getAbsolutePath() + "/" + filename + ".jks");
      KeyStoreSupporter.keyStoreToFile(targetDirectory, filename, jksKeyStore, password);
      Assertions.assertTrue(targetFile.exists(), getErrorTextFileNotCreated(targetFile));
      targetFile.delete();
    }

    saveJceksKeyStore:
    {
      KeyStore jceksKeyStore = KeyStoreSupporter.convertKeyStore(jksKeyStore,
                                                                 password,
                                                                 KeyStoreSupporter.KeyStoreType.JCEKS);
      File targetFile = new File(targetDirectory.getAbsolutePath() + "/" + filename + ".jceks");
      KeyStoreSupporter.keyStoreToFile(targetDirectory, filename, jceksKeyStore, password);
      Assertions.assertTrue(targetFile.exists(), getErrorTextFileNotCreated(targetFile));
      targetFile.delete();
    }

    savePkcs12KeyStore:
    {
      KeyStore pkcs12KeyStore = KeyStoreSupporter.convertKeyStore(jksKeyStore,
                                                                  password,
                                                                  KeyStoreSupporter.KeyStoreType.PKCS12);
      File targetFile = new File(targetDirectory.getAbsolutePath() + "/" + filename + ".p12");
      KeyStoreSupporter.keyStoreToFile(targetDirectory, filename, pkcs12KeyStore, password);
      Assertions.assertTrue(targetFile.exists(), getErrorTextFileNotCreated(targetFile));
      targetFile.delete();
    }
  }

  /**
   * This test will show that keystores can be read from files and byte-arrays
   */
  @Test
  public void testReadKeyStore() throws KeyStoreException, IOException
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

    createJksKeyStoreFile:
    {
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
      targetFile.delete();
    }

    createJceksKeyStoreFile:
    {
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
      targetFile.delete();
    }

    createPkcs12KeyStoreFile:
    {
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
      targetFile.delete();
    }
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
    Assertions.assertArrayEquals(certificate2.getEncoded(), cert2.getEncoded());
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
    String alias2 = "alias";
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

    alias2 += "_";

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
   * This test shows that merging of two keystore's will be successful even if keystore1 and keystore2 share an
   * identical certificate-entry.
   */
  @Test
  public void testMergeKeyStores3() throws Exception
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

    String password2 = password1;
    KeyStore keyStore2 = KeyStoreSupporter.convertKeyStore(keyStore1, password1, KeyStoreSupporter.KeyStoreType.JCEKS);

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

    Assertions.assertEquals(1, aliases.size());
    Assertions.assertTrue(aliases.contains(alias1), getErrorWrongAlias(alias1));

    Key key1 = mergedKeyStore.getKey(alias1, mergedKeyStoreKeyPasswords.toCharArray());
    Assertions.assertArrayEquals(keyPair1.getPrivate().getEncoded(), key1.getEncoded());

    Certificate cert1 = mergedKeyStore.getCertificate(alias1);
    Assertions.assertArrayEquals(certificate1.getEncoded(), cert1.getEncoded());
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
    Assertions.assertThrows(KeyStoreCreationFailedException.class,
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
