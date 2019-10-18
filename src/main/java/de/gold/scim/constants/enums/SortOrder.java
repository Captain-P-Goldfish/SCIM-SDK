package de.gold.scim.constants.enums;

import org.apache.commons.lang3.StringUtils;

import de.gold.scim.constants.ScimType;
import de.gold.scim.exceptions.BadRequestException;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 17:35 <br>
 * <br>
 * gives the valid types for sort ordering
 */
public enum SortOrder
{

  ASCENDING, DESCENDING;

  /**
   * tries to get the correct sortOrder value by the given value
   *
   * @param sortOrder the value that should be match to one of the defined enums
   * @return null or the sortOrder value
   * @throws BadRequestException if the sortOrder value is not null and does not match to one of the defined
   *           enums
   */
  public static SortOrder getByValue(String sortOrder)
  {
    if (StringUtils.isBlank(sortOrder))
    {
      return null;
    }
    try
    {
      return valueOf(sortOrder.toUpperCase());
    }
    catch (IllegalArgumentException ex)
    {
      throw new BadRequestException("sortOrdering value '" + sortOrder + "' cannot be parsed to a valid value", ex,
                                    ScimType.INVALID_PARAMETERS);
    }
  }
}
