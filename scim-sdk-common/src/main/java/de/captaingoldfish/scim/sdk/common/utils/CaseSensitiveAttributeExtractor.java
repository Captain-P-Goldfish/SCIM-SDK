package de.captaingoldfish.scim.sdk.common.utils;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;


/**
 * author Pascal Knueppel <br>
 * created at: 23.07.2022 - 14:30 <br>
 * <br>
 * used to extract attributes directly by their attribute-names as defined in their attributes-definition
 */
public class CaseSensitiveAttributeExtractor extends AttributeExtractor
{



  public CaseSensitiveAttributeExtractor(JsonNode jsonDocument)
  {
    super(jsonDocument);
  }

  /**
   * extracts the attribute case-sensitive from the given json document
   * 
   * @param schemaAttribute the attributes-definition of the attribute that should be extracted from the
   *          document
   * @return the attribute from the json document or an empty
   */
  @Override
  public Optional<JsonNode> getAttribute(SchemaAttribute schemaAttribute)
  {
    return Optional.ofNullable(jsonDocument.get(schemaAttribute.getName()));
  }
}
