package de.captaingoldfish.scim.sdk.translator;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.translator.parser.FileSystemJsonReader;
import de.captaingoldfish.scim.sdk.translator.parser.JsonRelationParser;
import de.captaingoldfish.scim.sdk.translator.utils.FileInfoWrapper;
import de.captaingoldfish.scim.sdk.translator.utils.SchemaRelations;
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
   * translate -r -l C:/Users/capta/Dropbox/projekte/java/scim-sdk-parent/scim-sdk-common/src/main/resources/de/captaingoldfish/scim/sdk/common -o C:/Users/capta/Dropbox/projekte/java/scim-sdk-parent/scim-sdk-schema-pojo-creator/target
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
                                 defaultValue = "my.scim.sdk.app") String packageDir)
  {
    File file = new File(schemaLocation);
    List<FileInfoWrapper> fileInfoWrapperList = FileSystemJsonReader.parseFileToJsonNode(file, recursive);
    JsonRelationParser relationParser = new JsonRelationParser(fileInfoWrapperList);
    List<SchemaRelations> schemaRelations = relationParser.getSchemaRelations();

    log.warn("test");
  }

  private void buildPojoFromSchema(String outputDir, String packageDir, Schema schema) throws IOException
  {
    String javaPojoContent = parseSchemaToPojo(schema, packageDir);
    final String filename = StringUtils.capitalize(schema.getName().get().replaceAll("\\s", ""));
    final String outputDirectory = outputDir.endsWith("/") ? outputDir : outputDir + "/";
    File outputFile = new File(String.format("%s%s.java", outputDirectory, filename));
    try (OutputStream outputStream = new FileOutputStream(outputFile))
    {
      outputStream.write(javaPojoContent.getBytes(StandardCharsets.UTF_8));
    }
    log.info("Created POJO at '{}'", outputFile.getAbsolutePath());
  }

  public String parseSchemaToPojo(Schema schema, String packageDir)
  {
    return null; // new SchemaToClassBuilder().generateClassFromSchema(schema, packageDir);
  }

}
