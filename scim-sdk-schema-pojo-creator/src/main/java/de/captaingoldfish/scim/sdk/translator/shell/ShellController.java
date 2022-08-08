package de.captaingoldfish.scim.sdk.translator.shell;


import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.translator.shell.parser.EndpointDefinitionBuilder;
import de.captaingoldfish.scim.sdk.translator.shell.parser.ResourceHandlerBuilder;
import de.captaingoldfish.scim.sdk.translator.shell.parser.ResourceNodeBuilder;
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
   * will translate the schemas at the given location into java pojos representing the necessary structures for
   * the SCIM-SDK implementation
   */
  @SneakyThrows
  @ShellMethod(key = "translate", value = "Translate SCIM schemas to Java POJOs for SCIM SDK")
  public String translateSchemas(@ShellOption(value = {"-l", "--location"}, // @formatter:off
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
    List<String> createdFiles = createPojos(schemaLocation, recursive, outputDir, packageDir, useLombok);

    if (createdFiles.isEmpty())
    {
      return "No files were created!";
    }
    else
    {
      return String.format("Successfully created the following files:\n- %s", String.join("\n- ", createdFiles));
    }
  }

  /**
   * creates the java pojos and stores them within the filesystem
   *
   * @return the list of absolute paths that were created
   */
  protected List<String> createPojos(String schemaLocation,
                                     boolean recursive,
                                     String outputDir,
                                     String packageDir,
                                     boolean useLombok)
  {
    File file = new File(schemaLocation);
    List<FileInfoWrapper> fileInfoWrapperList = FileSystemJsonReader.parseFileToJsonNode(file, recursive);
    JsonRelationParser relationParser = new JsonRelationParser(fileInfoWrapperList);
    List<SchemaRelation> schemaRelations = relationParser.getSchemaRelations();

    ResourceNodeBuilder resourceNodeBuilder = new ResourceNodeBuilder(useLombok);
    Map<Schema, String> resourceSchemaPojoMap = resourceNodeBuilder.createResourceSchemaPojos(packageDir,
                                                                                              schemaRelations);

    EndpointDefinitionBuilder endpointDefinitionBuilder = new EndpointDefinitionBuilder();
    Map<JsonNode, String> endpointDefinitionPojoMap = endpointDefinitionBuilder.createEndpointDefinitions(packageDir,
                                                                                                          schemaRelations);

    ResourceHandlerBuilder resourceHandlerBuilder = new ResourceHandlerBuilder();
    Map<SchemaRelation, String> resourceHandlerPojoMap = resourceHandlerBuilder.createResourceHandlerPojos(packageDir,
                                                                                                           schemaRelations);
    return writeCreatedResourcesToFileSystem(outputDir,
                                             resourceSchemaPojoMap,
                                             endpointDefinitionPojoMap,
                                             resourceHandlerPojoMap);
  }

  /**
   * takes the given resources and writes the created pojos to the file system at the specified location
   */
  private static List<String> writeCreatedResourcesToFileSystem(String outputDir,
                                                                Map<Schema, String> resourceSchemaPojoMap,
                                                                Map<JsonNode, String> endpointDefinitionPojoMap,
                                                                Map<SchemaRelation, String> resourceHandlerPojoMap)
  {
    List<String> resourceNodeFiles = PojoWriter.writeResourceNodesToFileSystem(resourceSchemaPojoMap, outputDir);
    List<String> endpointDefinitionFiles = PojoWriter.writeEndpointDefinitionsToFileSystem(endpointDefinitionPojoMap,
                                                                                           outputDir);
    resourceNodeFiles.addAll(endpointDefinitionFiles);
    List<String> resourceHandlerFiles = PojoWriter.writeResourceHandlerToFileSystem(resourceHandlerPojoMap, outputDir);
    resourceNodeFiles.addAll(resourceHandlerFiles);
    return resourceNodeFiles;
  }


}
