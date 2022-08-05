package de.captaingoldfish.scim.sdk.common.utils;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import lombok.RequiredArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 23.07.2022 - 14:24 <br>
 * <br>
 */
@RequiredArgsConstructor
public abstract class AttributeExtractor
{

  /**
   * the json document from which the attributes should be extracted
   */
  protected final JsonNode jsonDocument;

  /**
   * used to get an attribute extractor for resource validation
   *
   * @param schemaAttribute the attributes-definition of the attribute that should be extracted from the
   *          document
   * @return the extracted attribute or null
   */
  public abstract Optional<JsonNode> getAttribute(SchemaAttribute schemaAttribute);
}
