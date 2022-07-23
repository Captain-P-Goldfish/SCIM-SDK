package de.captaingoldfish.scim.sdk.common.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * author Pascal Knueppel <br>
 * created at: 23.07.2022 - 13:22 <br>
 * <br>
 * used to extract an attribute from a json document case-insensitive
 */
public class CaseInsensitiveAttributeExtractor extends AttributeExtractor
{

  /**
   * this attribute map stores all found attributes from the json document if once extracted to prevent
   * unnecessary redundant iterations
   */
  private final Map<String, String> attributeMap = new HashMap<>();

  public CaseInsensitiveAttributeExtractor(JsonNode jsonDocument)
  {
    super(jsonDocument);
  }

  /**
   * will try to extract an attribute from the json document case-insensitive:<br>
   * <ol>
   * <li>try to extract it by its correct attribute name from the json document</li>
   * <li>if not found try to extract it from the {@link #attributeMap}</li>
   * <li>if not found iterate over the document and store each found element with its key value in lowercase in
   * the {@link #attributeMap} until found</li>
   * </ol>
   * 
   * @param schemaAttribute the attribute that should be extracted from the document
   * @return the attribute or an empty if not found
   */
  public Optional<JsonNode> getAttribute(SchemaAttribute schemaAttribute)
  {
    // 1. step
    {
      JsonNode attribute = jsonDocument.get(schemaAttribute.getName());
      if (attribute != null)
      {
        // also added for a later equality-check in size of attributes
        attributeMap.put(schemaAttribute.getName().toLowerCase(), schemaAttribute.getName());
        return Optional.of(attribute);
      }
    }

    // 2. step
    {
      Optional<JsonNode> attribute = extractAttributeByAttributeMap(schemaAttribute);
      if (attribute.isPresent())
      {
        return attribute;
      }
    }

    // if an iteration over all attributes within the json document was already executed
    if (attributeMap.size() == jsonDocument.size())
    {
      return Optional.empty();
    }

    // step 3
    {
      Iterator<String> attributeNameIterator = jsonDocument.fieldNames();
      while (attributeNameIterator.hasNext())
      {
        String attributeName = attributeNameIterator.next();
        attributeMap.put(attributeName.toLowerCase(), attributeName);
      }
    }

    return extractAttributeByAttributeMap(schemaAttribute);
  }

  /**
   * tries to extract an attribute from the json document by checking the attribute map if the attribute names
   * is stored in there in lowercase
   * 
   * @param schemaAttribute the attribute definition that should be extracted from the json document
   * @return the extracted node or an empty if the node is not present within the document
   */
  private Optional<JsonNode> extractAttributeByAttributeMap(SchemaAttribute schemaAttribute)
  {
    final String attributeName = attributeMap.get(schemaAttribute.getName().toLowerCase());
    if (attributeName == null)
    {
      return Optional.empty();
    }
    // this can only happen if the attribute name is really present within the document
    JsonNode attribute = jsonDocument.get(attributeName);
    return Optional.of(attribute);
  }

}
