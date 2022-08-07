package de.captaingoldfish.scim.sdk.translator.shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 05.08.2022 - 12:29 <br>
 * <br>
 */
@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ShellController.class})
public class ShellControllerTest
{

  private static final String OUTPUT_DIR = "target/pojos";

  @SpyBean
  private ShellController shellController;

  /**
   * delete the created and compiled classes
   */
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

  /**
   * make sure that the files are correctly created under the following conditions:
   * <ol>
   * <li>resource-types exist with schema and one schema is referenced in two resource-types</li>
   * <li>resource-types do exist that reference non-existing schemas</li>
   * <li>resource-node classes are created even if the corresponding resource-type does not exist</li>
   * </ol>
   */
  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-1,false",
              "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-2,false",
              "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-3,false",
              "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-1,true",
              "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-2,true",
              "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-3,true"})
  public void testParseSchemasToJavaPojos(String pathToSchemas, boolean useLombok)
  {
    File file = new File(pathToSchemas);
    File targetDirectory = new File(OUTPUT_DIR);
    targetDirectory.mkdirs();
    final String packageDir = "de.captaingoldfish.example.resources";

    String result = shellController.translateSchemas(file.getAbsolutePath(),
                                                     true,
                                                     targetDirectory.getAbsolutePath(),
                                                     packageDir,
                                                     useLombok);
    log.info(result);

    Mockito.verify(shellController)
           .createdFiles(Mockito.eq(file.getAbsolutePath()),
                         Mockito.eq(true),
                         Mockito.eq(targetDirectory.getAbsolutePath()),
                         Mockito.eq(packageDir),
                         Mockito.eq(useLombok));

    Assertions.assertNotNull(targetDirectory.listFiles());
    String[] filesToCompile = Arrays.stream(targetDirectory.listFiles())
                                    .filter(f -> f.getName().endsWith(".java"))
                                    .map(File::getAbsolutePath)
                                    .toArray(String[]::new);

    MatcherAssert.assertThat(Arrays.stream(filesToCompile)
                                   .map(File::new)
                                   .map(File::getName)
                                   .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder("User.java",
                                                         "Group.java",
                                                         "EnterpriseUser.java",
                                                         "ServiceProviderConfiguration.java"));

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    int compilerResult = compiler.run(null, System.out, System.err, filesToCompile);
    Assertions.assertEquals(0, compilerResult, "compilation failed: " + compilerResult);
  }

  /**
   * references a schema directly with its absolute path and will parse it
   */
  @Test
  public void testParseSingleSchemaToJavaFile()
  {
    File file = new File("./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-1/schemas/users.json");
    File targetDirectory = new File(OUTPUT_DIR);
    final String packageDir = "de.captaingoldfish.example.resources";
    boolean recursive = false;
    boolean useLombok = false;

    String result = shellController.translateSchemas(file.getAbsolutePath(),
                                                     recursive,
                                                     targetDirectory.getAbsolutePath(),
                                                     packageDir,
                                                     useLombok);

    log.info(result);

    Assertions.assertNotNull(targetDirectory.listFiles());
    String[] filesToCompile = Arrays.stream(targetDirectory.listFiles())
                                    .filter(f -> f.getName().endsWith(".java"))
                                    .map(File::getAbsolutePath)
                                    .toArray(String[]::new);

    MatcherAssert.assertThat(Arrays.stream(filesToCompile)
                                   .map(File::new)
                                   .map(File::getName)
                                   .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder("User.java"));
  }

  /**
   * references a file that is a json file but not a SCIM schema representation
   */
  @Test
  public void testTryToParseNonScimJsonFile()
  {
    File file = new File("./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-4/test.json");
    File targetDirectory = new File(OUTPUT_DIR);
    final String packageDir = "de.captaingoldfish.example.resources";
    boolean recursive = false;
    boolean useLombok = false;

    String result = shellController.translateSchemas(file.getAbsolutePath(),
                                                     recursive,
                                                     targetDirectory.getAbsolutePath(),
                                                     packageDir,
                                                     useLombok);

    log.info(result);
    Assertions.assertEquals(0,
                            Arrays.stream(targetDirectory.listFiles())
                                  .filter(f -> f.getName().endsWith(".java"))
                                  .count());
    Assertions.assertEquals("No files were created!", result);
  }

  /**
   * references a file that is a directory and makes sure that the correct error message is printed
   */
  @Test
  public void testTryToParseADirectory()
  {
    File file = new File("./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-5");
    File targetDirectory = new File(OUTPUT_DIR);
    final String packageDir = "de.captaingoldfish.example.resources";
    boolean recursive = false;
    boolean useLombok = false;

    FileNotFoundException ex = Assertions.assertThrows(FileNotFoundException.class,
                                                       () -> shellController.translateSchemas(file.getAbsolutePath(),
                                                                                              recursive,
                                                                                              targetDirectory.getAbsolutePath(),
                                                                                              packageDir,
                                                                                              useLombok));
    Assertions.assertEquals(String.format("Path to '%s' is a directory and cannot be translated. Use '-r'"
                                          + " option to recursively iterate through directories.",
                                          file.getAbsolutePath()),
                            ex.getMessage());
  }

  /**
   * references a directory and tries to recursively parse the sub-files where a single SCIM file is present
   */
  @Test
  public void testTryToParseADirectoryRecursively()
  {
    File file = new File("./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-6");
    File targetDirectory = new File(OUTPUT_DIR);
    final String packageDir = "de.captaingoldfish.example.resources";
    boolean recursive = true;
    boolean useLombok = false;

    String result = shellController.translateSchemas(file.getAbsolutePath(),
                                                     recursive,
                                                     targetDirectory.getAbsolutePath(),
                                                     packageDir,
                                                     useLombok);
    log.info(result);

    Assertions.assertNotNull(targetDirectory.listFiles());
    String[] filesToCompile = Arrays.stream(targetDirectory.listFiles())
                                    .filter(f -> f.getName().endsWith(".java"))
                                    .map(File::getAbsolutePath)
                                    .toArray(String[]::new);

    MatcherAssert.assertThat(Arrays.stream(filesToCompile)
                                   .map(File::new)
                                   .map(File::getName)
                                   .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder("Group.java"));
  }
}
