package de.captaingoldfish.scim.sdk.translator;


import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.validation.MetaSchemaValidator;
import de.captaingoldfish.scim.sdk.translator.classbuilder.SchemaToClassBuilder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;


/**
 * @author Pascal Knueppel
 * @since 29.04.2021
 */
public class ViewController
{

  private static final Schema META_SCHEMA = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.META_RESOURCE_SCHEMA_JSON));

  @FXML
  private TextArea schemaTextArea;

  @FXML
  private TextArea pojoTextArea;

  @FXML
  void parseSchema(ActionEvent event)
  {
    Schema schema = getCurrentSchema();
    pojoTextArea.setText(parseSchema(schema));
  }

  private Schema getCurrentSchema()
  {
    String schemaString = schemaTextArea.getText();
    JsonNode currentSchema = JsonHelper.readJsonDocument(schemaString);
    JsonNode validatedNode = MetaSchemaValidator.getInstance().validateDocument(META_SCHEMA, currentSchema);
    return new Schema(validatedNode);
  }

  public String parseSchema(Schema schema)
  {
    return new SchemaToClassBuilder().generateClassFromSchema(schema);
  }



}
