package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Manager;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Address;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.GroupNode;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Ims;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.PhoneNumber;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Photo;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 13.04.2021
 */
@Slf4j
public class RequestResourceValidatorTest implements FileReferences
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
   * validate user resource with writable fields only
   */
  @Test
  public void testValidateUserDocumentWithWritableFieldsOnly()
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUser);

    final JsonNode userResource = User.builder()
                                      .externalId("123")
                                      .userName("max")
                                      .name(Name.builder()
                                                .formatted("Mr. Max Mustermann")
                                                .givenName("max")
                                                .middlename("anything")
                                                .familyName("mustermann")
                                                .honorificPrefix("mr.")
                                                .honorificSuffix("something")
                                                .build())
                                      .displayName("maxi")
                                      .nickName("maximus")
                                      .profileUrl("http://localhost/profile")
                                      .title("dr.")
                                      .userType("employee")
                                      .preferredLanguage("en_US")
                                      .locale("DE")
                                      .timeZone("Germany/Berlin")
                                      .active(true)
                                      .password(UUID.randomUUID().toString())
                                      .emails(Arrays.asList(Email.builder().value("abc@abc.de").primary(true).build(),
                                                            Email.builder().value("cde@cde.de").type("home").build()))
                                      .phoneNumbers(Arrays.asList(PhoneNumber.builder()
                                                                             .value("0123456")
                                                                             .primary(true)
                                                                             .type("work")
                                                                             .build(),
                                                                  PhoneNumber.builder().value("987654").build()))
                                      .ims(Arrays.asList(Ims.builder().value("maxi").primary(true).build(),
                                                         Ims.builder().value("blubb").type("test").build()))
                                      .photos(Arrays.asList(Photo.builder().value("http://localhost/avatar").build()))
                                      .addresses(Arrays.asList(Address.builder()
                                                                      .formatted("any street 37 99999 nowhere")
                                                                      .streetAddress("any street 37")
                                                                      .locality("nowhere")
                                                                      .region("void")
                                                                      .postalCode("99999")
                                                                      .country("empty")
                                                                      .primary(true)
                                                                      .build()))
                                      .build();
    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    User user = (User)Assertions.assertDoesNotThrow(() -> {
      return requestResourceValidator.validateDocument(userResource);
    });
    Assertions.assertNotNull(user);
    Assertions.assertEquals(userResource, user);
  }

  /**
   * validate user resource with writable and readOnly fields
   */
  @Test
  public void testValidateUserDocumentWithWritableAndReadOnlyFields()
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUser);

    final User userResource = User.builder()
                                  .externalId("123")
                                  .userName("max")
                                  .groups(Arrays.asList(GroupNode.builder().value("head-directory").build()))
                                  .build();
    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    User user = (User)Assertions.assertDoesNotThrow(() -> {
      return requestResourceValidator.validateDocument(userResource);
    });
    Assertions.assertNotNull(user);
    Assertions.assertNotEquals(userResource, user);
    Assertions.assertEquals(1, user.getSchemas().size(), user.getSchemas().toString());
    MatcherAssert.assertThat(user.getSchemas(), Matchers.containsInAnyOrder(SchemaUris.USER_URI));

    // remove the groups from the original element and compare again
    userResource.setGroups(null);
    Assertions.assertEquals(userResource, user);
  }

  /**
   * validate user resource without a schema reference
   */
  @Test
  public void testValidateUserDocumentWithoutSchemaReference()
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUser);

    final User userResource = User.builder().externalId("123").userName("max").build();
    userResource.setSchemas((Set<String>)null);
    Assertions.assertNull(userResource.get(AttributeNames.RFC7643.SCHEMAS));

    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    User user = (User)Assertions.assertDoesNotThrow(() -> {
      return requestResourceValidator.validateDocument(userResource);
    });
    Assertions.assertNotNull(user);
    Assertions.assertEquals(userResource, user);
  }

  /**
   * validate user resource with enterprise extension
   */
  @Test
  public void testValidateUserDocumentWithEnterpriseUser()
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUserNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUserNode);

    EnterpriseUser enterpriseUser = EnterpriseUser.builder()
                                                  .employeeNumber("123456")
                                                  .costCenter("654321")
                                                  .organization("orga")
                                                  .division("task-force")
                                                  .department("department")
                                                  .manager(Manager.builder().value("852963").build())
                                                  .build();
    final User userResource = User.builder().externalId("123").userName("max").enterpriseUser(enterpriseUser).build();
    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    User user = (User)Assertions.assertDoesNotThrow(() -> {
      return requestResourceValidator.validateDocument(userResource);
    });
    Assertions.assertNotNull(user);
    Assertions.assertEquals(userResource, user);
    Assertions.assertEquals(2, user.getSchemas().size(), user.getSchemas().toString());
    MatcherAssert.assertThat(user.getSchemas(),
                             Matchers.containsInAnyOrder(SchemaUris.USER_URI, SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * validate user resource with enterprise extension but without a schemas attribute in the resource itself.
   * This test must not fail but the extension reference must have been added into the documents
   * schemas-attribute
   */
  @Test
  public void testValidateUserDocumentWithEnterpriseUserAndNoSchemasReference()
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUserNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUserNode);

    EnterpriseUser enterpriseUser = EnterpriseUser.builder()
                                                  .employeeNumber("123456")
                                                  .costCenter("654321")
                                                  .organization("orga")
                                                  .division("task-force")
                                                  .department("department")
                                                  .manager(Manager.builder().value("852963").build())
                                                  .build();
    final User userResource = User.builder().externalId("123").userName("max").enterpriseUser(enterpriseUser).build();
    userResource.setSchemas((Set<String>)null);

    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    User user = (User)Assertions.assertDoesNotThrow(() -> {
      return requestResourceValidator.validateDocument(userResource);
    });
    Assertions.assertNotNull(user);
    Assertions.assertEquals(userResource, user);
    Assertions.assertEquals(2, user.getSchemas().size(), user.getSchemas().toString());
    MatcherAssert.assertThat(user.getSchemas(),
                             Matchers.containsInAnyOrder(SchemaUris.USER_URI, SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * validate an extension to an empty object by having only readOnly fields set and expect the extension to be
   * removed from the validated resource
   */
  @Test
  public void testEvaluateExtensionToEmpty()
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUserNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    TestHelper.modifyAttributeMetaData(enterpriseUserNode,
                                       AttributeNames.RFC7643.EMPLOYEE_NUMBER,
                                       null,
                                       Mutability.READ_ONLY,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUserNode);

    EnterpriseUser enterpriseUser = EnterpriseUser.builder().employeeNumber("123456").build();
    final User userResource = User.builder().externalId("123").userName("max").enterpriseUser(enterpriseUser).build();
    userResource.setSchemas((Set<String>)null);

    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    User validatedUser = (User)Assertions.assertDoesNotThrow(() -> {
      return requestResourceValidator.validateDocument(userResource);
    });
    Assertions.assertNotNull(validatedUser);
    Assertions.assertNotEquals(userResource, validatedUser);
    Assertions.assertFalse(validatedUser.getEnterpriseUser().isPresent(), validatedUser.toPrettyString());
    Assertions.assertEquals(1, validatedUser.getSchemas().size(), validatedUser.getSchemas().toString());
    MatcherAssert.assertThat(validatedUser.getSchemas(), Matchers.containsInAnyOrder(SchemaUris.USER_URI));
  }

  /**
   * validate that the main-schema reference in the document is missing but the enterprise user reference is
   * present. The main-schema reference must be added automatically by the api
   */
  @Test
  public void testHaveEnterpriseUserExtensionPresentButMainSchemaReferenceIsMissing()
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUserNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUserNode);

    EnterpriseUser enterpriseUser = EnterpriseUser.builder().employeeNumber("123456").build();
    final User userResource = User.builder().externalId("123").userName("max").enterpriseUser(enterpriseUser).build();
    userResource.setSchemas(new HashSet<>(Collections.singletonList(SchemaUris.ENTERPRISE_USER_URI)));

    Assertions.assertEquals(1, userResource.getSchemas().size(), userResource.getSchemas().toString());
    MatcherAssert.assertThat(userResource.getSchemas(), Matchers.containsInAnyOrder(SchemaUris.ENTERPRISE_USER_URI));

    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    User user = (User)Assertions.assertDoesNotThrow(() -> {
      return requestResourceValidator.validateDocument(userResource);
    });
    Assertions.assertNotNull(user);
    // the equality succeeds because the documents schemas attribute given to the validation is being altered
    Assertions.assertEquals(userResource, user);
    Assertions.assertTrue(user.getEnterpriseUser().isPresent(), user.toPrettyString());
    Assertions.assertEquals(2, user.getSchemas().size(), user.getSchemas().toString());
    MatcherAssert.assertThat(user.getSchemas(),
                             Matchers.containsInAnyOrder(SchemaUris.USER_URI, SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * validates that an extension referenced in the schemas attribute is ignored and removed from the validated
   * resource if the extension is not present within the document for as long as the extension is not required
   */
  @Test
  public void testHaveNonePresentSchemaReferenceInSchemasAttribute()
  {
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUserNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUserNode);

    final User userResource = User.builder().externalId("123").userName("max").build();
    userResource.addSchema(SchemaUris.ENTERPRISE_USER_URI);

    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    User user = (User)Assertions.assertDoesNotThrow(() -> {
      return requestResourceValidator.validateDocument(userResource);
    });
    Assertions.assertNotNull(user);
    // the equality succeeds because the documents schemas attribute given to the validation is being altered
    Assertions.assertEquals(userResource, user);
    Assertions.assertFalse(user.getEnterpriseUser().isPresent(), user.toPrettyString());
    Assertions.assertEquals(1, user.getSchemas().size(), user.getSchemas().toString());
    MatcherAssert.assertThat(user.getSchemas(), Matchers.containsInAnyOrder(SchemaUris.USER_URI));
  }

  /**
   * validates that an exception is thrown if a required extension is missing in the document
   */
  @Test
  public void testRequiredExtensionIsMissing()
  {
    // this resource type requires the role resource as an extension
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(USER_CUSTOM_RESOURCE_TYPE);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUserNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    final JsonNode roleResourceNode = JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUserNode,
                                                                             roleResourceNode);

    final User userResource = User.builder().externalId("123").userName("max").build();

    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    try
    {
      requestResourceValidator.validateDocument(userResource);
      Assertions.fail("this point must not be reached");
    }
    catch (DocumentValidationException ex)
    {
      String errorMessage = "Required extension 'urn:gold:params:scim:schemas:custom:2.0:Role' is missing";
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * validates that an exception is thrown if a required extension is missing but its schema-uri was added into
   * the schemas-attribute of the document
   */
  @Test
  public void testRequiredExtensionIsMissingButReferenceIsPresent()
  {
    // this resource type requires the role resource as an extension
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(USER_CUSTOM_RESOURCE_TYPE);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUserNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    final JsonNode roleResourceNode = JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUserNode,
                                                                             roleResourceNode);

    final User userResource = User.builder().externalId("123").userName("max").build();
    userResource.addSchema("urn:gold:params:scim:schemas:custom:2.0:Role");

    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    try
    {
      requestResourceValidator.validateDocument(userResource);
      Assertions.fail("this point must not be reached");
    }
    catch (DocumentValidationException ex)
    {
      String errorMessage = "Required extension 'urn:gold:params:scim:schemas:custom:2.0:Role' is missing";
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
      Assertions.assertEquals(errorMessage, ex.getMessage());
    }
  }

  /**
   * validates that the schema validation is successful if the required extension is present within the
   * document.
   */
  @Test
  public void testRequiredExtensionIsPresent()
  {
    // this resource type requires the role resource as an extension
    final JsonNode userResourceTypeNode = JsonHelper.loadJsonDocument(USER_CUSTOM_RESOURCE_TYPE);
    final JsonNode userSchemaNode = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    final JsonNode enterpriseUserNode = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    final JsonNode roleResourceNode = JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA);
    ResourceType userResourceType = resourceTypeFactory.registerResourceType(new UserHandlerImpl(true),
                                                                             userResourceTypeNode,
                                                                             userSchemaNode,
                                                                             enterpriseUserNode,
                                                                             roleResourceNode);

    final User userResource = User.builder().externalId("123").userName("max").build();
    final String roleUri = "urn:gold:params:scim:schemas:custom:2.0:Role";
    userResource.addSchema(roleUri);
    ObjectNode roleNode = (ObjectNode)JsonHelper.loadJsonDocument(ROLE_RESOURCE);
    roleNode.remove(AttributeNames.RFC7643.SCHEMAS);
    userResource.set(roleUri, roleNode);

    RequestResourceValidator requestResourceValidator = new RequestResourceValidator(userResourceType, HttpMethod.POST);
    User validatedUser = (User)Assertions.assertDoesNotThrow(() -> requestResourceValidator.validateDocument(userResource));
    // the schemas in the resources may have a different order here so we will do the comparisons of the resource
    // and the schemas attribute in separate steps
    Set<String> originalSchemas = userResource.getSchemas();
    Set<String> validatedSchemas = validatedUser.getSchemas();
    userResource.remove(AttributeNames.RFC7643.SCHEMAS);
    validatedUser.remove(AttributeNames.RFC7643.SCHEMAS);
    Assertions.assertEquals(userResource, validatedUser);
    MatcherAssert.assertThat(validatedSchemas,
                             Matchers.containsInAnyOrder(originalSchemas.stream()
                                                                        .map(Matchers::equalTo)
                                                                        .collect(Collectors.toList())));
  }
}
