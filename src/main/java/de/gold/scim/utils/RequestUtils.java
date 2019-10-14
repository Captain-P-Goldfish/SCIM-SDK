package de.gold.scim.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.BadRequestException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 12.10.2019 - 20:08 <br>
 * <br>
 * this class will add some helper methods that can be used to validate or modify request based attributes
 * based on the SCIM specification RFC7643 and RFC7644
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestUtils
{

  /**
   * this method will parse either the attributes parameter or the excludedAttributes parameter into a list. The
   * expected form of the attributes list is: form (e.g., userName, name, emails)
   *
   * @param attributes the comma separated string of scim attribute names
   * @return the list of attributes
   */
  public static List<String> getAttributes(String attributes)
  {
    if (StringUtils.isBlank(attributes))
    {
      return Collections.emptyList();
    }
    if (!attributes.matches("(^[a-zA-Z0-9][:a-zA-Z0-9.,]+[a-zA-Z0-9]$)*"))
    {
      String errorMessage = "the attributes or excludedAttributes parameter '" + attributes + "' is malformed please "
                            + "check your syntax and please note that whitespaces are not allowed.";
      throw new BadRequestException(errorMessage, null, null);
    }
    String[] attributeNameArray = attributes.split(",");
    return Arrays.asList(attributeNameArray);
  }

  /**
   * From RFC7644 chapter 3.9:<br>
   *
   * <pre>
   *     Clients MAY request a partial resource representation on any
   *     operation that returns a resource within the response by specifying
   *     either of the mutually exclusive URL query parameters "attributes" or
   *     "excludedAttributes"
   * </pre>
   *
   * so only one these parameters are allowed to be specified in a request
   *
   * @param attributes the required attributes that should be present in the response
   * @param excludedAttributes the attributes that should not be returned in the response
   */
  public static void validateAttributesAndExcludedAttributes(String attributes, String excludedAttributes)
  {
    if (StringUtils.isNotBlank(attributes) && StringUtils.isNotBlank(excludedAttributes))
    {
      final String errorMessage = "the attributes and excludedAttributes parameter must not be set at the same time:"
                                  + "\n\tattributes: '" + attributes + "'\n\texcludedAttributes: '" + excludedAttributes
                                  + "'";
      throw new BadRequestException(errorMessage, null, ScimType.INVALID_PARAMETERS);
    }
  }

}
