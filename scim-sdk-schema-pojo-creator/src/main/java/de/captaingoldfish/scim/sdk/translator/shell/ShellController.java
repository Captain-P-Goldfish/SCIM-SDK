package de.captaingoldfish.scim.sdk.translator.shell;


import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.translator.shell.parser.FreemarkerParser;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.FileInfoWrapper;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.FileSystemJsonReader;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.JsonRelationParser;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.PojoWriter;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.SchemaRelation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 29.04.2021
 */
@Slf4j
@ShellComponent
public class ShellController
{

  /**
   * // @formatter:off
   * translate -r -l E:/Dropbox/projekte/java/scim-sdk-parent/scim-sdk-common/src/main/resources/de/captaingoldfish  /scim/sdk/common -o E:/Dropbox/projekte/java/scim-sdk-parent/scim-sdk-schema-pojo-creator/target
   * // @formatter:on
   */
  @SneakyThrows
  @ShellMethod(key = "translate", value = "Translate SCIM schemas to Java POJOs for SCIM SDK")
  public void translateSchemas(@ShellOption(value = {"-l", "--location"}, // @formatter:off
                                help = "a directory containing resource schemas and resource types or a "
                                  + "direct file location of a resource schema") String schemaLocation, // @formatter:on
                               @ShellOption(value = {"-r", "--recursive"}, //
                                 help = "if the given directory should be searched recursively", //
                                 defaultValue = "false") boolean recursive, //
                               @ShellOption(value = {"-o", "--output"}, //
                                 help = "the output directory where the java POJOs will be placed", //
                                 defaultValue = ".") String outputDir,
                               @ShellOption(value = {"-p", "--package"}, //
                                 help = "The name of the package that will be added to the generated POJOs", //
                                 defaultValue = "my.scim.sdk.app") String packageDir,
                               @ShellOption(value = {"--useLombok"}, //
                                 help = "Add lombok @Builder annotations to constructors", //
                                 defaultValue = "false") boolean useLombok)
  {
    File file = new File(schemaLocation);
    List<FileInfoWrapper> fileInfoWrapperList = FileSystemJsonReader.parseFileToJsonNode(file, recursive);
    JsonRelationParser relationParser = new JsonRelationParser(fileInfoWrapperList);
    List<SchemaRelation> schemaRelations = relationParser.getSchemaRelations();

    FreemarkerParser freemarkerParser = new FreemarkerParser(useLombok);
    Map<Schema, String> pojoMap = freemarkerParser.createJavaResourcePojos(packageDir, schemaRelations);
    PojoWriter.writePojosToFileSystem(pojoMap, outputDir);
  }

}
