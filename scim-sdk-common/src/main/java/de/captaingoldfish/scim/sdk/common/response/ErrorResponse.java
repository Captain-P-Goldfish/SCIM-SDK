package de.captaingoldfish.scim.sdk.common.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResponseException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 14.10.2019 - 20:58 <br>
 * <br>
 * represents a SCIM error response
 */
@Slf4j
@NoArgsConstructor
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
    this(scimException, false);
  }

  public ErrorResponse(ScimException scimException, boolean useDetailMessage)
  {
    super(null);
    this.scimException = scimException;
    if (HttpStatus.INTERNAL_SERVER_ERROR == getHttpStatus() && !useDetailMessage)
    {
      log.error(scimException.getMessage(), scimException);
      setDetail("An internal error has occurred.");
    }
    else
    {
      log.debug(scimException.getMessage(), scimException);
      setDetail(scimException.getDetail());
    }
    setSchemas(Collections.singletonList(SchemaUris.ERROR_URI));
    setStatus(scimException.getStatus());
    setScimType(scimException.getScimType());
    Optional.ofNullable(scimException.getResponseHeaders()).ifPresent(getHttpHeaders()::putAll);
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
   * @return the unspecific error messages from the schema validation that could not be mapped to a specific
   *         field
   */
  public List<String> getErrorMessages()
  {
    ObjectNode errors = (ObjectNode)this.get(AttributeNames.Custom.ERRORS);
    if (errors == null)
    {
      return Collections.emptyList();
    }
    ArrayNode errorMessages = Optional.ofNullable((ArrayNode)errors.get(AttributeNames.Custom.ERROR_MESSAGES))
                                      .orElse(new ArrayNode(JsonNodeFactory.instance));
    if (errorMessages.isEmpty())
    {
      return Collections.emptyList();
    }
    List<String> errorMessageList = new ArrayList<>();
    errorMessages.forEach(node -> errorMessageList.add(node.textValue()));
    return errorMessageList;
  }

  /**
   * @return the field errors from the schema validation that indicate errors directly on specific resource
   *         fields
   */
  public Map<String, List<String>> getFieldErrors()
  {
    ObjectNode errors = (ObjectNode)this.get(AttributeNames.Custom.ERRORS);
    if (errors == null)
    {
      return new HashMap<>();
    }
    ObjectNode fieldErrors = Optional.ofNullable((ObjectNode)errors.get(AttributeNames.Custom.FIELD_ERRORS))
                                     .orElse(new ObjectNode(JsonNodeFactory.instance));
    if (fieldErrors.isEmpty())
    {
      return new HashMap<>();
    }
    Map<String, List<String>> errorMessageList = new HashMap<>();
    fieldErrors.fields().forEachRemaining(nodeDefinition -> {
      List<String> errorMessages = new ArrayList<>();
      nodeDefinition.getValue().forEach(node -> errorMessages.add(node.textValue()));
      errorMessageList.put(nodeDefinition.getKey(), errorMessages);
    });
    return errorMessageList;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return scimException == null ? 0 : scimException.getStatus();
  }

  /**
   * this method will tell us if this error response is actually an error. In cases in which an exception was
   * thrown but the status code to return is less than 400 and no details are given the response body should be
   * empty
   *
   * @return true if the response body should be empty, false else
   */
  private boolean useEmptyBody()
  {
    return getHttpStatus() < HttpStatus.BAD_REQUEST && StringUtils.isBlank(getDetail().orElse(null))
           && StringUtils.isBlank(getScimType().orElse(null));
  }

  @Override
  public String toString()
  {
    if (useEmptyBody())
    {
      return null;
    }
    return super.toString();
  }

  @Override
  public String toPrettyString()
  {
    if (useEmptyBody())
    {
      return null;
    }
    return super.toPrettyString();
  }
}
