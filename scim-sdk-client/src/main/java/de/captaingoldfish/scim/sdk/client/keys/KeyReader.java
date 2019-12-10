package de.captaingoldfish.scim.sdk.client.keys;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import de.captaingoldfish.scim.sdk.client.exceptions.CertificateCreationException;
import de.captaingoldfish.scim.sdk.client.exceptions.KeyGenerationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * project: autent-key-utils <br>
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
   * @throws KeyGenerationException if the private key could not be created from the given byte-array
   */
  public static PrivateKey readPrivateRSAKey(byte[] privateKey)
  {
    if (log.isTraceEnabled())
    {
      log.trace("trying to create private key. privateKey.length: {}-bytes", privateKey.length);
    }
    KeyFactory keyFactory = null;
    try
    {
      keyFactory = KeyFactory.getInstance("RSA", SecurityProvider.BOUNCY_CASTLE_PROVIDER);
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new KeyGenerationException("could not create private key since the RSA algorithm was not found.", e);
    }
    EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKey);
    try
    {
      return keyFactory.generatePrivate(privateKeySpec);
    }
    catch (InvalidKeySpecException e)
    {
      throw new KeyGenerationException("could not read a private rsa key from the given byte-array", e);
    }
  }

  /**
   * will read a public rsa key from a given byte-array of a {@link X509EncodedKeySpec}
   *
   * @param publicKey the bytes of the rsa key
   * @return the public-key interface implementation of rsa
   * @throws KeyGenerationException if the public key could not be created from the given byte-array
   */
  public static PublicKey readPublicRSAKey(byte[] publicKey)
  {
    if (log.isTraceEnabled())
    {
      log.trace("trying to create public key. publicKey.length: {}-bytes", publicKey.length);
    }
    KeyFactory keyFactory = null;
    try
    {
      keyFactory = KeyFactory.getInstance("RSA", SecurityProvider.BOUNCY_CASTLE_PROVIDER);
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new KeyGenerationException("could not create public key since the RSA algorithm was not found.", e);
    }
    try
    {
      return keyFactory.generatePublic(new X509EncodedKeySpec(publicKey));
    }
    catch (InvalidKeySpecException e)
    {
      throw new KeyGenerationException("could not read a public rsa key from the given byte-array", e);
    }
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
    if (log.isTraceEnabled())
    {
      log.trace("read X509 certificate. certificate.length: {}-bytes", certificateBytes.length);
    }

    return readX509Certificate(new ByteArrayInputStream(certificateBytes));
  }

  /**
   * should read a X509 certificate from the given byte-array
   *
   * @param certificateStream the certificate inputstream
   * @return the X509 certificate
   * @throws CertificateCreationException if the certificate could not be created from the given data.
   */
  public static X509Certificate readX509Certificate(InputStream certificateStream)
  {
    try (InputStream in = certificateStream)
    {
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509", SecurityProvider.BOUNCY_CASTLE_PROVIDER);
      X509Certificate x509Certificate = (X509Certificate)certFactory.generateCertificate(in);
      if (x509Certificate == null)
      {
        throw new CertificateCreationException("the byte-array does not seem to contain data of a X509"
                                               + " certificate.");
      }
      if (log.isTraceEnabled())
      {
        log.trace("X509 certificate was successfully read.");
      }
      return x509Certificate;
    }
    catch (CertificateException | IOException e)
    {
      throw new CertificateCreationException("was not able to create X509 certificate from byte-array", e);
    }
  }

  /**
   * @param keyLength the key length of the RSA key that should be generated
   * @return a new rsa keypair with the given length
   */
  public static KeyPair generateNewRsaKeyPair(int keyLength)
  {
    KeyPairGenerator keyGenerator = null;
    try
    {
      keyGenerator = KeyPairGenerator.getInstance("RSA");
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new IllegalStateException("unknown algorithm", e);
    }
    keyGenerator.initialize(keyLength);
    return keyGenerator.generateKeyPair();
  }
}
