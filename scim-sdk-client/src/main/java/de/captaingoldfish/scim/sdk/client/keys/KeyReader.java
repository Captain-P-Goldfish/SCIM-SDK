package de.captaingoldfish.scim.sdk.client.keys;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * author: Pascal Knueppel <br>
 * created at: 02.03.2017 <br>
 * <br>
 * will provide support methods to translate keys to java objects
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KeyReader
{

  /**
   * will read a private rsa key from a given byte-array of a {@link PKCS8EncodedKeySpec}
   *
   * @param privateKey the bytes of the rsa key
   * @return the private-key interface implementation of rsa
   */
  @SneakyThrows
  public static PrivateKey readPrivateRSAKey(byte[] privateKey)
  {
    log.trace("trying to create private key. privateKey.length: {}-bytes", privateKey.length);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA", SecurityProvider.BOUNCY_CASTLE_PROVIDER);
    EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKey);
    return keyFactory.generatePrivate(privateKeySpec);
  }

  /**
   * should read a X509 certificate from the given byte-array
   *
   * @param certificateBytes the bytes of the certificate
   * @return the X509 certificate
   * @throws CertificateCreationException if the certificate could not be created from the given data.
   */
  public static X509Certificate readX509Certificate(byte[] certificateBytes)
  {
    return readX509Certificate(new ByteArrayInputStream(certificateBytes));
  }

  /**
   * should read a X509 certificate from the given byte-array
   *
   * @param certificateStream the certificate inputstream
   * @return the X509 certificate
   * @throws CertificateCreationException if the certificate could not be created from the given data.
   */
  @SneakyThrows
  public static X509Certificate readX509Certificate(InputStream certificateStream)
  {
    try (InputStream in = certificateStream)
    {
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509", SecurityProvider.BOUNCY_CASTLE_PROVIDER);
      X509Certificate x509Certificate = (X509Certificate)certFactory.generateCertificate(in);
      log.trace("X509 certificate was successfully read.");
      return x509Certificate;
    }
  }
}
