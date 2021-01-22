package de.captaingoldfish.scim.sdk.client.keys;

import java.security.Provider;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


/**
 * author: Pascal Knueppel <br>
 * created at: 09.12.2019 - 14:55 <br>
 * <br>
 * this class will provide some security providers
 */
public class SecurityProvider
{

  /**
   * the bouncy castle provider that is needed for PKCS12 keystores
   */
  public static final Provider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();

}
