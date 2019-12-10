package de.captaingoldfish.scim.sdk.client.keys;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.client.exceptions.KeyStoreReadingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 08:20 <br>
 * <br>
 * this class will be used to have the usage of a keystore wrapped in a single place. Means we will hold the
 * password of the keystore, the aliases and the key passwords within this wrapper
 */
@Slf4j
@Data
public class KeyStoreWrapper
{

  private static final String COULD_NOT_ACCESS_KEYSTORE = "could not access the given keystore";

  /**
   * the keystore that is the main object of this class
   */
  private KeyStore keyStore;

  /**
   * the password to access the keystore
   */
  private String keystorePassword;

  /**
   * a list of aliases within the keystore that have a matching key-password entry
   */
  private Map<String, AliasPasswordPair> keystoreEntries = new HashMap<>();

  /**
   * constructor to befill the entries for this class<br>
   * it is expected that the keystore-password has already been entered into the keystore object therefore we
   * wont need it anymore for the keystore.
   * 
   * @param keyStore the keystore that should be accessible
   * @param privateKeyPassword the password used to access the private keys (the keystore is expected to contain
   *          a single entry)
   */
  public KeyStoreWrapper(KeyStore keyStore, String privateKeyPassword)
  {
    this.keyStore = keyStore;
    this.keystorePassword = privateKeyPassword;
  }

  /**
   * constructor to befill the entries for this class<br>
   * it is expected that the keystore-password has already been entered into the keystore object therefore we
   * wont need it anymore<br>
   * <br>
   * <b>The keystore is expected to be of type JKS</b>
   *
   * @param keyStore the keystore that should be accessible
   * @param keystorePassword the keystore to open the keystore
   */
  public KeyStoreWrapper(byte[] keyStore, String keystorePassword)
  {
    this(keyStore, KeyStoreSupporter.KeyStoreType.JKS, keystorePassword);
  }

  /**
   * constructor to befill the entries for this class<br>
   * it is expected that the keystore-password has already been entered into the keystore object therefore we
   * wont need it anymore
   *
   * @param keyStore the keystore that should be accessible
   * @param keyStoreType the type of the keystore
   * @param keystorePassword the keystore to open the keystore
   */
  public KeyStoreWrapper(byte[] keyStore, KeyStoreSupporter.KeyStoreType keyStoreType, String keystorePassword)
  {
    this(new ByteArrayInputStream(keyStore), keyStoreType, keystorePassword);
  }

  /**
   * constructor to befill the entries for this class<br>
   * it is expected that the keystore-password has already been entered into the keystore object therefore we
   * wont need it anymore<br>
   * <br>
   * <b>The keystore is expected to be of type JKS</b>
   *
   * @param keyStore the keystore that should be accessible
   * @param keystorePassword the keystore to open the keystore
   */
  public KeyStoreWrapper(InputStream keyStore, String keystorePassword)
  {
    this(keyStore, KeyStoreSupporter.KeyStoreType.JKS, keystorePassword);
  }

  /**
   * constructor to befill the entries for this class<br>
   * it is expected that the keystore-password has already been entered into the keystore object therefore we
   * wont need it anymore
   *
   * @param keyStore the keystore that should be accessible
   * @param keyStoreType the type of the keystore
   * @param keystorePassword the keystore to open the keystore
   */
  public KeyStoreWrapper(InputStream keyStore, KeyStoreSupporter.KeyStoreType keyStoreType, String keystorePassword)
  {
    this.keyStore = KeyStoreSupporter.readKeyStore(keyStore, keyStoreType, keystorePassword);
    this.keystorePassword = keystorePassword;
  }

  /**
   * constructor to befill the entries for this class<br>
   * it is expected that the keystore-password has already been entered into the keystore object therefore we
   * wont need it anymore
   *
   * @param keyStore the keystore that should be accessible
   * @param keystorePassword the keystore to open the keystore
   * @param aliasPasswordPair a single alias key-password pair to access at least a single entry within the
   *          keystore
   * @param aliasPasswordPairs a list of alias key-password pairs to access other entreies as well
   */
  public KeyStoreWrapper(KeyStore keyStore,
                         String keystorePassword,
                         AliasPasswordPair aliasPasswordPair,
                         AliasPasswordPair... aliasPasswordPairs)
  {
    this(keyStore, keystorePassword);
    this.keystoreEntries.put(aliasPasswordPair.getAlias(), aliasPasswordPair);
    if (aliasPasswordPairs != null)
    {
      Arrays.stream(aliasPasswordPairs).forEach(app -> keystoreEntries.put(app.getAlias(), app));
    }
  }

