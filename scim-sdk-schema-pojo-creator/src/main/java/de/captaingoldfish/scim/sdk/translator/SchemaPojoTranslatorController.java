package de.captaingoldfish.scim.sdk.translator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


/**
 * @author Pascal Knueppel
 * @since 29.04.2021
 */
public class SchemaPojoTranslatorController extends Application
{

  /**
   * the JavaFX root node of this application
   */
  private Parent rootNode;

  @Override
  public void start(Stage stage) throws Exception
  {
    stage.setTitle("Schema-To-POJO-creator");
    // stage.getIcons().add();

    final String fxmlViewPath = "/de/captaingoldfish/scim/sdk/translator/views/main-view.fxml";

    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlViewPath));
    rootNode = fxmlLoader.load();

    Platform.runLater(() -> {
      stage.close();
      stage.setScene(new Scene(rootNode, 950, 950));
      stage.show();
    });
  }
}
