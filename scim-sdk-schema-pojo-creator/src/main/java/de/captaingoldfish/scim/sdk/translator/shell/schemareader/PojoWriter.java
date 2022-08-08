package de.captaingoldfish.scim.sdk.translator.shell.schemareader;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 05.08.2022 - 20:36 <br>
 * <br>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PojoWriter
{

  /**
   * will write the given java pojos into the filesystem at the given output directory
   *
   * @param pojoMap the map of pojos that were created
   * @param outputDir the target directory where the pojos should be saved
   * @return the absolute file paths of all files that were created
   */
  @SneakyThrows
  public static List<String> writeResourceNodesToFileSystem(Map<Schema, String> pojoMap, String outputDir)
  {
    new File(outputDir).mkdirs();
    List<String> createdFilePaths = new ArrayList<>();
    for ( Map.Entry<Schema, String> schemaPojoEntry : pojoMap.entrySet() )
    {
      Schema schema = schemaPojoEntry.getKey();
      final String fileName = String.format("%s/%s.java",
                                            outputDir,
                                            StringUtils.capitalize(schema.getName().get()).replaceAll("\\s", ""));
      try (OutputStream outputStream = Files.newOutputStream(Paths.get(fileName)))
      {
        outputStream.write(schemaPojoEntry.getValue().getBytes());
      }
      createdFilePaths.add(fileName);
    }
    return createdFilePaths;
  }

  /**
   * will write the given java pojos into the filesystem at the given output directory
   *
   * @param pojoMap the map of pojos that were created
   * @param outputDir the target directory where the pojos should be saved
   * @return the absolute file paths of all files that were created
   */
  @SneakyThrows
  public static List<String> writeEndpointDefinitionsToFileSystem(Map<JsonNode, String> pojoMap, String outputDir)
  {
    new File(outputDir).mkdirs();
    List<String> createdFilePaths = new ArrayList<>();
    for ( Map.Entry<JsonNode, String> schemaPojoEntry : pojoMap.entrySet() )
    {
      JsonNode resourceType = schemaPojoEntry.getKey();
      final String name = String.format("%sEndpointDefinition",
                                        resourceType.get(AttributeNames.RFC7643.NAME).textValue());
      final String fileName = String.format("%s/%s.java",
                                            outputDir,
                                            StringUtils.capitalize(name).replaceAll("\\s", ""));
      try (OutputStream outputStream = Files.newOutputStream(Paths.get(fileName)))
      {
        outputStream.write(schemaPojoEntry.getValue().getBytes());
      }
      createdFilePaths.add(fileName);
    }
    return createdFilePaths;
  }
}