  /**
   * constructor to befill the entries for this class
   *
   * @param keyStore the keystore that should be accessible
   * @param keyStoreType to resolve the given keystore into its appropriate type
   * @param keystorePassword the password to access the keystore if necessary
   * @param aliasPasswordPair a single alias key-password pair to access at least a single entry within the
   *          keystore
   * @param aliasPasswordPairs a list of alias key-password pairs to access other entreies as well
   */
  public KeyStoreWrapper(byte[] keyStore,
                         KeyStoreSupporter.KeyStoreType keyStoreType,
                         String keystorePassword,
                         AliasPasswordPair aliasPasswordPair,
                         AliasPasswordPair... aliasPasswordPairs)
  {
    this(KeyStoreSupporter.readKeyStore(keyStore, keyStoreType, keystorePassword), keystorePassword, aliasPasswordPair,
         aliasPasswordPairs);
    this.keystorePassword = keystorePassword;
  }

  /**
   * constructor to befill the entries for this class
   *
   * @param keyStore the keystore that should be accessible
   * @param keyStoreType to resolve the given keystore into its appropriate type
   * @param keystorePassword the password to access the keystore if necessary
   * @param aliasPasswordPair a single alias key-password pair to access at least a single entry within the
   *          keystore
   * @param aliasPasswordPairs a list of alias key-password pairs to access other entreies as well
   */
  public KeyStoreWrapper(InputStream keyStore,
                         KeyStoreSupporter.KeyStoreType keyStoreType,
                         String keystorePassword,
                         AliasPasswordPair aliasPasswordPair,
                         AliasPasswordPair... aliasPasswordPairs)
  {
    this(KeyStoreSupporter.readKeyStore(keyStore, keyStoreType, keystorePassword), keystorePassword, aliasPasswordPair,
         aliasPasswordPairs);
    this.keystorePassword = keystorePassword;
  }

  /**
   * constructor to befill the entries for this class
   *
   * @param keyStore the keystore that should be accessible. <b>It is necessary for the keystore to have an
   *          appropriate file ending like 'jks', 'jceks', 'p12' or 'pfx'!</b>
   * @param keystorePassword the password to access the keystore if necessary
   * @param aliasPasswordPair a single alias key-password pair to access at least a single entry within the
   *          keystore
   * @param aliasPasswordPairs a list of alias key-password pairs to access other entreies as well
   */
  public KeyStoreWrapper(File keyStore,
                         String keystorePassword,
                         AliasPasswordPair aliasPasswordPair,
                         AliasPasswordPair... aliasPasswordPairs)
  {
    this(KeyStoreSupporter.readKeyStore(keyStore, keystorePassword), keystorePassword, aliasPasswordPair,
         aliasPasswordPairs);
    this.keystorePassword = keystorePassword;
  }

