package de.captaingoldfish.scim.sdk.translator.shell.parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.FileInfoWrapper;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.FileSystemJsonReader;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.JsonRelationParser;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.SchemaRelation;
import lombok.SneakyThrows;


/**
 * author Pascal Knueppel <br>
 * created at: 05.08.2022 - 12:29 <br>
 * <br>
 */
public class FreemarkerParserTest
{

  @SneakyThrows
  @Test
  public void testFreemarkerParserTest()
  {
    final String schemaLocation = "E:/Dropbox/projekte/java/scim-sdk-parent/scim-sdk-common/src/main/resources/de"
                                  + "/captaingoldfish/scim/sdk/common";
    File file = new File(schemaLocation);
    List<FileInfoWrapper> fileInfoWrapperList = FileSystemJsonReader.parseFileToJsonNode(file, true);
    JsonRelationParser relationParser = new JsonRelationParser(fileInfoWrapperList);
    List<SchemaRelation> schemaRelations = relationParser.getSchemaRelations();

    FreemarkerParser freemarkerParser = new FreemarkerParser(true);
    final Map<Schema, String> javaPojos = freemarkerParser.createJavaResourcePojos("de.captaingoldfish.example.resources",
                                                                                   schemaRelations);

    final String targetDir = "target/pojos";
    new File(targetDir).mkdirs();
    for ( Map.Entry<Schema, String> schemaPojoEntry : javaPojos.entrySet() )
    {
      Schema schema = schemaPojoEntry.getKey();
      final String fileName = String.format("%s/%s.java", targetDir, StringUtils.capitalize(schema.getName().get()));
      try (OutputStream outputStream = new FileOutputStream(fileName))
      {
        outputStream.write(schemaPojoEntry.getValue().getBytes());
      }
    }
  }
}
