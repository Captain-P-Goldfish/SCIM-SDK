package de.captaingoldfish.scim.sdk.server.utils;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.StringUtils;

import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;


/**
 * author Pascal Knueppel <br>
 * created at: 13.10.2019 - 15:43 <br>
 * <br>
 */
public class RequestUtilsTest
{

  /**
   * will verify that no exception is thrown if only one of the parameters are given
   */
  @ParameterizedTest
  @CsvSource({"nickName,", ",nickName"})
  public void testGiveAttributesAndExcludedAttributes(String attributes, String excludedAttributes)
  {
    Assertions.assertDoesNotThrow(() -> RequestUtils.validateAttributesAndExcludedAttributes(attributes,
                                                                                             excludedAttributes));
  }

  /**
   * will verify that a {@link BadRequestException} is thrown if the parameters are * given both
   */
  @Test
  public void testGiveAttributesAndExcludedAttributes()
  {
    final String attributes = "nickName";
    final String excludedAttributes = "userName";
    Assertions.assertThrows(BadRequestException.class,
                            () -> RequestUtils.validateAttributesAndExcludedAttributes(attributes, excludedAttributes));
  }

  /**
   * will verify that none of the given string combinations will throw an exception and that the parameters are
   * correctly returned
   *
   * @param attributes the attribute string
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "id", "externalId", "name", "userName", "userName,nickName",
                          "id,nickName,urn:ietf:params:scim:schemas:core:2.0:User:name,userName,name",
                          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager"})
  public void testParseAttributes(String attributes)
  {
    List<String> attributeList = Assertions.assertDoesNotThrow(() -> RequestUtils.getAttributes(attributes));
    if (StringUtils.isBlank(attributes))
    {
      MatcherAssert.assertThat(attributeList, Matchers.empty());
    }
    else
    {
      MatcherAssert.assertThat(attributeList, Matchers.hasSize(attributes.split(",").length));
    }
  }

  /**
   * will verify that malformed attribute strings will cause a {@link BadRequestException}
   *
   * @param attributes the attribute string
   */
  @ParameterizedTest
  @ValueSource(strings = {".", ":", ",", ",userName", "userName,", "user_name", "userName,nickName,", ":username",
                          "userName:"})
  public void testParseAttributesFail(String attributes)
  {
    Assertions.assertThrows(BadRequestException.class, () -> RequestUtils.getAttributes(attributes));
  }

  @ParameterizedTest
  @ValueSource(strings = {"p1=v1", "p1=v1&p2=v2", "&p1=v1&&p2=v2&", "p1=&p2=", "p1&p2", "=v1&=v2"})
  public void testGetQueryParameters(String query)
  {
    Assertions.assertDoesNotThrow(() -> RequestUtils.getQueryParameters(query));
  }

}
