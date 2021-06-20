package de.captaingoldfish.scim.sdk.server.schemas.validation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.TestHelper;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 16.05.2021
 */
@Slf4j
public class ResponseResoureValidatorTest implements FileReferences
{

  /**
   * the factory that builds and holds all registered resource-types
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * creates a endpoint reference url to a specific resource that was registered within the
   * {@link #resourceTypeFactory}
   */
  private BiFunction<String, String, String> referenceUrlSupplier = (resourceName, resourceId) -> {
    return String.format("http://localhost:8080/scim/v2/%s/%s", resourceName, resourceId);
  };

  /**
   * initializes this test and verifies that the no-args constructor of the {@link TestUser} instance does not
   * set its schemas-attribute
   */
  @SneakyThrows
  @BeforeEach
  public void initialize()
  {
    this.resourceTypeFactory = new ResourceTypeFactory();

    TestUser testUser = TestUser.class.getConstructor().newInstance();
    Assertions.assertEquals(0,
                            testUser.getSchemas().size(),
                            "These tests depend on the test users no-args-constructor to not add any schemas");
  }

  /**
   * this test validates a resource that does not set its "schemas" attribute within its "no-arg" constructor
   * and still expects the main schema to be present in the validated resource object
   */
  @Test
  public void testSchemasAttributeIsCorrectlyAddedIfNotAddedOnTypeConstruction()
  {
    JsonNode userResourceType = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    Schema userSchema = new Schema(JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON));
    JsonNode enterpriseUserExtension = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    // the user schema must not have any required attributes in order to allow the enterprise schema to be
    // returned alone
    TestHelper.modifyAttributeMetaData(userSchema,
                                       AttributeNames.RFC7643.ID,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       false,
                                       null,
                                       null);
    TestHelper.modifyAttributeMetaData(userSchema,
                                       AttributeNames.RFC7643.USER_NAME,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       false,
                                       null,
                                       null);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(new TestUserResourceHandler(),
                                                                         userResourceType,
                                                                         userSchema,
                                                                         enterpriseUserExtension);

    final String employeeNumber = UUID.randomUUID().toString();
    EnterpriseUser enterpriseUser = EnterpriseUser.builder().employeeNumber(employeeNumber).build();
    TestUser testUser = TestUser.builder().enterpriseUser(enterpriseUser).build();
    Assertions.assertEquals(0, testUser.getSchemas().size());
    Assertions.assertEquals(0, testUser.getSchemas().size());

