package de.gold.scim.schemas;

import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.constants.ClassPathReferences;
import de.gold.scim.constants.SchemaUris;
import de.gold.scim.exceptions.DocumentValidationException;
import de.gold.scim.utils.JsonHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 12:22 <br>
 * <br>
 * This class is a meta information holder that tells us how a schema must be written. It will be used to
 * validate customized schemas of developers or users
 */
@Slf4j
public final class MetaSchema
{

  /**
   * singleton instance of schema definition
   */
  private static final MetaSchema SCHEMA_DEFINITION = new MetaSchema();

  /**
   * this object will hold the meta schema description on how a schema should be described.
   */
  @Getter(AccessLevel.PROTECTED)
  private JsonNode schemaDocument;

  private MetaSchema()
  {
    loadCustomSchemaDefinition(ClassPathReferences.META_SCHEMA_JSON);
  }

  public static MetaSchema getInstance()
  {
    return SCHEMA_DEFINITION;
  }

  /**
   * will load a modified custom schema definition from a classpath location.
   *
   * @param classPath the fully qualified classpath location path
   */
  public final void loadCustomSchemaDefinition(String classPath)
  {
    schemaDocument = JsonHelper.loadJsonDocument(classPath);
    validateCustomSchema();
  }

  /**
   * will load a modified custom schema definition from a file location
   *
   * @param file the file that holds the modified schema definition
   */
  public final void loadCustomSchemaDefinition(File file)
  {
    schemaDocument = JsonHelper.loadJsonDocument(file);
    validateCustomSchema();
  }

  /**
   * will load a modified custom schema definition from a string representations
   *
   * @param jsonDocument the json document as string representations
   */
  public final void loadCustomSchemaDefinitionFromDocument(String jsonDocument)
  {
    schemaDocument = JsonHelper.readJsonDocument(jsonDocument);
    validateCustomSchema();
  }

  /**
   * this method will validate that the loaded custom schema is fulfilling the minimum requirements to a schema
   * description
   */
  protected void validateCustomSchema()
  {
    log.trace("validating meta schema definition");
    BiFunction<String, Boolean, String> isPresent = (attributeName, blankAllowed) -> {
      String value = JsonHelper.getSimpleAttribute(schemaDocument, attributeName)
                               .orElseThrow(() -> new DocumentValidationException("schema does not contain an '"
                                                                                  + attributeName + "' attribute", null,
                                                                                  null, null));
      if (!blankAllowed && StringUtils.isBlank(value))
      {
        throw new DocumentValidationException("value of attribute '" + attributeName + "' must not be blank", null,
                                              null, null);
      }
      return value;
    };

    String id = isPresent.apply(AttributeNames.ID, false);
    if (!StringUtils.equals(SchemaUris.SCHEMA_URI, id))
    {
      throw new DocumentValidationException("schema id must match the value: " + SchemaUris.SCHEMA_URI, null, null,
                                            null);
    }
    isPresent.apply(AttributeNames.NAME, false);
    isPresent.apply(AttributeNames.DESCRIPTION, false);

    String attributeErrorMessage = "schema does not contain an 'attributes' attribute";
    JsonNode attributes = JsonHelper.getArrayAttribute(schemaDocument, AttributeNames.ATTRIBUTES)
                                    .orElseThrow(() -> new DocumentValidationException(attributeErrorMessage, null,
                                                                                       null, null));
    if (attributes.size() == 0)
    {
      throw new DocumentValidationException("attributes element must at least contain a single attribute: "
                                            + SchemaUris.SCHEMA_URI, null, null, null);
    }
    for ( JsonNode attribute : attributes )
    {
      Consumer<String> validateAttributeExcistence = attributeName -> {
        String errorMessage = "attributes must have an element: " + attributeName;
        String attributeValue = JsonHelper.getSimpleAttribute(attribute, attributeName)
                                          .orElseThrow(() -> new DocumentValidationException(errorMessage, null, null,
                                                                                             null));
        if (StringUtils.isBlank(attributeValue))
        {
          throw new DocumentValidationException("attribute '" + attributeName + "' must not be blank", null, null,
                                                null);
        }
      };

      validateAttributeExcistence.accept(AttributeNames.NAME);
      validateAttributeExcistence.accept(AttributeNames.DESCRIPTION);
      validateAttributeExcistence.accept(AttributeNames.TYPE);
    }
  }

}
