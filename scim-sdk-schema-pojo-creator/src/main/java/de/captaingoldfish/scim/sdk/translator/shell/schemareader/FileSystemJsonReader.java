package de.captaingoldfish.scim.sdk.translator.shell.schemareader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 29.01.2022
 */
@Slf4j
public class FileSystemJsonReader
{

  @SneakyThrows
  public static List<FileInfoWrapper> parseFileToJsonNode(File file, boolean recursive)
  {
    List<FileInfoWrapper> schemaList = new ArrayList<>();
    if (file.isDirectory() && recursive)
    {
      for ( File listFile : Optional.ofNullable(file.listFiles()).orElse(new File[0]) )
      {
        List<FileInfoWrapper> subdirectorySchemaList = parseFileToJsonNode(listFile, true);
        schemaList.addAll(subdirectorySchemaList);
      }
    }
    else if (file.isDirectory())
    {
      throw new FileNotFoundException(String.format("Path to '%s' is a directory and cannot be translated. "
                                                    + "Use '-r' option to recursively iterate through directories.",
                                                    file.getAbsolutePath()));
    }
    else
    {
      if (!file.exists())
      {
        throw new FileNotFoundException(String.format("The file under path '%s' does not exist",
                                                      file.getAbsolutePath()));
      }
      if (!file.getName().endsWith(".json"))
      {
        log.info("Ignoring file '{}' because it does not end with '.json'", file.getAbsolutePath());
        return schemaList;
      }
      try (InputStream inputStream = new FileInputStream(file))
      {
        String schemaString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        final JsonNode currentSchema;
        try
        {
          currentSchema = JsonHelper.readJsonDocument(schemaString);
        }
        catch (Exception ex)
        {
          log.info("File '{}' is not a valid json document!", file.getAbsolutePath());
          return schemaList;
        }
        schemaList.add(new FileInfoWrapper(file, currentSchema));
      }
    }
    return schemaList;
  }

}
