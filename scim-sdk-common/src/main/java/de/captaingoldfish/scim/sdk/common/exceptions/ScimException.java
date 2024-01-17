package de.captaingoldfish.scim.sdk.common.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import lombok.Getter;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 15:19 <br>
 * <br>
 * the base-exception of all scim errors
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

  /**
   * a map of http headers that should be included in the response
   */
  protected Map<String, String> responseHeaders;

  public ScimException(String message, Throwable cause, Integer status, String scimType)
  {
    this(message, cause, status, scimType, new HashMap<>());
  }

  public ScimException(String message,
                       Throwable cause,
                       Integer status,
                       String scimType,
                       Map<String, String> responseHeaders)
  {
    super(message, cause);
    this.status = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    this.scimType = StringUtils.isBlank(scimType) ? null : scimType;
    this.responseHeaders = responseHeaders;
  }

  public String getDetail()
  {
    return getMessage();
  }
}