  /**
   * will extract the private key for the given alias
   * 
   * @param alias the keystore entry to get the private key from
   * @return the private key of the alias
   */
  public Optional<PrivateKey> getPrivateKey(String alias)
  {
    if (keyStore == null || StringUtils.isBlank(alias))
    {
      return Optional.empty();
    }
    AliasPasswordPair aliasPasswordPair = keystoreEntries.get(alias);
    try
    {
      PrivateKey privateKey;
      if (aliasPasswordPair == null)
      {
        privateKey = (PrivateKey)keyStore.getKey(alias, keystorePassword.toCharArray());
      }
      else
      {
        privateKey = (PrivateKey)keyStore.getKey(alias, aliasPasswordPair.getKeyPassword().toCharArray());
      }
      if (privateKey == null && log.isWarnEnabled())
      {
        log.warn("no private key found for alias: {}", alias);
      }
      return Optional.ofNullable(privateKey);
    }
    catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e)
    {
      throw new KeyStoreReadingException("could not read keystore entry with alias: " + alias, e);
    }
  }

  /**
   * this method will extract a private key with the given alias and the given password
   * 
   * @param alias the alias that holds the private key
   * @param password the password to access the private key
   * @return the private key or null if no entry was found
   */
  public Optional<PrivateKey> getPrivateKey(String alias, String password)
  {
    if (keyStore == null || StringUtils.isBlank(alias) || password == null)
    {
      return Optional.empty();
    }
    try
    {
      PrivateKey privateKey = (PrivateKey)keyStore.getKey(alias, password.toCharArray());
      if (privateKey == null && log.isWarnEnabled())
      {
        log.warn("no private key found for alias: {}", alias);
      }
      return Optional.ofNullable(privateKey);
    }
    catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e)
    {
      throw new KeyStoreReadingException("could not read keystore entry with alias: " + alias, e);
    }
  }

  /**
   * will read the certificate from the given alias
   * 
   * @param alias the keystore entry to read
   * @return the certificate under the given keystore entry
   */
  public Optional<X509Certificate> getCertificate(String alias)
  {
    if (keyStore == null || StringUtils.isBlank(alias))
    {
      return Optional.empty();
    }
    try
    {
      X509Certificate x509Certificate = (X509Certificate)keyStore.getCertificate(alias);
      if (x509Certificate == null && log.isWarnEnabled())
      {
        log.warn("no certificate entry found for alias: {}", alias);
      }
      return Optional.ofNullable(x509Certificate);
    }
    catch (KeyStoreException e)
    {
      throw new KeyStoreReadingException("could not read certificate with alias: " + alias, e);
    }
  }

  /**
   * will read the certificate from the given alias
   * 
   * @param alias the keystore entry to read
   * @return the certificate under the given keystore entry
   */
  public Optional<X509Certificate[]> getCertificateChain(String alias)
  {
    if (keyStore == null || StringUtils.isBlank(alias))
    {
      return Optional.empty();
    }
    try
    {
      Certificate[] chain = keyStore.getCertificateChain(alias);

      if (chain == null && log.isWarnEnabled())
      {
        log.warn("no certificate entry found for alias: {}", alias);
      }
      if (chain == null)
      {
        return Optional.empty();
      }
      else
      {
        X509Certificate[] x509Certificate = new X509Certificate[chain.length];
        for ( int i = 0 ; i < chain.length ; i++ )
        {
          x509Certificate[i] = (X509Certificate)chain[i];
        }
        return Optional.of(x509Certificate);
      }
    }
    catch (KeyStoreException e)
    {
      throw new KeyStoreReadingException("could not read certificate with alias: " + alias, e);
    }
  }

  /**
   * will return all aliases as list. <br>
   * this is just a convenience method to prevent handling with {@link KeyStoreException}
   */
  public List<String> getAliasesAsList()
  {
    if (keyStore == null)
    {
      throw new KeyStoreReadingException(COULD_NOT_ACCESS_KEYSTORE);
    }
    try
    {
      Enumeration<String> aliasEnumeration = keyStore.aliases();
      List<String> aliases = new ArrayList<>();
      while (aliasEnumeration.hasMoreElements())
      {
        aliases.add(aliasEnumeration.nextElement());
      }
      return aliases;
    }
    catch (KeyStoreException e)
    {
      throw new KeyStoreReadingException(COULD_NOT_ACCESS_KEYSTORE, e);
    }
  }

  /**
   * will return all aliases without having to handle the exception. <br>
   * this is just a convenience method to prevent handling with {@link KeyStoreException}
   */
  public Enumeration<String> getAliases()
  {
    if (keyStore == null)
    {
      throw new KeyStoreReadingException(COULD_NOT_ACCESS_KEYSTORE);
    }
    try
    {
      return keyStore.aliases();
    }
    catch (KeyStoreException e)
    {
      throw new KeyStoreReadingException(COULD_NOT_ACCESS_KEYSTORE, e);
    }
  }

  /**
   * used as data holder to hold the key-passwords for any alias
   */
  @Data
  @AllArgsConstructor
  @Builder
  public static class AliasPasswordPair
  {

    /**
     * the alias of the given keystore {@link #keyStore}
     */
    private String alias;

    /**
     * the key-password to access the private key under {@link #alias}
     */
    private String keyPassword;

  }

}
