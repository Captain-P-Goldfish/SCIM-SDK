package de.gold.scim.filter.antlr;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.StringUtils;

import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 17:18 <br>
 * <br>
 */
@Slf4j
public class FilterAttributeNameTest
{

  /**
   * verifies that the {@link FilterAttributeName} constructor with the string parameter will resolve the given
   * attribute names correctly
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "userName", "emails", "name.givenName", "urn:ietf:params:scim:schemas:core:2.0:User:name",
                          "urn:ietf:params:scim:schemas:core:2.0:User:name.givenName"})

  public void testCreateAttributeNameWithStringConstructor(String attributeNameString)
  {
    FilterAttributeName attributeName = Assertions.assertDoesNotThrow(() -> new FilterAttributeName(attributeNameString));
    MatcherAssert.assertThat(attributeName.getFullName(), Matchers.endsWith(attributeNameString));
    if (StringUtils.isNotBlank(attributeNameString) && attributeNameString.contains(":"))
    {
      Assertions.assertEquals(attributeNameString, attributeName.getFullName());
      MatcherAssert.assertThat(attributeNameString, Matchers.endsWith(attributeName.getShortName()));
      MatcherAssert.assertThat(attributeNameString, Matchers.startsWith(attributeName.getResourceUri()));
    }
    else if (StringUtils.isNotBlank(attributeNameString))
    {
      Assertions.assertEquals(attributeNameString, attributeName.getShortName());
      Assertions.assertNull(attributeName.getResourceUri());
      Assertions.assertEquals(attributeNameString, attributeName.getFullName());
    }
    else
    {
      Assertions.assertNull(attributeName.getShortName());
      Assertions.assertNull(attributeName.getResourceUri());
      MatcherAssert.assertThat(attributeName.getFullName(), Matchers.isEmptyString());
    }
  }
}
