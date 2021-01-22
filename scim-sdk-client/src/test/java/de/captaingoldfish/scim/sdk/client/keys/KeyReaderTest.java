package de.captaingoldfish.scim.sdk.client.keys;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.ExtendedInvalidKeySpecException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.SneakyThrows;


/**
 * @author Pascal Knueppel
 * @since 19.02.2021
 */
public class KeyReaderTest
{

  @Test
  public void testReadPrivateKey()
  {
    KeyPair keyPair = KeyStoreSupporterTest.generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    PrivateKey privateKey = KeyReader.readPrivateRSAKey(keyPair.getPrivate().getEncoded());
    Assertions.assertArrayEquals(keyPair.getPrivate().getEncoded(), privateKey.getEncoded());
    Assertions.assertThrows(ExtendedInvalidKeySpecException.class,
                            () -> KeyReader.readPrivateRSAKey(new byte[]{50, 50, 50}));
  }

  @SneakyThrows
  @Test
  public void testReadCertificate()
  {
    KeyPair keyPair = KeyStoreSupporterTest.generateKey(new KeyGenerationParameters(new SecureRandom(), 512));
    DistinguishedName distinguishedName = new DistinguishedName("test", null, null, null, null, null);
    X509Certificate certificate = CertificateCreator.createX509SelfSignedCertificate(keyPair,
                                                                                     distinguishedName,
                                                                                     new Date(),
                                                                                     new Date(System.currentTimeMillis()
                                                                                              + 1000L * 60 * 60 * 24));
    X509Certificate readCertificate = KeyReader.readX509Certificate(certificate.getEncoded());
    Assertions.assertArrayEquals(certificate.getEncoded(), readCertificate.getEncoded());
    Assertions.assertThrows(Exception.class, () -> KeyReader.readX509Certificate(new byte[]{50, 50, 50}));
  }
}
