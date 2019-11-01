package de.gold.scim.common.exceptions;

import org.apache.commons.lang3.StringUtils;

import de.gold.scim.common.constants.HttpStatus;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 15:19 <br>
 * <br>
 * the base exception of all scim errors
 */
@Getter
@Setter
public abstract class ScimException extends RuntimeException
{

  /**
   * The HTTP status code.
   */
  protected int status;

  /**
   * the scim type for scim errors as defined in RFC7644 3.12.
   */
  protected String scimType;

  public ScimException(String message, Throwable cause, Integer status, String scimType)
  {
    super(message, cause);
    this.status = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    this.scimType = StringUtils.isBlank(scimType) ? null : scimType;
  }

  public String getDetail()
  {
    return getMessage();
  }
}
