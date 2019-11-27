package de.captaingoldfish.scim.sdk.server.schemas.custom;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;


/**
 * author Pascal Knueppel <br>
 * created at: 27.11.2019 - 23:24 <br>
 * <br>
 */
public class ResourceTypeAuthorizationTest implements FileReferences
{

  /**
   * the resource type factory in which the resources will be registered
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    this.resourceTypeFactory = new ResourceTypeFactory();
  }

  /**
   * this test will verify that the roles are correctly read from a json file if a
   * {@link de.captaingoldfish.scim.sdk.server.schemas.ResourceType} is parsed
   */
  @Test
  public void testRolesLoadedFromJsonFile()
  {
    JsonNode rolesResourceType = JsonHelper.loadJsonDocument(USER_AUTHORIZED_RESOURCE_TYPE);
    JsonNode rolesSchema = JsonHelper.loadJsonDocument(ROLE_RESOURCE_SCHEMA);
    ResourceType resourceType = resourceTypeFactory.registerResourceType(null, rolesResourceType, rolesSchema);

    MatcherAssert.assertThat(resourceType.getFeatures().getAuthorization().getRoles(), Matchers.contains("admin"));
    MatcherAssert.assertThat(resourceType.getFeatures().getAuthorization().getRolesCreate(),
                             Matchers.contains("create"));
    MatcherAssert.assertThat(resourceType.getFeatures().getAuthorization().getRolesGet(), Matchers.contains("get"));
    MatcherAssert.assertThat(resourceType.getFeatures().getAuthorization().getRolesUpdate(),
                             Matchers.contains("update"));
    MatcherAssert.assertThat(resourceType.getFeatures().getAuthorization().getRolesDelete(),
                             Matchers.contains("delete"));
  }

  /**
   * verifies that the getter and setter methods are correctly implemented
   */
  @Test
  public void testGetterAndSetter()
  {
    Function<String, Set<String>> getSet = s -> new HashSet<>(Collections.singletonList(s));
    ResourceTypeAuthorization authorization = ResourceTypeAuthorization.builder()
                                                                       .roles(getSet.apply("admin"))
                                                                       .rolesCreate(getSet.apply("create"))
                                                                       .rolesGet(getSet.apply("get"))
                                                                       .rolesUpdate(getSet.apply("update"))
                                                                       .rolesDelete(getSet.apply("delete"))
                                                                       .build();
    MatcherAssert.assertThat(authorization.getRoles(), Matchers.contains("admin"));
    MatcherAssert.assertThat(authorization.getRolesCreate(), Matchers.contains("create"));
    MatcherAssert.assertThat(authorization.getRolesGet(), Matchers.contains("get"));
    MatcherAssert.assertThat(authorization.getRolesUpdate(), Matchers.contains("update"));
    MatcherAssert.assertThat(authorization.getRolesDelete(), Matchers.contains("delete"));
  }

  /**
   * verifies that the getter and setter methods are correctly implemented
   */
  @Test
  public void testGetterAndSetter2()
  {
    ResourceTypeAuthorization authorization = ResourceTypeAuthorization.builder().build();
    authorization.setRoles("admin", "superAdmin");
    authorization.setRolesCreate("admin", "create");
    authorization.setRolesGet("admin", "get");
    authorization.setRolesUpdate("admin", "update");
    authorization.setRolesDelete("admin", "delete");

    MatcherAssert.assertThat(authorization.getRoles(), Matchers.containsInAnyOrder("admin", "superAdmin"));
    MatcherAssert.assertThat(authorization.getRolesCreate(), Matchers.containsInAnyOrder("admin", "create"));
    MatcherAssert.assertThat(authorization.getRolesGet(), Matchers.containsInAnyOrder("admin", "get"));
    MatcherAssert.assertThat(authorization.getRolesUpdate(), Matchers.containsInAnyOrder("admin", "update"));
    MatcherAssert.assertThat(authorization.getRolesDelete(), Matchers.containsInAnyOrder("admin", "delete"));
  }
}
