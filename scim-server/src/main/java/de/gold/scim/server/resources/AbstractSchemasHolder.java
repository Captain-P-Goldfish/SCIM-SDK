package de.gold.scim.server.resources;

import java.util.List;

import de.gold.scim.server.constants.AttributeNames;
import de.gold.scim.server.resources.base.ScimObjectNode;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 18:52 <br>
 * <br>
 * an abstract implementation that simply adds methods for adding and getting the "schemas"-attribute
 */
public abstract class AbstractSchemasHolder extends ScimObjectNode
{

  public AbstractSchemasHolder()
  {
    super(null);
  }

  /**
   * @return the list of schemas witin this resource
   */
  public List<String> getSchemas()
  {
    return getSimpleArrayAttribute(AttributeNames.RFC7643.SCHEMAS);
  }

  /**
   * adds a list of schemas to this resource
   */
  public void setSchemas(List<String> schemas)
  {
    setStringAttributeList(AttributeNames.RFC7643.SCHEMAS, schemas);
  }
}
