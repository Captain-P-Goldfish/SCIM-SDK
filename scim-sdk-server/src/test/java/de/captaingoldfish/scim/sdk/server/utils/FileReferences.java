package de.captaingoldfish.scim.sdk.server.utils;

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

  String RESOURCE_BASE_PATH = "/de/captaingoldfish/scim/sdk/server/files/resources";

  String RESOURCE_TYPE_BASE_PATH = "/de/captaingoldfish/scim/sdk/server/files/types";

  String RESOURCE_SCHEMA_BASE_PATH = "/de/captaingoldfish/scim/sdk/server/files/schemas";

  String UTILS_BASE_PATH = "/de/captaingoldfish/scim/sdk/server/files/utils";

  String USER_RESOURCE = RESOURCE_BASE_PATH + "/user/user-resource.json";

  String USER_RESOURCE_ENTERPRISE = RESOURCE_BASE_PATH + "/user/user-resource-with-enterprise-user.json";

  String USER_RESOURCE_IMMUTABLE = RESOURCE_BASE_PATH + "/user/user-resource-with-immutable.json";

  String USER_RESOURCE_X509 = RESOURCE_BASE_PATH + "/user/user-resource-x509.json";

  String USER_CUSTOM_RESOURCE_TYPE = RESOURCE_TYPE_BASE_PATH + "/custom-user-resource-type.json";

  String GROUP_RESOURCE = RESOURCE_BASE_PATH + "/group/group-resource.json";

  String GROUP_RESOURCE_TWO_MEMBERS = RESOURCE_BASE_PATH + "/group/group-resource-two-members.json";

  String GROUP_RESOURCE_USER_MEMBER = RESOURCE_BASE_PATH + "/group/group-resource-user-member.json";

  String GROUP_RESOURCE_GROUP_MEMBER = RESOURCE_BASE_PATH + "/group/group-resource-group-member.json";

  String ROLE_RESOURCE = RESOURCE_BASE_PATH + "/role/role-resource.json";

  String ROLE_RESOURCE_TYPE = RESOURCE_TYPE_BASE_PATH + "/role-resource-type.json";

  String ROLE_RESOURCE_SCHEMA = RESOURCE_SCHEMA_BASE_PATH + "/role-schema.json";

  String DUPLICATE_NAME_SCHEMA = RESOURCE_SCHEMA_BASE_PATH + "/bad/duplicate-name-value.json";

  String DUPLICATE_SUB_NAME_SCHEMA = RESOURCE_SCHEMA_BASE_PATH + "/bad/duplicate-sub-name-value.json";

  String READ_ONLY_NEVER_SCHEMA = RESOURCE_SCHEMA_BASE_PATH + "/bad/read-only-and-never.json";

  String WRITE_ONLY_ALWAYS_SCHEMA = RESOURCE_SCHEMA_BASE_PATH + "/bad/write-only-and-always.json";

  String ALL_TYPES_JSON = RESOURCE_BASE_PATH + "/miscellaneous/all-types.json";

  String ALL_TYPES_JSON_SCHEMA = RESOURCE_SCHEMA_BASE_PATH + "/all-types.json";

  String ALL_TYPES_VALIDATION_SCHEMA = RESOURCE_SCHEMA_BASE_PATH + "/all-types-with-validation.json";

  String ENTERPRISE_USER_VALIDATION_SCHEMA = RESOURCE_SCHEMA_BASE_PATH + "/enterprise-user-with-validation.json";

  String ALL_TYPES_RESOURCE_TYPE = RESOURCE_TYPE_BASE_PATH + "/all-types.json";

  String EMAILS_ATTRIBUTE = UTILS_BASE_PATH + "/emails.json";

  String READ_ONLY_EMAILS_ATTRIBUTE = UTILS_BASE_PATH + "/read-only-emails.json";

  String IMMUTABLE_EMAILS_ATTRIBUTE = UTILS_BASE_PATH + "/immutable-emails.json";

  String READ_ONLY_NAME_ATTRIBUTE = UTILS_BASE_PATH + "/read-only-name.json";

  String IMMUTABLE_NAME_ATTRIBUTE = UTILS_BASE_PATH + "/immutable-name.json";

  String USER_AUTHORIZED_RESOURCE_TYPE = RESOURCE_TYPE_BASE_PATH + "/user-resource-type-authorized.json";

  String VALIDATION_TEST_SCHEMA = RESOURCE_BASE_PATH + "/schemas/validation-test-schema.json";


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
