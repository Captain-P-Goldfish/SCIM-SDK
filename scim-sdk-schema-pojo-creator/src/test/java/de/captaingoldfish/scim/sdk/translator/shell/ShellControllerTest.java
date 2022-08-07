package de.captaingoldfish.scim.sdk.translator.shell;

import java.io.File;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
  @ValueSource(strings = {"./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-1",
                          "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-2",
                          "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-3"})
  public void testFreemarkerParserTest(String pathToSchemas)
  {
    File file = new File(pathToSchemas);
    File targetDirectory = new File(OUTPUT_DIR);
    targetDirectory.mkdirs();
    final String packageDir = "de.captaingoldfish.example.resources";

    String result = shellController.translateSchemas(file.getAbsolutePath(),
                                                     true,
                                                     targetDirectory.getAbsolutePath(),
                                                     packageDir,
                                                     true);
    log.info(result);

    Mockito.verify(shellController)
           .getCreatedFiles(Mockito.eq(file.getAbsolutePath()),
                            Mockito.eq(true),
                            Mockito.eq(targetDirectory.getAbsolutePath()),
                            Mockito.eq(packageDir),
                            Mockito.eq(true));

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
