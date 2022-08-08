package de.captaingoldfish.scim.sdk.translator.shell.schemareader;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.translator.shell.utils.UtilityMethods;
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
   * @param overrideExistingFiles if already existing files should be replaced or not
   * @return the absolute file paths of all files that were created
   */
  @SneakyThrows
  public static List<String> writeResourceNodesToFileSystem(Map<Schema, String> pojoMap,
                                                            String outputDir,
                                                            String packageName,
                                                            boolean overrideExistingFiles)
  {
    final String targetDirectoryPath = String.format("%s/%s",
                                                     outputDir,
                                                     UtilityMethods.getResourcesPackage(packageName, true)
                                                                   .replaceAll("\\.", "/"));
    final File targetDirectory = new File(targetDirectoryPath);
    if (!targetDirectory.exists())
    {
      targetDirectory.mkdirs();
    }

    List<String> createdFilePaths = new ArrayList<>();
    for ( Map.Entry<Schema, String> schemaPojoEntry : pojoMap.entrySet() )
    {
      Schema schema = schemaPojoEntry.getKey();
      final String fileName = UtilityMethods.getResourceName(schema.getName().get());
      final String filePath = String.format("%s/%s.java", targetDirectory, fileName);
      if (!overrideExistingFiles && new File(filePath).exists())
      {
        continue;
      }
      try (OutputStream outputStream = Files.newOutputStream(Paths.get(filePath)))
      {
        outputStream.write(schemaPojoEntry.getValue().getBytes());
        createdFilePaths.add(filePath.replaceAll("\\\\", "/"));
      }
    }
    return createdFilePaths;
  }

  /**
   * will write the given java pojos into the filesystem at the given output directory
   *
   * @param pojoMap the map of pojos that were created
   * @param outputDir the target directory where the pojos should be saved
   * @param overrideExistingFiles if already existing files should be replaced or not
   * @return the absolute file paths of all files that were created
   */
  @SneakyThrows
  public static List<String> writeEndpointDefinitionsToFileSystem(Map<JsonNode, String> pojoMap,
                                                                  String outputDir,
                                                                  String packageName,
                                                                  boolean overrideExistingFiles)
  {
    final String targetDirectoryPath = String.format("%s/%s",
                                                     outputDir,
                                                     UtilityMethods.getEndpointsPackage(packageName, true)
                                                                   .replaceAll("\\.", "/"));
    final File targetDirectory = new File(targetDirectoryPath);
    if (!targetDirectory.exists())
    {
      targetDirectory.mkdirs();
    }
    List<String> createdFilePaths = new ArrayList<>();
    for ( Map.Entry<JsonNode, String> schemaPojoEntry : pojoMap.entrySet() )
    {
      JsonNode resourceType = schemaPojoEntry.getKey();
      final String endpointName = String.format("%sEndpointDefinition",
                                                resourceType.get(AttributeNames.RFC7643.NAME).textValue());
      final String fileName = UtilityMethods.getResourceName(endpointName);
      final String filePath = String.format("%s/%s.java", targetDirectory, fileName);
      if (!overrideExistingFiles && new File(filePath).exists())
      {
        continue;
      }
      try (OutputStream outputStream = Files.newOutputStream(Paths.get(filePath)))
      {
        outputStream.write(schemaPojoEntry.getValue().getBytes());
        createdFilePaths.add(filePath.replaceAll("\\\\", "/"));
      }
    }
    return createdFilePaths;
  }

  /**
   * will write the resource handler pojos to the filesystem
   *
   * @param resourceHandlerPojoMap the map of pojos that were created
   * @param outputDir the target directory where the pojos should be saved
   * @param overrideExistingFiles if already existing files should be replaced or not
   * @return the absolute file paths of all files that were created
   */
  @SneakyThrows
  public static List<String> writeResourceHandlerToFileSystem(Map<SchemaRelation, String> resourceHandlerPojoMap,
                                                              String outputDir,
                                                              String packageName,
                                                              boolean overrideExistingFiles)
  {
    final String targetDirectoryPath = String.format("%s/%s",
                                                     outputDir,
                                                     UtilityMethods.getResourceHandlerPackage(packageName, true)
                                                                   .replaceAll("\\.", "/"));
    final File targetDirectory = new File(targetDirectoryPath);
    if (!targetDirectory.exists())
    {
      targetDirectory.mkdirs();
    }
    List<String> createdFilesPaths = new ArrayList<>();
    for ( Map.Entry<SchemaRelation, String> createdPojoEntry : resourceHandlerPojoMap.entrySet() )
    {
      final SchemaRelation schemaRelation = createdPojoEntry.getKey();
      final String pojo = createdPojoEntry.getValue();

      final String fileName = UtilityMethods.getResourceName(schemaRelation.getResourceType()
                                                                           .getJsonNode()
                                                                           .get(AttributeNames.RFC7643.NAME)
                                                                           .textValue());
      final String filePath = String.format("%s/%sResourceHandler.java", targetDirectory, fileName);
      if (!overrideExistingFiles && new File(filePath).exists())
      {
        continue;
      }
      try (OutputStream outputStream = Files.newOutputStream(Paths.get(filePath)))
      {
        outputStream.write(pojo.getBytes());
        createdFilesPaths.add(filePath.replaceAll("\\\\", "/"));
      }
    }
    return createdFilesPaths;
  }

  @SneakyThrows
  public static Optional<String> writeScimConfig(String outputDir,
                                                 String packageName,
                                                 String scimConfigPojo,
                                                 boolean overrideExistingFiles)
  {
    final String targetDirectoryPath = String.format("%s/%s",
                                                     outputDir,
                                                     UtilityMethods.getScimConfigPackage(packageName, true)
                                                                   .replaceAll("\\.", "/"));
    final File targetDirectory = new File(targetDirectoryPath);
    if (!targetDirectory.exists())
    {
      targetDirectory.mkdirs();
    }

    final String fileName = "ScimConfig";
    final String filePath = String.format("%s/%s.java", targetDirectory, fileName);
    if (!overrideExistingFiles && new File(filePath).exists())
    {
      return Optional.empty();
    }
    try (OutputStream outputStream = Files.newOutputStream(Paths.get(filePath)))
    {
      outputStream.write(scimConfigPojo.getBytes());
    }
    return Optional.of(filePath);
  }
}
