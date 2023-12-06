package de.captaingoldfish.scim.sdk.server.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


/**
 * author Pascal Knueppel <br>
 * created at: 13.10.2019 - 15:43 <br>
 * <br>
 */
public class RequestUtilsTest
{

  @ParameterizedTest
  @ValueSource(strings = {"p1=v1", "p1=v1&p2=v2", "&p1=v1&&p2=v2&", "p1=&p2=", "p1&p2", "=v1&=v2"})
  public void testGetQueryParameters(String query)
  {
    Assertions.assertDoesNotThrow(() -> RequestUtils.getQueryParameters(query));
  }

}
