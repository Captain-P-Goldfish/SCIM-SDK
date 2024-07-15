package de.captaingoldfish.scim.sdk.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import lombok.experimental.UtilityClass;


/**
 * @author Pascal Knueppel
 * @since 14.07.2024
 */
@UtilityClass
public class RequestUtils
{

  /**
   * parses the given attributes into an array of strings
   *
   * @param attributes the attributes request parameter that is expected to be a comma separated string
   * @return the array of the separated attribute names or an empty
   */
  public static List<String> getAttributeList(String attributes)
  {
    if (StringUtils.isBlank(attributes))
    {
      return Collections.emptyList();
    }
    if (!attributes.matches("(^[a-zA-Z0-9$]([:a-zA-Z0-9.,$]+)?[a-zA-Z0-9$]$)*"))
    {
      String errorMessage = "the attributes or excludedAttributes parameter '" + attributes + "' is malformed please "
                            + "check your syntax and please note that whitespaces are not allowed.";
      throw new BadRequestException(errorMessage, null, null);
    }
    return new ArrayList<>(Arrays.asList(attributes.split(",")));
  }

}
