package de.captaingoldfish.scim.sdk.keycloak.themes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.SneakyThrows;


/**
 * @author Pascal Knueppel
 * @since 18.12.2020
 */
public class ThemeTest
{

  /**
   * this unit test is explicitly added for updating to new keycloak versions. It will inform me as soon as
   * there are changes in the kc-menu.html file
   */
  @SneakyThrows
  @Test
  public void testKcMenuIsUnchanged()
  {
    final String hashOfKcMenu = "7d25fcf3ae28ec0973ef7c5e5890d1f4d52c68d2";
    MessageDigest sha1DigestBuilder = MessageDigest.getInstance("SHA-1");
    final String kcMenuLocation = "/theme/base/admin/resources/templates/kc-menu.html";
    byte[] kcMenuBytes = IOUtils.toByteArray(getClass().getResourceAsStream(kcMenuLocation));
    byte[] kcMenuDigest = Hex.encode(sha1DigestBuilder.digest(kcMenuBytes));
    Assertions.assertArrayEquals(hashOfKcMenu.getBytes(StandardCharsets.UTF_8),
                                 kcMenuDigest,
                                 String.format("expected hash: '%s'\nactual hash: '%s'",
                                               hashOfKcMenu,
                                               new String(kcMenuDigest)));
  }
}
