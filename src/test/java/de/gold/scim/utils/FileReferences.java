package de.gold.scim.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;


/**
 * author Pascal Knueppel <br>
 * created at: 04.10.2019 - 20:07 <br>
 * <br>
 */
public interface FileReferences
{

  public static final String RESOURCE_BASE_PATH = "/de/gold/scim/files/resources";

  public static final String RESOURCE_TYPE_BASE_PATH = "/de/gold/scim/files/types";

  public static final String RESOURCE_SCHEMA_BASE_PATH = "/de/gold/scim/files/schemas";

  public static final String USER_RESOURCE = RESOURCE_BASE_PATH + "/user/user-resource.json";

  public static final String USER_RESOURCE_ENTERPRISE = RESOURCE_BASE_PATH
                                                        + "/user/user-resource-with-enterprise-user.json";

  public static final String USER_RESOURCE_IMMUTABLE = RESOURCE_BASE_PATH + "/user/user-resource-with-immutable.json";

  public static final String USER_RESOURCE_X509 = RESOURCE_BASE_PATH + "/user/user-resource-x509.json";

  public static final String USER_CUSTOM_RESOURCE_TYPE = RESOURCE_TYPE_BASE_PATH + "/custom-user-resource-type.json";

  public static final String GROUP_RESOURCE = RESOURCE_BASE_PATH + "/group/group-resource.json";

  public static final String GROUP_RESOURCE_TWO_MEMBERS = RESOURCE_BASE_PATH + "/group/group-resource-two-members.json";

  public static final String GROUP_RESOURCE_USER_MEMBER = RESOURCE_BASE_PATH + "/group/group-resource-user-member.json";

  public static final String GROUP_RESOURCE_GROUP_MEMBER = RESOURCE_BASE_PATH
                                                           + "/group/group-resource-group-member.json";

  public static final String ROLE_RESOURCE = RESOURCE_BASE_PATH + "/role/role-resource.json";

  public static final String ROLE_RESOURCE_TYPE = RESOURCE_TYPE_BASE_PATH + "/role-resource-type.json";

  public static final String ROLE_RESOURCE_SCHEMA = RESOURCE_SCHEMA_BASE_PATH + "/role-schema.json";


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
