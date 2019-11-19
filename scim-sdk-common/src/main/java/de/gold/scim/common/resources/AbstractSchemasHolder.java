package de.gold.scim.common.resources;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.gold.scim.common.constants.AttributeNames;
import de.gold.scim.common.resources.base.ScimObjectNode;


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
  public Set<String> getSchemas()
  {
    return getSimpleArrayAttributeSet(AttributeNames.RFC7643.SCHEMAS);
  }

  /**
   * adds a set of schemas to this resource
   */
  public void setSchemas(Set<String> schemas)
  {
    setStringAttributeList(AttributeNames.RFC7643.SCHEMAS, schemas);
  }

  /**
   * adds a list of schemas to this resource
   */
  public void setSchemas(List<String> schemas)
  {
    setSchemas(new HashSet<>(schemas));
  }

  /**
   * adds a single schema to this resource node
   *
   * @param schemaUri the uri to add
   */
  public void addSchema(String schemaUri)
  {
    Set<String> schemas = getSchemas();
    if (!schemas.contains(schemaUri))
    {
      schemas.add(schemaUri);
      setSchemas(schemas);
    }
  }

  /**
   * removes a single schema from this resource node
   *
   * @param schemaUri the uri to add
   */
  public void removeSchema(String schemaUri)
  {
    Set<String> schemas = getSchemas();
    schemas.remove(schemaUri);
    setSchemas(schemas);
  }
}
