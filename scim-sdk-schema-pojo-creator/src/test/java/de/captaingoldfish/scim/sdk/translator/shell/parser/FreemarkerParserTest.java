package de.captaingoldfish.scim.sdk.translator.shell.parser;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.FileInfoWrapper;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.FileSystemJsonReader;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.JsonRelationParser;
import de.captaingoldfish.scim.sdk.translator.shell.schemareader.SchemaRelation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 05.08.2022 - 12:29 <br>
 * <br>
 */
@Slf4j
public class FreemarkerParserTest
{

  private static final String OUTPUT_DIR = "target/pojos";

  @BeforeEach
  public void deleteExistingPojos()
  {
    File[] filesToDelete = new File(OUTPUT_DIR).listFiles();
    if (filesToDelete == null)
    {
      return;
    }
    for ( File file : filesToDelete )
    {
      try
      {
        FileUtils.forceDelete(file);
      }
      catch (IOException e)
      {
        log.debug("Could not delete {}", file.getAbsolutePath());
      }
    }
  }

  @SneakyThrows
  @ParameterizedTest
  @ValueSource(strings = {"./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-1",
                          "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-2",
                          "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-3"})
  public void testFreemarkerParserTest(String pathToSchemas)
  {
    File file = new File(pathToSchemas);
    List<FileInfoWrapper> fileInfoWrapperList = FileSystemJsonReader.parseFileToJsonNode(file, true);
    JsonRelationParser relationParser = new JsonRelationParser(fileInfoWrapperList);
    List<SchemaRelation> schemaRelations = relationParser.getSchemaRelations();

    FreemarkerParser freemarkerParser = new FreemarkerParser(true);
    final Map<Schema, String> javaPojos = freemarkerParser.createJavaResourcePojos("de.captaingoldfish.example.resources",
                                                                                   schemaRelations);

    File targetDirectory = new File(OUTPUT_DIR);
    targetDirectory.mkdirs();
    for ( Map.Entry<Schema, String> schemaPojoEntry : javaPojos.entrySet() )
    {
      Schema schema = schemaPojoEntry.getKey();
      final String fileName = String.format("%s/%s.java",
                                            OUTPUT_DIR,
                                            StringUtils.capitalize(schema.getName().get().replaceAll(" ", "")));
      try (OutputStream outputStream = Files.newOutputStream(Paths.get(fileName)))
      {
        outputStream.write(schemaPojoEntry.getValue().getBytes());
      }
    }

    Assertions.assertNotNull(targetDirectory.listFiles());
    String[] filesToCompile = Arrays.stream(targetDirectory.listFiles())
                                    .filter(f -> f.getName().endsWith(".java"))
                                    .map(File::getAbsolutePath)
                                    .toArray(String[]::new);

    MatcherAssert.assertThat(Arrays.stream(filesToCompile)
                                   .map(File::new)
                                   .map(File::getName)
                                   .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder("User.java", "Group.java", "EnterpriseUser.java"));

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    int compilerResult = compiler.run(null, System.out, System.err, filesToCompile);
    Assertions.assertEquals(0, compilerResult, "compilation failed: " + compilerResult);
  }
}
