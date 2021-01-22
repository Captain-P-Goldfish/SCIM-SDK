package de.captaingoldfish.scim.sdk.client.keys;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CertificateCreator
{

  /**
   * will create a self-signed certificate that is valid from now on for 365 days
   *
   * @param keyPair the root keypair.
   * @param dn the issuer and subject dn are the same in this case.
   * @return a X509-Certificate in version 3 valid from now for 365 days
   */
  public static X509Certificate createX509SelfSignedCertificate(KeyPair keyPair, DistinguishedName dn)
  {
    try
    {
      if (log.isTraceEnabled())
      {
        log.trace("starting creation of the self-signed-certificate");
      }
      Date startDate = new Date();
      final long oneYearInMillis = 31536000000L;
      Date expiryDate = new Date(System.currentTimeMillis() + oneYearInMillis);
      return createSignedX509Certificate(dn, dn, startDate, expiryDate, keyPair.getPrivate(), keyPair.getPublic());
    }
    catch (Exception e)
    {
      throw new IllegalStateException(e);
    }
  }

  /**
   * will create a self-signed certificate
   *
   * @param keyPair the root keypair.
   * @param dn the issuer and subject dn are the same in this case.
   * @param startDate startdate of the validity of the created certificate
   * @param expiryDate expiration date of the created certificate
   * @return a X509-Certificate in version 3
   */
  public static X509Certificate createX509SelfSignedCertificate(KeyPair keyPair,
                                                                DistinguishedName dn,
                                                                Date startDate,
                                                                Date expiryDate)
  {
    try
    {
      if (log.isTraceEnabled())
      {
        log.trace("starting creation of the self-signed-certificate");
      }
      return createSignedX509Certificate(dn, dn, startDate, expiryDate, keyPair.getPrivate(), keyPair.getPublic());
    }
    catch (Exception e)
    {
      throw new IllegalStateException(e);
    }
  }

  /**
   * this method will create a signed certificate with a default validity of one year
   * 
   * @param subjectDn the subject dn of the certificate that will be build here
   * @param issuerDn the issuer dn of the certificate that will be build here
   * @param issuerPrivateKey the private key of the issuer that shall be used to sign the certificate
   * @param subjectPublicKey the public key of the subject to create the certificate
   * @return a new certificate for the subject signed by the issuers private key
   */
  public static X509Certificate createSignedCertificate(DistinguishedName subjectDn,
                                                        DistinguishedName issuerDn,
                                                        PrivateKey issuerPrivateKey,
                                                        PublicKey subjectPublicKey)
  {
    if (log.isTraceEnabled())
    {
      log.trace("starting creation of the self-signed-certificate");
    }
    Date startDate = new Date();
    final long oneYearInMillis = 31536000000L;
    Date expiryDate = new Date(System.currentTimeMillis() + oneYearInMillis);
    return createSignedX509Certificate(subjectDn, issuerDn, startDate, expiryDate, issuerPrivateKey, subjectPublicKey);
  }

  /**
   * will create a signed certificate
   *
   * @param subjectDNText the DN of the subject that is requesting the certificate
   * @param issuerDNText the DN of the issuer that is granting the certificate
   * @param startDate startdate of the validity of the created certificate
   * @param expiryDate expiration date of the created certificate
   * @param signerPrivateKey the private key of the issuer that is granting the requested certificate to the
   *          subject.
   * @param subjectPublicKey the public key of the subject which is the core of the certificate.
   * @return a X509-Certificate in version 3
   * @throws UnsupportedDnException if one of the given DN's contains unsupported characters
   */
  public static X509Certificate createSignedX509Certificate(DistinguishedName subjectDNText,
                                                            DistinguishedName issuerDNText,
                                                            Date startDate,
                                                            Date expiryDate,
                                                            PrivateKey signerPrivateKey,
                                                            PublicKey subjectPublicKey)
  {
    if (log.isTraceEnabled())
    {
      log.trace("starting creation of the certificate");
    }
    X500Name subjectDN = subjectDNText.toX500Name();
    X500Name issuerDN = issuerDNText.toX500Name();
    if (log.isDebugEnabled())
    {
      log.debug("certificate issuer: " + issuerDNText.toString());
      log.debug("certificate subject: " + subjectDNText.toString());
    }
    // @formatter:off
    SubjectPublicKeyInfo subjPubKeyInfo = SubjectPublicKeyInfo.getInstance(
                                                          ASN1Sequence.getInstance(subjectPublicKey.getEncoded()));
    // @formatter:on
    BigInteger serialNumber = new BigInteger(130, new SecureRandom());

    X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(issuerDN, serialNumber, startDate, expiryDate,
                                                                    subjectDN, subjPubKeyInfo);
    ContentSigner contentSigner = null;
    try
    {
      // @formatter:off
      contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
                                                              .setProvider(SecurityProvider.BOUNCY_CASTLE_PROVIDER)
                                                              .build(signerPrivateKey);
      X509Certificate x509Certificate = new JcaX509CertificateConverter()
                                                              .setProvider(SecurityProvider.BOUNCY_CASTLE_PROVIDER)
                                                              .getCertificate(certGen.build(contentSigner));
      // @formatter:on
      if (log.isTraceEnabled())
      {
        log.trace("creation of the certificate was successful.");
      }
      if (log.isDebugEnabled())
      {
        log.debug("serialnumber of the new certificate: " + serialNumber);
        log.debug("certificate will be valid from: " + startDate + "\t to \t" + expiryDate);
      }
      return x509Certificate;
    }
    catch (CertificateException | OperatorCreationException e)
    {
      throw new IllegalStateException(e);
    }
  }

  /**
   * will create a signed certificate
   *
   * @param subjectDNText the DN of the subject that is requesting the certificate
   * @param startDate startdate of the validity of the created certificate
   * @param expiryDate expiration date of the created certificate
   * @param signerPrivateKey the private key of the issuer that is granting the requested certificate to the
   *          subject.
   * @param subjectPublicKey the public key of the subject which is the core of the certificate.
   * @param chain
   * @return a X509-Certificate in version 3
   * @throws UnsupportedDnException if one of the given DN's contains unsupported characters
   */
  public static X509Certificate createSignedX509Certificate_(DistinguishedName subjectDNText,
                                                             X500Name issuerDN,
                                                             Date startDate,
                                                             Date expiryDate,
                                                             PrivateKey signerPrivateKey,
                                                             PublicKey subjectPublicKey)
  {
    if (log.isTraceEnabled())
    {
      log.trace("starting creation of the certificate");
    }

    X500Name subjectDN = subjectDNText.toX500Name();
    if (log.isDebugEnabled())
    {
      log.debug("certificate issuer: " + issuerDN.toString());
      log.debug("certificate subject: " + subjectDNText.toString());
    }
    // @formatter:off
    SubjectPublicKeyInfo subjPubKeyInfo = SubjectPublicKeyInfo.getInstance(
                                                          ASN1Sequence.getInstance(subjectPublicKey.getEncoded()));
    // @formatter:on
    BigInteger serialNumber = new BigInteger(130, new SecureRandom());

    X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(issuerDN, serialNumber, startDate, expiryDate,
                                                                    subjectDN, subjPubKeyInfo);
    ContentSigner contentSigner = null;
    try
    {
      // @formatter:off
      contentSigner = new JcaContentSignerBuilder("SHA256withRSA")
                                                              .setProvider(SecurityProvider.BOUNCY_CASTLE_PROVIDER)
                                                              .build(signerPrivateKey);
      X509Certificate x509Certificate = new JcaX509CertificateConverter()
                                                              .setProvider(SecurityProvider.BOUNCY_CASTLE_PROVIDER)
                                                              .getCertificate(certGen.build(contentSigner));
      // @formatter:on
      if (log.isTraceEnabled())
      {
        log.trace("creation of the certificate was successful.");
      }
      if (log.isDebugEnabled())
      {
        log.debug("serialnumber of the new certificate: " + serialNumber);
        log.debug("certificate will be valid from: " + startDate + "\t to \t" + expiryDate);
      }
      return x509Certificate;
    }
    catch (CertificateException | OperatorCreationException e)
    {
      throw new IllegalStateException(e);
    }
  }
}