    JsonNode validatedDocument = Assertions.assertDoesNotThrow(() -> {
      log.error(testUser.toPrettyString());
      return new ResponseResourceValidator(resourceType, null, null, null,
                                           referenceUrlSupplier).validateDocument(testUser);
    });
    log.warn(validatedDocument.toPrettyString());
    TestUser validatedTestUser = (TestUser)validatedDocument;
    Set<String> schemas = validatedTestUser.getSchemas();
    MatcherAssert.assertThat(schemas, Matchers.containsInAnyOrder(SchemaUris.USER_URI, SchemaUris.ENTERPRISE_USER_URI));
  }

  /**
   * test implementation of a resource node that misses to add the main schema to its "schemas"-attribute
   */
  @NoArgsConstructor
  public static class TestUser extends ResourceNode
  {

    @Builder
    private TestUser(String id, EnterpriseUser enterpriseUser, Meta meta)
    {
      // no schemas attribute is set
      setId(id);
      setEnterpriseUser(enterpriseUser);
      setMeta(meta);
    }

    /**
     * The following SCIM extension defines attributes commonly used in representing users that belong to, or act
     * on behalf of, a business or enterprise. The enterprise User extension is identified using the following
     * schema URI: "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User".
     */
    public Optional<EnterpriseUser> getEnterpriseUser()
    {
      return getObjectAttribute(SchemaUris.ENTERPRISE_USER_URI, EnterpriseUser.class);
    }

    /**
     * The following SCIM extension defines attributes commonly used in representing users that belong to, or act
     * on behalf of, a business or enterprise. The enterprise User extension is identified using the following
     * schema URI: "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User".
     */
    public void setEnterpriseUser(EnterpriseUser enterpriseUser)
    {
      setAttribute(SchemaUris.ENTERPRISE_USER_URI, enterpriseUser);
    }
  }

  /**
   * this class is actually a simple copy of
   * {@link de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl} and maps the implementation
   * just on the here temporarily created {@link TestUser} instance
   */
  public static class TestUserResourceHandler extends ResourceHandler<TestUser>
  {

    @Getter
    private Map<String, TestUser> inMemoryMap = new HashMap<>();

    @Override
    public TestUser createResource(TestUser resource, Context context)
    {
      Assertions.assertTrue(resource.getMeta().isPresent());
      Meta meta = resource.getMeta().get();
      Assertions.assertFalse(meta.getLocation().isPresent());
      Assertions.assertTrue(meta.getResourceType().isPresent());
      Assertions.assertEquals(ResourceTypeNames.USER, meta.getResourceType().get());
      final String userId = UUID.randomUUID().toString();
      if (inMemoryMap.containsKey(userId))
      {
        throw new ConflictException("resource with id '" + userId + "' does already exist");
      }
      resource.setId(userId);
      inMemoryMap.put(userId, resource);
      resource.remove(AttributeNames.RFC7643.META);
      Instant created = Instant.now();
      resource.setMeta(Meta.builder().created(created).build());
      return resource;
    }

    @Override
    public TestUser getResource(String id,
                                List<SchemaAttribute> attributes,
                                List<SchemaAttribute> excludedAttributes,
                                Context context)
    {
      TestUser user = inMemoryMap.get(id);
      if (user != null)
      {
        Meta meta = user.getMeta().orElse(Meta.builder().build());
        user.remove(AttributeNames.RFC7643.META);
        user.setMeta(Meta.builder()
                         .created(meta.getCreated().orElse(null))
                         .lastModified(meta.getLastModified().orElse(null))
                         .build());
      }
      return Optional.ofNullable(user)
                     .map(u -> JsonHelper.copyResourceToObject(u.deepCopy(), TestUser.class))
                     .orElse(null);
    }

    @Override
    public PartialListResponse<TestUser> listResources(long startIndex,
                                                       int count,
                                                       FilterNode filter,
                                                       SchemaAttribute sortBy,
                                                       SortOrder sortOrder,
                                                       List<SchemaAttribute> attributes,
                                                       List<SchemaAttribute> excludedAttributes,
                                                       Context context)
    {
      List<TestUser> resourceNodes = new ArrayList<>(inMemoryMap.values());
      resourceNodes.forEach(user -> {
        Meta meta = user.getMeta().get();
        user.remove(AttributeNames.RFC7643.META);
        user.setMeta(Meta.builder()
                         .created(meta.getCreated().get())
                         .lastModified(meta.getLastModified().get())
                         .build());
      });
      return PartialListResponse.<TestUser> builder()
                                .resources(resourceNodes)
                                .totalResults(resourceNodes.size())
                                .build();
    }

    @Override
    public TestUser updateResource(TestUser resource, Context context)
    {
      Assertions.assertTrue(resource.getMeta().isPresent());
      Meta meta = resource.getMeta().get();
      Assertions.assertTrue(meta.getLocation().isPresent());
      Assertions.assertTrue(meta.getResourceType().isPresent());
      Assertions.assertEquals(ResourceTypeNames.USER, meta.getResourceType().get());
      String userId = resource.getId().get();
      TestUser oldUser = inMemoryMap.get(userId);
      if (oldUser == null)
      {
        throw new ResourceNotFoundException("resource with id '" + userId + "' does not exist", null, null);
      }
      inMemoryMap.put(userId, resource);
      Meta oldMeta = oldUser.getMeta().get();

      oldUser.remove(AttributeNames.RFC7643.META);
      resource.remove(AttributeNames.RFC7643.META);

      Instant lastModified = null;
      if (!oldUser.equals(resource))
      {
        lastModified = Instant.now();
      }
      resource.setMeta(Meta.builder().created(oldMeta.getCreated().get()).lastModified(lastModified).build());
      return resource;
    }

    @Override
    public void deleteResource(String id, Context context)
    {
      if (inMemoryMap.containsKey(id))
      {
        inMemoryMap.remove(id);
      }
      else
      {
        throw new ResourceNotFoundException("resource with id '" + id + "' does not exist", null, null);
      }
    }
  }
}
