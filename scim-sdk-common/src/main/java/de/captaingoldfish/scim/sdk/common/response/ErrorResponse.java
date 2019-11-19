package de.captaingoldfish.scim.sdk.common.response;

import java.util.Collections;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResponseException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 20:58 <br>
 * <br>
 * represents a SCIM error response
 */
@Slf4j
public class ErrorResponse extends ScimResponse
{

  /**
   * the exception that should be turned into a SCIM error response
   */
  @Getter
  private ScimException scimException;

  public ErrorResponse(JsonNode responseNode)
  {
    super(responseNode);
    setSchemas(Collections.singletonList(SchemaUris.ERROR_URI));
    this.scimException = new ResponseException(getDetail().orElse(null), getStatus(), getScimType().orElse(null));
  }

  public ErrorResponse(ScimException scimException)
  {
    super(null);
    this.scimException = scimException;
    if (HttpStatus.INTERNAL_SERVER_ERROR == getHttpStatus())
    {
      log.error(scimException.getMessage(), scimException);
    }
    else
    {
      log.debug(scimException.getMessage(), scimException);
    }
    setSchemas(Collections.singletonList(SchemaUris.ERROR_URI));
    setStatus(scimException.getStatus());
    setDetail(scimException.getDetail());
    setScimType(scimException.getScimType());
  }

  /**
   * The HTTP status code (see Section 6 of [RFC7231]) expressed as a JSON string. REQUIRED.
   */
  public int getStatus()
  {
    return getIntegerAttribute(AttributeNames.RFC7643.STATUS).orElseThrow(() -> {
      return new InternalServerException("the http 'status' is a mandatory attribute", null, null);
    });
  }

  /**
   * The HTTP status code (see Section 6 of [RFC7231]) expressed as a JSON string. REQUIRED.
   */
  public void setStatus(int status)
  {
    setAttribute(AttributeNames.RFC7643.STATUS, status);
  }

  /**
   * A SCIM detail error keyword. See Table 9. OPTIONAL.
   */
  public Optional<String> getScimType()
  {
    return getStringAttribute(AttributeNames.RFC7643.SCIM_TYPE);
  }

  /**
   * A SCIM detail error keyword. See Table 9. OPTIONAL.
   */
  public void setScimType(String scimType)
  {
    setAttribute(AttributeNames.RFC7643.SCIM_TYPE, scimType);
  }

  /**
   * A detailed human-readable message. OPTIONAL.
   */
  public Optional<String> getDetail()
  {
    return getStringAttribute(AttributeNames.RFC7643.DETAIL);
  }

  /**
   * A detailed human-readable message. OPTIONAL.
   */
  public void setDetail(String detail)
  {
    setAttribute(AttributeNames.RFC7643.DETAIL, detail);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return scimException.getStatus();
  }
}
