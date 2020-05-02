package de.captaingoldfish.scim.sdk.sample.common;

import java.util.Collections;
import java.util.List;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;


/**
 * <br>
 * <br>
 * created at: 02.05.2020
 *
 * @author Pascal Knüppel
 */
public class ScimKeystore extends ResourceNode
{

  /**
   * the URI set in the keystore schema definition under the "id" attribute
   */
  private static final String KEYSTORE_URI = "urn:custom:params:scim:schemas:mycompany:2.0:Keystore";

  public ScimKeystore()
  {
    setSchemas(Collections.singleton(KEYSTORE_URI));
  }

  public String getName()
  {
    return getStringAttribute(FieldNames.NAME).orElse(null);
  }

  public void setName(String name)
  {
    setAttribute(FieldNames.NAME, name);
  }

  public String getKeystorePassword()
  {
    return getStringAttribute(FieldNames.KEYSTORE_PASSWORD).orElse(null);
  }

  public void setKeystorePassword(String keystorePassword)
  {
    setAttribute(FieldNames.KEYSTORE_PASSWORD, keystorePassword);
  }

  public KeystoreType getKeystoreType()
  {
    // there will never be an exception here because the api will make sure that only legal values are present so
    // the optional will also never return null here because it is a required type
    return KeystoreType.valueOf(getStringAttribute(FieldNames.KEYSTORE_TYPE).map(String::toUpperCase).orElse(null));
  }

  public void setKeystoreType(KeystoreType keystoreType)
  {
    setAttribute(FieldNames.KEYSTORE_TYPE, keystoreType.name());
  }

  public KeystoreEntries getKeystoreEntries()
  {
    return getObjectAttribute(FieldNames.ENTRIES, KeystoreEntries.class).orElse(null);
  }

  public void setKeystoreEntries(KeystoreEntries keystoreEntries)
  {
    setAttribute(FieldNames.ENTRIES, keystoreEntries);
  }

  public static class KeystoreEntries extends ScimObjectNode
  {

    public String getAlias()
    {
      return getStringAttribute(FieldNames.ALIAS).orElse(null);
    }

    public void setAlias(String alias)
    {
      setAttribute(FieldNames.ALIAS, alias);
    }

    public String getKeyPassword()
    {
      return getStringAttribute(FieldNames.KEY_PASSWORD).orElse(null);
    }

    public void setKeyPassword(String keyPassword)
    {
      setAttribute(FieldNames.KEY_PASSWORD, keyPassword);
    }

    public List<String> getCertificateChain()
    {
      return getSimpleArrayAttribute(FieldNames.CERTIFICATE_CHAIN);
    }

    public void setCertificateChain(List<String> certificateChain)
    {
      setStringAttributeList(FieldNames.CERTIFICATE_CHAIN, certificateChain);
    }

    public String getPrivateKey()
    {
      return getStringAttribute(FieldNames.PRIVATE_KEY).orElse(null);
    }

    public void setPrivateKey(String privateKey)
    {
      setAttribute(FieldNames.PRIVATE_KEY, privateKey);
    }
  }

  /**
   * contains the field names that are defined in the keystore schema definition
   */
  private static final class FieldNames
  {

    private static final String NAME = "name";

    private static final String KEYSTORE_PASSWORD = "keystorePassword";

    private static final String KEYSTORE_TYPE = "keystoreType";

    private static final String ENTRIES = "entries";

    private static final String ALIAS = "alias";

    private static final String KEY_PASSWORD = "keyPassword";

    private static final String CERTIFICATE_CHAIN = "certificateChain";

    private static final String PRIVATE_KEY = "privateKey";

  }
}
