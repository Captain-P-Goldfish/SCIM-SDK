package de.gold.scim.response;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.HttpStatus;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.exceptions.ScimException;
import de.gold.scim.utils.JsonHelper;
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
   * this is the json representation that is used as error response
   */
  private final JsonNode errorNode;

  /**
   * the exception that should be turned into a SCIM error response
   */
  @Getter
  private ScimException scimException;

  public ErrorResponse(ScimException scimException)
  {
    this.errorNode = new ObjectNode(JsonNodeFactory.instance);
    this.scimException = scimException;
    if (HttpStatus.SC_INTERNAL_SERVER_ERROR == getHttpStatus())
    {
      log.error(scimException.getMessage(), scimException);
    }
    else
    {
      log.debug(scimException.getMessage(), scimException);
    }
    writeValuesToErrorNode();
  }

  /**
   * adds the attributes into the error node representation
   */
  protected void writeValuesToErrorNode()
  {
    ArrayNode schemas = new ArrayNode(JsonNodeFactory.instance);
    schemas.add(SchemaUris.ERROR_URI);
    JsonHelper.addAttribute(this.errorNode, AttributeNames.RFC7643.SCHEMAS, schemas);
    JsonHelper.addAttribute(this.errorNode, AttributeNames.RFC7643.STATUS, new IntNode(scimException.getStatus()));
    if (StringUtils.isNotBlank(scimException.getDetail()))
    {
      JsonHelper.addAttribute(this.errorNode, AttributeNames.RFC7643.DETAIL, new TextNode(scimException.getDetail()));
    }
    if (StringUtils.isNotBlank(scimException.getScimType()))
    {
      JsonHelper.addAttribute(this.errorNode,
                              AttributeNames.RFC7643.SCIM_TYPE,
                              new TextNode(scimException.getScimType()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHttpStatus()
  {
    return scimException.getStatus();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toJsonDocument()
  {
    return errorNode.toString();
  }
}
