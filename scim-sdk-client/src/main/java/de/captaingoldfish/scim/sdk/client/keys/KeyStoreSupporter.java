package de.captaingoldfish.scim.sdk.client.keys;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.captaingoldfish.scim.sdk.client.exceptions.KeyStoreCreationFailedException;
import de.captaingoldfish.scim.sdk.client.exceptions.KeyStoreEntryException;
import de.captaingoldfish.scim.sdk.client.exceptions.KeyStoreReadingException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * author: Pascal Knueppel <br>
 * created at: 09.12.2019 <br>
 * <br>
 * This class is meant to provide additional operations to work with keystores. This implies adding new
 * entries into a keystore, reading entries, convert a keystore from jks to pkcs12 or vice versa etc.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyStoreSupporter
{

  /**
   * used for JKS keystores
   */
  private static final String SUN_PROVIDER = "SUN";

  /**
   * used for JCEKS keystores
   */
  private static final String SUN_JCE_PROVIDER = "SunJCE";

  /**
   * this method will make sure that the correct security provider is chosen for the different keystore types.
   * The experience shows us that {@link BouncyCastleProvider} is often tried to be used for JKS and JCEKS
   * keystores. But bouncy castle cannot handle these types why we are chosing the providers manually here
   *
   * @param keyStoreType the keystore type for which a provider is needed.
   * @return the provider that can handle the given keystore
   */
  public static Provider selectProvider(KeyStoreType keyStoreType)
  {
    if (keyStoreType.equals(KeyStoreType.PKCS12))
    {
      return SecurityProvider.BOUNCY_CASTLE_PROVIDER;
    }
    else if (keyStoreType.equals(KeyStoreType.JKS))
    {
      return Security.getProvider(SUN_PROVIDER);
    }
    else
    {
      return Security.getProvider(SUN_JCE_PROVIDER);
    }
  }

  /**
   * creates a keystore from the given {@code privateKey} and the {@code certificate}
   *
   * @param privateKey the private key that should be packed into a keystore
   * @param certificate the certificate that should be packed into the keystore alongside the private key
   * @param alias the alias that should be used for the private key and the certificate
   * @param keystorePassword the password to safe the keystore and the private key
   * @param keyStoreType the type of the keystore
   * @return the keystore with the private key and the certificate
   * @throws KeyStoreCreationFailedException if the algorithm of the {@code keyStoreType} could not be resolved
   * @throws KeyStoreEntryException if the certificate or private key could not be added to the keystore
   */
  @SneakyThrows
  public static KeyStore toKeyStore(PrivateKey privateKey,
                                    Certificate certificate,
                                    String alias,
                                    String keystorePassword,
                                    KeyStoreType keyStoreType)
  {
    log.trace("putting private key and certificate into a keystore of type '{}'", keyStoreType.name());

    if (privateKey == null)
    {
      throw new KeyStoreEntryException("private key is null and cannot be added into a keystore!");
    }
    if (certificate == null)
    {
      throw new KeyStoreEntryException("certificate is null and thus the given private key cannot be added to"
                                       + " the keystore!");
    }

    KeyStore keyStore = createEmptyKeyStore(keyStoreType, keystorePassword);

    addCertificateEntryToKeyStore(keyStore, certificate, alias);

    log.trace("adding the private key to the keystore with alias '{}'", alias);
    Certificate[] certificateChain = {certificate};
    keyStore.setEntry(alias,
                      new KeyStore.PrivateKeyEntry(privateKey, certificateChain),
                      new KeyStore.PasswordProtection(keystorePassword.toCharArray()));

    return keyStore;
  }

  /**
   * creates a keystore from the given {@code certificate}
   *
   * @param certificate the certificate that should be packed into the keystore alongside the private key
   * @param alias the alias that should be used for the private key and the certificate
   * @param keystorePassword the password to safe the keystore and the private key
   * @param keyStoreType the type of the keystore
   * @return the keystore with the private key and the certificate
   * @throws KeyStoreCreationFailedException if the algorithm of the {@code keyStoreType} could not be resolved
   * @throws KeyStoreEntryException if the certificate or private key could not be added to the keystore
   */
  public static KeyStore toKeyStore(Certificate certificate,
                                    String alias,
                                    String keystorePassword,
                                    KeyStoreType keyStoreType)
  {
    log.trace("putting private key and certificate into a keystore of type '{}'", keyStoreType.name());

    if (certificate == null)
    {
      throw new KeyStoreEntryException("certificate is null and thus the given private key cannot be added to"
                                       + " the keystore!");
    }

    KeyStore keyStore = createEmptyKeyStore(keyStoreType, keystorePassword);
    addCertificateEntryToKeyStore(keyStore, certificate, alias);

    return keyStore;
  }

  /**
   * creates a keystore from the given {@code privateKey} and the {@code certificate}
   *
   * @param privateKeyBytes the private key that should be packed into a keystore
   * @param certificateBytes the certificate that should be packed into the keystore alongside the private key
   * @param alias the alias that should be used for the private key and the certificate
   * @param keystorePassword the password to safe the keystore and the private key
   * @param keyStoreType the type of the keystore
   * @return the keystore with the private key and the certificate
   * @throws KeyStoreCreationFailedException if the algorithm of the {@code keyStoreType} could not be resolved
   * @throws KeyStoreEntryException if the certificate or private key could not be added to the keystore
   * @throws KeyGenerationException if the private key could not be created from the given byte-array
   * @throws CertificateCreationException if the certificate could not be created from the given data.
   */
  public static KeyStore toKeyStore(byte[] privateKeyBytes,
                                    byte[] certificateBytes,
                                    String alias,
                                    String keystorePassword,
                                    KeyStoreType keyStoreType)
  {
    PrivateKey privateKey = KeyReader.readPrivateRSAKey(privateKeyBytes);
    Certificate certificate = KeyReader.readX509Certificate(certificateBytes);
    return toKeyStore(privateKey, certificate, alias, keystorePassword, keyStoreType);
  }

  /**
   * will convert the given keystore into a byte array
   *
   * @param keyStore the keystore that should be converted
   * @param password the keystore password that will be used as encryption password for the keystore
   * @return the byte array that contains the data of the given keystore
   */
  @SneakyThrows
  public static byte[] getBytes(KeyStore keyStore, String password)
  {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
    {
      keyStore.store(outputStream, password.toCharArray());
      return outputStream.toByteArray();
    }
  }

  /**
   * creates an empty keystore
   *
   * @param keyStoreType the type of keystore to create
   * @param keystorePassword the password to secure the keystore
   * @return the newly created empty keystore-instance
   */
  @SneakyThrows
  public static KeyStore createEmptyKeyStore(KeyStoreType keyStoreType, String keystorePassword)
  {
    KeyStore keyStore;
    Provider provider = selectProvider(keyStoreType);
    log.trace("creating a {} keystore with '{}' Provider", keyStoreType, provider.getName());
    keyStore = KeyStore.getInstance(keyStoreType.name(), provider);
    keyStore.load(null, keystorePassword == null ? null : keystorePassword.toCharArray());
    return keyStore;
  }

  /**
   * this method simply adds a certificate entry to the given keystore. This method is only to extend the adding
   * of certificate method by logging and it prevents overriding existing entries.
   *
   * @param keyStore the keystore to which the certificate should be added
   * @param certificate the certificate to add to the given keystore
   * @param alias the alias that will be used for the certificate entry.
   * @return the keystore that was also given as parameter with the added certificate.
   */
  public static KeyStore addCertificateEntryToKeyStore(KeyStore keyStore, Certificate certificate, String alias)
  {
    Optional<Certificate> certificateOptional = getCertificate(keyStore, alias);
    if (certificateOptional.isPresent())
    {
      throw new IllegalArgumentException(String.format("certificate entry for alias '%s' does already exist", alias));
    }
    else
    {
      addCertificateEntry(keyStore, alias, certificate);
    }
    return keyStore;
  }

  /**
   * convenience method for adding a certificate entry to the given keystore under the given alias, without
   * having to handle the checked exception
   *
   * @param keyStore the keystore to extend with the certificate
   * @param alias the alias under which the certificate should be stored
   * @param certificate the certificate for the new entry
   * @return the keystore that was given as parameter
   */
  @SneakyThrows
  public static KeyStore addCertificateEntry(KeyStore keyStore, String alias, Certificate certificate)
  {
    keyStore.setCertificateEntry(alias, certificate);
    log.trace("successfully added certificate entry under alias '{}' to keystore '{}'", alias, keyStore);
    return keyStore;
  }

  /**
   * will try to add the given entry under the given alias to the given keystore
   *
   * @param keyStore the keystore to which the key entry should be added
   * @param alias the alias to use for the key-entry
   * @param key the key to set under the given alias
   * @param password the password to secure the key within the keystore
   * @return the same keystore that was given as parameter
   */
  public static KeyStore addEntryToKeystore(KeyStore keyStore,
                                            String alias,
                                            Key key,
                                            Certificate[] certificateChain,
                                            String password)
  {
    Optional<Key> existingKey = getKeyEntry(keyStore, alias, password);
    if (existingKey.isPresent())
    {
      throw new IllegalArgumentException(String.format("key entry for alias '%s' does already exist", alias));
    }
    else
    {
      addEntryToKeystore(keyStore, alias, key, password, certificateChain);
    }
    return keyStore;
  }

  /**
   * will add a key entry with its certificate chain to the given keystore
   *
   * @param keyStore the keystore to which the key-entry should be added
   * @param alias the alias under which to store the key-entry
   * @param key the key to put into the keystore under the given alias
   * @param password the password to secure the given key
   * @param certificateChain the certificate chain for the given key
   * @return the keystore that was passed as parameter
   */
  @SneakyThrows
  private static KeyStore addEntryToKeystore(KeyStore keyStore,
                                             String alias,
                                             Key key,
                                             String password,
                                             Certificate... certificateChain)
  {
    if (key == null)
    {
      if (certificateChain == null)
      {
        throw new IllegalArgumentException("missing certificate");
      }
      if (certificateChain.length > 1)
      {
        throw new IllegalArgumentException("only a single certificate can be added as certificate entry");
      }
      keyStore.setCertificateEntry(alias, certificateChain[0]);
    }
    else
    {
      keyStore.setKeyEntry(alias, key, password.toCharArray(), certificateChain);
    }
    log.trace("successfully added key-entry under alias '{}' to keystore '{}'", alias, keyStore);
    return keyStore;
  }

  /**
   * This method will convert a given keystore with all its entries into another type of keystore. <br>
   * this will of course only work if the private key passwords are matching the keystore password.
   *
   * @param keyStore the kystore that shall be converted
   * @param keyStorePassword the password to open the keystore
   * @param keyStoreType the type to which the keystore should be converted
   * @return the converted keystore.
   */
  public static KeyStore convertKeyStore(KeyStore keyStore, String keyStorePassword, KeyStoreType keyStoreType)
  {
    if (keyStore.getType().equals(keyStoreType.name()))
    {
      log.warn("you tried to convert type '{}' to type '{}', this is unnecessary and the original "
               + "keystore will be returned.",
               keyStore.getType(),
               keyStoreType.name());
      return keyStore;
    }

    log.trace("converting '{}'-keystore to '{}'-type", keyStore.getType(), keyStoreType.name());
    Enumeration<String> aliases = getAliases(keyStore);

    KeyStore newKeyStore = createEmptyKeyStore(keyStoreType, keyStorePassword);
    while (aliases.hasMoreElements())
    {
      String alias = aliases.nextElement();
      tryCopyEntry(keyStore, keyStorePassword, keyStorePassword, keyStoreType, newKeyStore, alias);
    }
    return newKeyStore;
  }

  /**
   * this method tries to access an entry of the given {@code keyStore} and will add it to the
   * {@code newKeyStore} object no matter if the given alias is a key-entry or a certificate entry
   *
   * @param keyStore the keystore that holds the original entry
   * @param keyStorePassword the password to access the original keystore
   * @param keyPassword the password to access the original key entry under the given alias
   * @param keyStoreType the type of the original keystore
   * @param newKeyStore the new keystore to which the entry should be copied
   * @param alias the alias of the entry that should be copied
   */
  public static void tryCopyEntry(KeyStore keyStore,
                                  String keyStorePassword,
                                  String keyPassword,
                                  KeyStoreType keyStoreType,
                                  KeyStore newKeyStore,
                                  String alias)
  {
    log.trace("adding key-entry of alias '{}' to new keystore of type '{}'", alias, keyStoreType.name());
    Optional<Certificate[]> certificateChainOptional = getCertificateChain(keyStore, alias);
    Optional<Certificate> certificateOptional = getCertificate(keyStore, alias);
    Optional<Key> key = getKeyEntry(keyStore, alias, keyPassword);
    if (key.isPresent() && certificateChainOptional.isPresent())
    {
      addEntryToKeystore(newKeyStore, alias, key.get(), keyStorePassword, certificateChainOptional.get());
    }
    else if (certificateOptional.isPresent())
    {
      Certificate certificate = certificateOptional.get();
      addCertificateEntryToKeyStore(newKeyStore, certificate, alias);
    }
    else
    {
      log.warn("could not find any entries to copy under the alias '{}'", alias);
    }
  }

  /**
   * Will store the given keystore into the given file.
   *
   * @param file the file where the keystore should be saved.
   * @param keyStore the keystore to save.
   * @param keystorePassword the password to access and save the given keystore
   */
  @SneakyThrows
  public static void keyStoreToFile(File file, KeyStore keyStore, String keystorePassword)
  {
    log.trace("creating file '{}' for keystore of type '{}'.", file.getAbsolutePath(), keyStore.getType());
    try (OutputStream outputStream = new FileOutputStream(file))
    {
      keyStore.store(outputStream, keystorePassword.toCharArray());
      log.trace("keystore was successfully saved in file '{}'", file.getAbsolutePath());
    }
  }

  /**
   * Will store the given keystore into the given file.
   *
   * @param directory the target directory where the keystore should be saved.
   * @param filename the file where the keystore should be saved.
   * @param keyStore the keystore to save.
   * @param keystorePassword the password to access and save the given keystore
   */
  public static void keyStoreToFile(File directory, String filename, KeyStore keyStore, String keystorePassword)
  {
    log.trace("creating file '{}/{}.{}' for keystore of type '{}'.",
              directory.getAbsolutePath(),
              filename,
              KeyStoreType.valueOf(keyStore.getType()).getFileExtension(),
              keyStore.getType());
    KeyStoreType keyStoreType = KeyStoreType.valueOf(keyStore.getType());
    File keyStoreFile = new File(directory.getAbsolutePath() + File.separator + filename + "."
                                 + keyStoreType.getFileExtension());
    keyStoreToFile(keyStoreFile, keyStore, keystorePassword);
  }

  /**
   * will read a file to a keystore.
   *
   * @param file the file that should be read to a keystore
   * @param keyStorePassword the password to access the keystore
   * @return the read keystore
   */
  public static KeyStore readKeyStore(File file, String keyStorePassword)
  {
    if (!file.exists())
    {
      throw new KeyStoreCreationFailedException("The file '" + file.getAbsolutePath() + "' does not exist!");
    }
    String[] fileParts = file.getName().split("\\.");
    String fileExtension = fileParts[fileParts.length - 1];
    // @formatter:off
    KeyStoreType keyStoreType = KeyStoreType.byFileExtension(fileExtension)
      .orElseThrow(() -> new KeyStoreCreationFailedException("could not determine the type of the keystore. A specific "
        + "file extension like jks, jceks, p12 or pfx is needed."));
    // @formatter:on
    return getKeyStoreFromFile(file, keyStorePassword, keyStoreType);
  }

  @SneakyThrows
  private static KeyStore getKeyStoreFromFile(File file, String keyStorePassword, KeyStoreType keyStoreType)
  {
    try (InputStream inputStream = new FileInputStream(file))
    {
      KeyStore keyStore = KeyStore.getInstance(keyStoreType.name(), selectProvider(keyStoreType));
      keyStore.load(inputStream, keyStorePassword.toCharArray());
      return keyStore;
    }
  }

  /**
   * will read a byte array to a keystore.
   *
   * @param keyStoreBytes the bytes of the keyStore that should be read
   * @param keyStoreType the type of the keystore.
   * @param keyStorePassword the password to access the keystore
   * @return the read keystore
   */
  @SneakyThrows
  public static KeyStore readKeyStore(byte[] keyStoreBytes, KeyStoreType keyStoreType, String keyStorePassword)
  {
    if (keyStoreBytes == null || keyStoreType == null || keyStorePassword == null)
    {
      throw new KeyStoreCreationFailedException("Cannot create a keystore if null values are given...");
    }
    try (InputStream inputStream = new ByteArrayInputStream(keyStoreBytes))
    {
      KeyStore keyStore = KeyStore.getInstance(keyStoreType.name(), selectProvider(keyStoreType));
      keyStore.load(inputStream, keyStorePassword.toCharArray());
      return keyStore;
    }
  }

  /**
   * will read an input stream to a keystore.
   *
   * @param keyStoreStream the bytes of the keyStore that should be read
   * @param keyStoreType the type of the keystore.
   * @param keyStorePassword the password to access the keystore
   * @return the read keystore
   */
  @SneakyThrows
  public static KeyStore readKeyStore(InputStream keyStoreStream, KeyStoreType keyStoreType, String keyStorePassword)
  {
    if (keyStoreStream == null || keyStoreType == null || keyStorePassword == null)
    {
      throw new KeyStoreCreationFailedException("Cannot create a keystore if null values are given...");
    }
    try (InputStream inputStream = keyStoreStream)
    {
      KeyStore keyStore = KeyStore.getInstance(keyStoreType.name(), selectProvider(keyStoreType));
      keyStore.load(inputStream, keyStorePassword.toCharArray());
      return keyStore;
    }
  }

  /**
   * will read a keystore from the given byte array that can only be used as truststore
   *
   * @param truststoreBytes the bytes of the truststore
   * @param keyStoreType the keystore type that the truststore represents
   * @return a keystore that can only be used as truststore
   */
  public static KeyStore readTruststore(byte[] truststoreBytes, KeyStoreType keyStoreType)
  {
    if (truststoreBytes == null)
    {
      throw new KeyStoreCreationFailedException("Cannot create a truststore if truststoreBytes is null");
    }
    return readTruststore(new ByteArrayInputStream(truststoreBytes), keyStoreType, null);
  }

  /**
   * will read a keystore from the given byte array that can only be used as truststore
   *
   * @param truststoreBytes the bytes of the truststore
   * @param keyStoreType the keystore type that the truststore represents
   * @param password an optional password that can be entered for JKS keystores and must be entered for PKCS12
   *          keystores
   * @return a keystore that can only be used as truststore
   */
  public static KeyStore readTruststore(byte[] truststoreBytes, KeyStoreType keyStoreType, String password)
  {
    if (truststoreBytes == null)
    {
      throw new KeyStoreCreationFailedException("Cannot create a truststore if truststoreBytes is null");
    }
    return readTruststore(new ByteArrayInputStream(truststoreBytes), keyStoreType, password);
  }

  /**
   * will read a keystore from the given inputstream that can only be used as truststore
   *
   * @param truststoreStream a stream containing the truststore data
   * @param keyStoreType the keystore type that the truststore represents
   * @return a keystore that can only be used as truststore
   */
  public static KeyStore readTruststore(InputStream truststoreStream, KeyStoreType keyStoreType)
  {
    return readTruststore(truststoreStream, keyStoreType, null);
  }

  /**
   * will read a keystore from the given inputstream that can only be used as truststore
   *
   * @param truststoreStream a stream containing the truststore data
   * @param keyStoreType the keystore type that the truststore represents
   * @param password an optional password that can be entered for JKS keystores and must be entered for PKCS12
   *          keystores
   * @return a keystore that can only be used as truststore
   */
  @SneakyThrows
  public static KeyStore readTruststore(InputStream truststoreStream, KeyStoreType keyStoreType, String password)
  {
    if (truststoreStream == null)
    {
      throw new KeyStoreCreationFailedException("Cannot create a truststore if truststore is null");
    }
    try (InputStream inputStream = truststoreStream)
    {
      KeyStore keyStore = KeyStore.getInstance(keyStoreType.name(), selectProvider(keyStoreType));
      String keystorePin = password;
      if (KeyStoreType.PKCS12.equals(keyStoreType) && password == null)
      {
        keystorePin = "";
      }
      keyStore.load(inputStream, keystorePin == null ? null : keystorePin.toCharArray());
      return keyStore;
    }
  }

  /**
   * this method will merge all accessible entries from the given keystores into a single keystore <br>
   * <b>WARNING:</b> <br>
   * It might be that keystore1 and 2 may contain different entries under the same alias. In order for these
   * both not to collide with one another the alias from keystore2 will be extended by "_2" <br>
   * <br>
   * If keystore 1 and 2 will share the same entry under different aliases the alias from keystore1 is preferred
   * unless the entry of keystore1 is accessible. Otherwise the entry of keystore2 will be added if it is
   * accessbile instead. <br>
   * <br>
   * If a private key entry cannot be accessed since its password is not matching the keystore password the
   * entry will be omitted and only be added as a certificate entry.
   *
   * @param keyStore1 the first keystore
   * @param password1 the password to access the first keystore
   * @param keyStore2 the second keystore
   * @param password2 the password to access the second keystore
   * @param keyStoreType this will be the type of the keystore that contains the new entries.
   * @param mergedKeyStoreKeyPassword this will be the password of all added private keys within the merged
   *          keystore.
   * @return a new keystore that contains all entries of the two keystores that were directly accessible.
   */
  public static KeyStore mergeKeyStores(KeyStore keyStore1,
                                        String password1,
                                        KeyStore keyStore2,
                                        String password2,
                                        KeyStoreType keyStoreType,
                                        String mergedKeyStoreKeyPassword)
  {
    log.trace("trying to merge the following keystores {}-{} and {}-{}",
              keyStore1.getType(),
              keyStore1,
              keyStore2.getType(),
              keyStore2);
    KeyStore mergedKeyStore = createEmptyKeyStore(keyStoreType, null);
    // this list will save all used aliases. It might be that keystore1 and 2 may contain different entries
    // under the same alias. In order for these both not to collide with one another the alias from keystore2
    // will be extended by "_2"
    List<String> aliasMap = new ArrayList<>();

    Enumeration<String> aliases1 = getAliases(keyStore1);
    log.trace("adding the entries of keystore1 '{}'", keyStore1);
    while (aliases1.hasMoreElements())
    {
      String alias = aliases1.nextElement();
      aliasMap.add(alias);
      tryCopyEntry(keyStore1, mergedKeyStoreKeyPassword, password1, keyStoreType, mergedKeyStore, alias);
    }

    Enumeration<String> aliases2 = getAliases(keyStore2);
    log.trace("adding the entries of keystore2 '{}'", keyStore2);
    while (aliases2.hasMoreElements())
    {
      String alias = aliases2.nextElement();

      Optional<Key> key = getKeyEntry(keyStore2, alias, password2);

      if (key.isPresent())
      {
        getCertificateChain(keyStore2, alias).ifPresent(certificates -> addEntryToKeystore(mergedKeyStore,
                                                                                           alias,
                                                                                           key.get(),
                                                                                           certificates,
                                                                                           password2));
      }
      else
      {
        Optional<Certificate> certificate = getCertificate(keyStore2, alias);
        certificate.ifPresent(cert -> addCertificateEntryToKeyStore(mergedKeyStore, cert, alias));
      }
    }
    return mergedKeyStore;
  }

  /**
   * reads the first found keystore entry and expects it to be a private-key entry. This method can be used if
   * the alias of the keystore is unknown and the given keystore contains only a single private-key-entry
   *
   * @param keyStore the keystore with hopefully only a single private key entry
   * @param privateKeyPassword the password of the private key
   * @return the keypair of the keystore. (Since empty keystores are invalid this method should never return
   *         null)
   * @throws KeyStoreReadingException if the keystore entry could not be read or if the first keystore entry is
   *           only a certificate entry
   */
  @SneakyThrows
  public static KeyPair readFirstKeyPairEntryFromKeyStore(KeyStore keyStore, String privateKeyPassword)
  {
    Enumeration<String> aliases = getAliases(keyStore);
    KeyPair keyPair = null;
    while (aliases.hasMoreElements())
    {
      String alias = aliases.nextElement();
      PrivateKey privateKey = (PrivateKey)keyStore.getKey(alias, privateKeyPassword.toCharArray());
      PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
      keyPair = new KeyPair(publicKey, privateKey);
      break;
    }
    return keyPair;
  }

  /**
   * convenience method to access the aliases of the keystore without having to handle the exception
   *
   * @param keyStore the keystore to get the aliases from
   * @return the aliases of the given keystore
   * @throws KeyStoreReadingException in case of a {@link KeyStoreException}
   */
  @SneakyThrows
  public static Enumeration<String> getAliases(KeyStore keyStore)
  {
    return keyStore.aliases();
  }

  /**
   * convenience method to access the private key from the keystore without having to handle the checked
   * exceptions
   *
   * @param keyStore the keystore from which the private key should be accessed
   * @param alias the alias of the private key entry
   * @param password the password of the private key entry for the given alias
   * @return the private key entry if it does exist
   */
  @SneakyThrows
  public static Optional<Key> getKeyEntry(KeyStore keyStore, String alias, String password)
  {
    try
    {
      return Optional.ofNullable(keyStore.getKey(alias,
                                                 Optional.ofNullable(password).map(String::toCharArray).orElse(null)));
    }
    catch (UnrecoverableKeyException e)
    {
      Throwable root = ExceptionUtils.getRootCause(e);
      String rootMessage = root == null ? e.getMessage() : root.getMessage();
      log.debug("could not recover key: {}", rootMessage);
      return Optional.empty();
    }
  }

  /**
   * will get the certificateChain from the given alias
   *
   * @param keyStore the keystore from which the certificate chain should be extracted
   * @param alias the alias where the chain should be found
   * @return the certificate chain if present or an empty
   */
  @SneakyThrows
  public static Optional<Certificate[]> getCertificateChain(KeyStore keyStore, String alias)
  {
    return Optional.ofNullable(keyStore.getCertificateChain(alias));
  }

  /**
   * convenience method to read a certificate entry from a keystore
   */
  @SneakyThrows
  public static Optional<Certificate> getCertificate(KeyStore keyStore, String alias)
  {
    return Optional.ofNullable(keyStore.getCertificate(alias));
  }

  /**
   * represents the possible keystore types that are supported
   */
  public enum KeyStoreType
  {

    JKS("jks"), JCEKS("jceks"), PKCS12("p12");

    /**
     * if a keystore is going to be saved this is where to get the fileextension
     */
    private String fileExtension;

    KeyStoreType(String fileExtension)
    {
      this.fileExtension = fileExtension;
    }

    /**
     * tries to find the correct keystore type by a file extension
     *
     * @param fileExtension the file extension.
     * @return the corresponding keystore type if some fits.
     */
    public static Optional<KeyStoreType> byFileExtension(String fileExtension)
    {
      if (StringUtils.isBlank(fileExtension))
      {
        return Optional.empty();
      }
      // in case the dot before the extension is given too
      for ( KeyStoreType keyStoreType : values() )
      {
        if (fileExtension.toLowerCase(Locale.ENGLISH).endsWith(keyStoreType.getFileExtension()))
        {
          return Optional.of(keyStoreType);
        }
      }
      // if the keystore type was not found by now ask if the given keystore has the other pkcs12 ending pfx
      if (fileExtension.toLowerCase(Locale.ENGLISH).endsWith("pfx"))
      {
        return Optional.of(PKCS12);
      }
      return Optional.empty();
    }

    /**
     * @see #fileExtension
     */
    public String getFileExtension()
    {
      return fileExtension;
    }
  }
}
