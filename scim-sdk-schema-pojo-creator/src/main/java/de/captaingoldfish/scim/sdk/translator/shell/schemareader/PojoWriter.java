package de.captaingoldfish.scim.sdk.translator.shell.schemareader;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;


/**
 * author Pascal Knueppel <br>
 * created at: 05.08.2022 - 20:36 <br>
 * <br>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PojoWriter
{

  @SneakyThrows
  public static void writePojosToFileSystem(Map<Schema, String> pojoMap, String outputDir)
  {
    new File(outputDir).mkdirs();
    for ( Map.Entry<Schema, String> schemaPojoEntry : pojoMap.entrySet() )
    {
      Schema schema = schemaPojoEntry.getKey();
      final String fileName = String.format("%s/%s.java", outputDir, StringUtils.capitalize(schema.getName().get()));
      try (OutputStream outputStream = Files.newOutputStream(Paths.get(fileName)))
      {
        outputStream.write(schemaPojoEntry.getValue().getBytes());
      }
    }
  }
}
