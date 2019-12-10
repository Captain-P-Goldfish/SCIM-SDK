package de.captaingoldfish.scim.sdk.client.http;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 09:15 <br>
 * <br>
 */
public class ParameterBuilderTest
{

  /**
   * verifies that the parameters are correctly added
   */
  @Test
  public void testBuildMapIsEmpty()
  {
    Map<String, String> parameterMap = ParameterBuilder.builder().build();
    Assertions.assertNotNull(parameterMap);
    MatcherAssert.assertThat(parameterMap, Matchers.anEmptyMap());
  }

  /**
   * verifies that the map is setup correctly
   */
  @Test
  public void testBuildMap()
  {
    Map<String, String> baseMap = new HashMap<>();
    baseMap.put("1", "1");
    baseMap.put("2", "2");
    baseMap.put("3", "3");
    baseMap.put("4", "4");

    ParameterBuilder parameterBuilder = ParameterBuilder.builder();
    baseMap.forEach(parameterBuilder::addParameter);

    Map<String, String> parameterMap = parameterBuilder.build();
    Assertions.assertNotNull(parameterMap);
    Assertions.assertEquals(baseMap, parameterMap);
  }
}
