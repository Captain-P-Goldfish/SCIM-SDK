package de.captaingoldfish.scim.sdk.translator.shell;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

import de.captaingoldfish.scim.sdk.translator.shell.utils.UtilityMethods;
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

  private static final String OUTPUT_DIR = "target/pojos/src/main/java";

  private static final String DEFAULT_PACKAGE_NAME = "de.captaingoldfish.scim.example";

  @SpyBean
  private ShellController shellController;

  /**
   * delete the created and compiled classes
   */
  @SneakyThrows
  @BeforeEach
  public void deleteExistingPojos()
  {
    File fileToDelete = new File(OUTPUT_DIR);
    while (fileToDelete.exists())
    {
      try
      {
        FileUtils.forceDelete(fileToDelete);
        Thread.sleep(10);
      }
      catch (Exception e)
      {
        log.debug("Failed to delete file at location '{}'. Trying to delete file again",
                  fileToDelete.getAbsolutePath());
      }
    }
  }

  /**
   * make sure that the files are correctly created under the following conditions:
   * <ol>
   * <li>resource-types exist with schema and one schema is referenced in two resource-types</li>
   * </ol>
   */
  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"true,true", "false,false", "false,true", "true,false"})
  public void testParseSetup1(boolean useLombok, boolean createConfig)
  {
    final String pathToSchema = "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-1";
    parseSetup(useLombok,
               true,
               pathToSchema,
               Arrays.asList("User.java",
                             "Group.java",
                             "EnterpriseUser.java",
                             "ServiceProviderConfiguration.java",
                             "AllTypes.java",
                             "BulkIdReferences.java"),
               Arrays.asList("UserEndpointDefinition.java",
                             "GroupEndpointDefinition.java",
                             "MeEndpointDefinition.java",
                             "ServiceProviderConfigEndpointDefinition.java",
                             "AllTypesEndpointDefinition.java",
                             "BulkIdReferencesEndpointDefinition.java"),
               Arrays.asList("UserResourceHandler.java",
                             "MeResourceHandler.java",
                             "GroupResourceHandler.java",
                             "ServiceProviderConfigResourceHandler.java",
                             "AllTypesResourceHandler.java",
                             "BulkIdReferencesResourceHandler.java"),
               createConfig);
  }

  /**
   * make sure that the files are correctly created under the following conditions:
   * <ol>
   * <li>resource-types do exist that reference non-existing schemas</li>
   * </ol>
   */
  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"true,true", "false,false", "false,true", "true,false"})
  public void testParseSetup2(boolean useLombok, boolean createConfig)
  {
    final String pathToSchema = "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-2";
    parseSetup(useLombok,
               true,
               pathToSchema,
               Arrays.asList("User.java", "Group.java", "EnterpriseUser.java", "ServiceProviderConfiguration.java"),
               Arrays.asList("UserEndpointDefinition.java",
                             "GroupEndpointDefinition.java",
                             "MeEndpointDefinition.java",
                             "ServiceProviderConfigEndpointDefinition.java"),
               Arrays.asList("UserResourceHandler.java",
                             "GroupResourceHandler.java",
                             "MeResourceHandler.java",
                             "ServiceProviderConfigResourceHandler.java"),
               createConfig);
  }

  /**
   * make sure that the files are correctly created under the following conditions:
   * <ol>
   * <li>resource-node classes are created even if the corresponding resource-type does not exist</li>
   * </ol>
   */
  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"true,true", "false,false", "false,true", "true,false"})
  public void testParseSetup3(boolean useLombok, boolean createConfig)
  {
    final String pathToSchema = "./src/test/resources/de/captaingoldfish/scim/sdk/translator/setup-3";
    parseSetup(useLombok,
               true,
               pathToSchema,
               Arrays.asList("User.java", "Group.java", "EnterpriseUser.java", "ServiceProviderConfiguration.java"),
               Arrays.asList("UserEndpointDefinition.java"),
               Arrays.asList("UserResourceHandler.java"),
               createConfig);
  }

  /**
   * parses a predefined setup and verifies if all expected files are present
   */
  private void parseSetup(boolean useLombok,
                          boolean overrideExistingFiles,
                          String pathToSchema,
                          List<String> expectedResources,
                          List<String> expectedEndpoints,
                          List<String> expectedResourceHandler,
                          boolean createConfig)
  {
    File file = new File(pathToSchema);
    File resourceDirectory = new File(String.format("%s/%s",
                                                    OUTPUT_DIR,
                                                    UtilityMethods.getResourcesPackage(DEFAULT_PACKAGE_NAME, true)));
    File endpointDirectory = new File(String.format("%s/%s",
                                                    OUTPUT_DIR,
                                                    UtilityMethods.getEndpointsPackage(DEFAULT_PACKAGE_NAME, true)));
    File resourceHandlerDirectory = new File(String.format("%s/%s",
                                                           OUTPUT_DIR,
                                                           UtilityMethods.getResourceHandlerPackage(DEFAULT_PACKAGE_NAME,
                                                                                                    true)));
    File scimConfigDirectory = new File(String.format("%s/%s",
                                                      OUTPUT_DIR,
                                                      UtilityMethods.getScimConfigPackage(DEFAULT_PACKAGE_NAME, true)));


    final File targetDirectory = new File(OUTPUT_DIR);

    String result = shellController.translateSchemas(file.getAbsolutePath(),
                                                     true,
                                                     targetDirectory.getAbsolutePath(),
                                                     DEFAULT_PACKAGE_NAME,
                                                     useLombok,
                                                     overrideExistingFiles,
                                                     createConfig);
    log.info(result);

    Mockito.verify(shellController)
           .createPojos(Mockito.eq(file.getAbsolutePath()),
                        Mockito.eq(true),
                        Mockito.eq(targetDirectory.getAbsolutePath()),
                        Mockito.eq(DEFAULT_PACKAGE_NAME),
                        Mockito.eq(useLombok),
                        Mockito.eq(overrideExistingFiles),
                        Mockito.eq(createConfig));

    Assertions.assertNotNull(resourceDirectory.listFiles());
    String[] resourcesToCompile = Arrays.stream(resourceDirectory.listFiles())
                                        .filter(f -> f.getName().endsWith(".java"))
                                        .map(File::getAbsolutePath)
                                        .toArray(String[]::new);
    Assertions.assertNotNull(endpointDirectory.listFiles());
    String[] endpointsToCompile = Arrays.stream(endpointDirectory.listFiles())
                                        .filter(f -> f.getName().endsWith(".java"))
                                        .map(File::getAbsolutePath)
                                        .toArray(String[]::new);
    Assertions.assertNotNull(resourceHandlerDirectory.listFiles());
    String[] resourceHandlerToCompile = Arrays.stream(resourceHandlerDirectory.listFiles())
                                              .filter(f -> f.getName().endsWith(".java"))
                                              .map(File::getAbsolutePath)
                                              .toArray(String[]::new);

    String[] scimConfigToCompile = new String[0];
    if (createConfig)
    {
      Assertions.assertNotNull(scimConfigDirectory.listFiles());
      scimConfigToCompile = Arrays.stream(scimConfigDirectory.listFiles())
                                  .filter(f -> f.getName().endsWith(".java"))
                                  .map(File::getAbsolutePath)
                                  .toArray(String[]::new);
      Assertions.assertEquals(createConfig ? 1 : 0, scimConfigToCompile.length);
    }

    MatcherAssert.assertThat(Arrays.stream(resourcesToCompile)
                                   .map(File::new)
                                   .map(File::getName)
                                   .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder(expectedResources.toArray(new String[0])));
    MatcherAssert.assertThat(Arrays.stream(endpointsToCompile)
                                   .map(File::new)
                                   .map(File::getName)
                                   .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder(expectedEndpoints.toArray(new String[0])));
    MatcherAssert.assertThat(Arrays.stream(resourceHandlerToCompile)
                                   .map(File::new)
                                   .map(File::getName)
                                   .collect(Collectors.toList()),
                             Matchers.containsInAnyOrder(expectedResourceHandler.toArray(new String[0])));
    if (createConfig)
    {
      MatcherAssert.assertThat(Arrays.stream(scimConfigToCompile)
                                     .map(File::new)
                                     .map(File::getName)
                                     .collect(Collectors.toList()),
                               Matchers.containsInAnyOrder("ScimConfig.java"));
    }

    List<String> filesToCompile = new ArrayList<>(Arrays.asList(resourcesToCompile));
    filesToCompile.addAll(Arrays.asList(endpointsToCompile));
    filesToCompile.addAll(Arrays.asList(resourceHandlerToCompile));
    filesToCompile.addAll(Arrays.asList(scimConfigToCompile));

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    int compilerResult = compiler.run(null, System.out, System.err, filesToCompile.toArray(new String[0]));
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
    boolean recursive = false;
    boolean useLombok = false;

    String result = shellController.translateSchemas(file.getAbsolutePath(),
                                                     recursive,
                                                     targetDirectory.getAbsolutePath(),
                                                     DEFAULT_PACKAGE_NAME,
                                                     useLombok,
                                                     true,
                                                     false);

    log.info(result);

    File resourceDirectory = new File(String.format("%s/%s",
                                                    OUTPUT_DIR,
                                                    UtilityMethods.getResourcesPackage(DEFAULT_PACKAGE_NAME, true)));
    Assertions.assertNotNull(resourceDirectory.listFiles());
    String[] filesToCompile = Arrays.stream(resourceDirectory.listFiles())
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
                                                     useLombok,
                                                     true,
                                                     false);

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
                                                                                              useLombok,
                                                                                              true,
                                                                                              false));
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
    boolean recursive = true;
    boolean useLombok = false;

    String result = shellController.translateSchemas(file.getAbsolutePath(),
                                                     recursive,
                                                     targetDirectory.getAbsolutePath(),
                                                     DEFAULT_PACKAGE_NAME,
                                                     useLombok,
                                                     true,
                                                     false);
    log.info(result);

    File resourceDirectory = new File(String.format("%s/%s",
                                                    OUTPUT_DIR,
                                                    UtilityMethods.getResourcesPackage(DEFAULT_PACKAGE_NAME, true)));
    Assertions.assertNotNull(resourceDirectory.listFiles());
    String[] filesToCompile = Arrays.stream(resourceDirectory.listFiles())
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
