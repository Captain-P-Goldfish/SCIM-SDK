package de.captaingoldfish.scim.sdk.server.schemas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.schemas.validation.RequestResourceValidator;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;


/**
 * this test class will provide tests for specific attributes that have been marked with attribute validations
 * 
 * @author Pascal Knueppel
 * @since 23.01.2021
 */
public class SchemaAttributeValidationTest implements FileReferences
{

  /**
   * the factory that builds and holds all registered resource-types
   */
  private ResourceTypeFactory resourceTypeFactory;

  @BeforeEach
  public void initialize()
  {
    this.resourceTypeFactory = new ResourceTypeFactory();
  }

  /**
   * this test checks the negative case of the following:<br>
   * <br>
   * RFC7643 defines the username as required value that must not be empty, so an attribute validation pattern
   * has been added to the username attribute of the user-resource-schema
   * 
   * @see <a href=
   *      "https://tools.ietf.org/html/rfc7643#section-8.7.1">https://tools.ietf.org/html/rfc7643#section-8.7.1</a>
   */
  @ParameterizedTest
  @ValueSource(strings = {"", " ", "      "})
  public void testUsernameMustNotBeEmpty(String emptyString)
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUser);
    SchemaAttribute usernameAttribute = userResourceType.getMainSchema()
                                                        .getSchemaAttribute(AttributeNames.RFC7643.USER_NAME);
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    user.setUserName(emptyString);

    try
    {
      new RequestResourceValidator(userResourceType, HttpMethod.POST).validateDocument(user);
      Assertions.fail("this point must not be reached");
    }
    catch (DocumentValidationException ex)
    {
      String errorMessage = String.format("The '%s'-attribute '%s' with value '%s' must match the regular expression of '%s'",
                                          usernameAttribute.getType(),
                                          usernameAttribute.getScimNodeName(),
                                          emptyString,
                                          usernameAttribute.getPattern().get().pattern());
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * this test checks the positive case of the following:<br>
   * <br>
   * RFC7643 defines the username as required value that must not be empty, so an attribute validation pattern
   * has been added to the username attribute of the user-resource-schema
   * 
   * @see <a href=
   *      "https://tools.ietf.org/html/rfc7643#section-8.7.1">https://tools.ietf.org/html/rfc7643#section-8.7.1</a>
   */
  @ParameterizedTest
  @ValueSource(strings = {"a", "1", "$", " a", "a ", " a$5 "})
  public void testUsernameWithValidValues(String validUsername)
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUser);
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    user.setUserName(validUsername);

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      return new RequestResourceValidator(userResourceType, HttpMethod.POST).validateDocument(user);
    });

    User validatedUser = JsonHelper.copyResourceToObject(validatedDocument, User.class);
    Assertions.assertEquals(validUsername, validatedUser.getUserName().get());
  }
}
