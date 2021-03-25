package de.captaingoldfish.scim.sdk.client.setup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;


/**
 * @author Pascal Knueppel
 * @since 25.03.2021
 */
public interface FileReferences
{

  public static final String BASE_PATH = "/de/captaingoldfish/scim/sdk/client";

  public static final String BROKEN_LISTRESPONSE = BASE_PATH + "/testfiles/broken-listresponse.json";

  /**
   * reads a file from the test-resources and modifies the content
   *
   * @param resourcePath the path to the resource
   * @return the resource read into a string value
   */
  default String readResourceFile(String resourcePath)
  {
    return readResourceFile(resourcePath, null);
  }

  /**
   * reads a file from the test-resources and modifies the content
   *
   * @param resourcePath the path to the resource
   * @param changeResourceFileContent a function on the file content to modify the return string
   * @return the resource read into a string value
   */
  default String readResourceFile(String resourcePath, Function<String, String> changeResourceFileContent)
  {
    try (InputStream resourceInputStream = getClass().getResourceAsStream(resourcePath))
    {
      String content = IOUtils.toString(resourceInputStream, StandardCharsets.UTF_8.name());
      if (changeResourceFileContent != null)
      {
        content = changeResourceFileContent.apply(content);
      }
      return content;
    }
    catch (IOException e)
    {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }
}
