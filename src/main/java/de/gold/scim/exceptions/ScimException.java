package de.gold.scim.exceptions;

import org.apache.commons.lang3.StringUtils;

import de.gold.scim.utils.HttpStatus;
import lombok.Data;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 15:19 <br>
 * <br>
 * the base exception of all scim errors
 */
@Data
public abstract class ScimException extends RuntimeException
{

  /**
   * A detailed human-readable message..
   */
  protected String detail;

  /**
   * The HTTP status code.
   */
  protected int status;

  /**
   * the scim type for scim errors as defined in RFC7644 3.12.
   */
  protected String scimType;

  public ScimException(String message, Throwable cause, String detail, Integer status, String scimType)
  {
    super(message, cause);
    this.detail = StringUtils.isBlank(detail) ? "" : detail;
    this.status = status == null ? HttpStatus.SC_INTERNAL_SERVER_ERROR : status;
    this.scimType = StringUtils.isBlank(scimType) ? null : scimType;
  }
}
