package de.captaingoldfish.scim.sdk.server.schemas.custom;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 26.11.2019 - 15:17 <br>
 * <br>
 */
@Slf4j
@NoArgsConstructor
public class ResourceTypeAuthorization extends ScimObjectNode
{

  @Builder
  public ResourceTypeAuthorization(Boolean authenticated,
                                   Boolean useOrOnRoles,
                                   Set<String> roles,
                                   Set<String> rolesCreate,
                                   Set<String> rolesGet,
                                   Set<String> rolesUpdate,
                                   Set<String> rolesDelete)
  {
    this();
    setAuthenticated(authenticated);
    setUseOrOnRoles(useOrOnRoles);
    setRoles(roles);
    setRolesCreate(rolesCreate);
    setRolesGet(rolesGet);
    setRolesUpdate(rolesUpdate);
    setRolesDelete(rolesDelete);
  }

  /**
   * tells us if access to this endpoint will require authentication. Default is true
   */
  public boolean isAuthenticated()
  {
    return getBooleanAttribute(AttributeNames.Custom.AUTHENTICATED).orElse(true);
  }

  /**
   * tells us if access to this endpoint will require authentication. Default is true
   */
  public void setAuthenticated(Boolean authenticated)
  {
    setAttribute(AttributeNames.Custom.AUTHENTICATED, authenticated);
  }

  /**
   * tells us if the roles entered within the arrays must all be present for the user to access the endpoint or
   * if only a single role is necessary. Default is false
   */
  public boolean isUseOrOnRoles()
  {
    return getBooleanAttribute(AttributeNames.Custom.USE_OR_ON_ROLES).orElse(false);
  }

  /**
   * tells us if the roles entered within the arrays must all be present for the user to access the endpoint or
   * if only a single role is necessary. Default is false
   */
  public void setUseOrOnRoles(Boolean useOrOnRoles)
  {
    setAttribute(AttributeNames.Custom.USE_OR_ON_ROLES, useOrOnRoles);
  }

  /**
   * the roles the client must have to access the resource endpoint. This setting defines the roles necessary
   * for all endpoints [create, get, list, update, patch, delete]. This setting may be overridden by other
   * attributes
   */
  public Set<String> getRoles()
  {
    return getSimpleArrayAttributeSet(AttributeNames.Custom.ROLES);
  }

  /**
   * the roles the client must have to access the resource endpoint. This setting defines the roles necessary
   * for all endpoints [create, get, list, update, patch, delete]. This setting may be overridden by other
   * attributes
   */
  public void setRoles(Set<String> roles)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES, roles);
  }

  /**
   * the roles the client must have to access the resource endpoint. This setting defines the roles necessary
   * for all endpoints [create, get, list, update, patch, delete]. This setting may be overridden by other
   * attributes
   */
  public void setRoles(String... roles)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES, new HashSet<>(Arrays.asList(roles)));
  }

  /**
   * the roles the client must have to access the create endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the create endpoint only)
   */
  public Set<String> getRolesCreate()
  {
    return getSimpleArrayAttributeSet(AttributeNames.Custom.ROLES_CREATE);
  }

  /**
   * the roles the client must have to access the create endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the create endpoint only)
   */
  public void setRolesCreate(Set<String> rolesCreate)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES_CREATE, rolesCreate);
  }

  /**
   * the roles the client must have to access the create endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the create endpoint only)
   */
  public void setRolesCreate(String... rolesCreate)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES_CREATE, new HashSet<>(Arrays.asList(rolesCreate)));
  }

  /**
   * the roles the client must have to access the get endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the get endpoint only)
   */
  public Set<String> getRolesGet()
  {
    return getSimpleArrayAttributeSet(AttributeNames.Custom.ROLES_GET);
  }

  /**
   * the roles the client must have to access the get endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the get endpoint only)
   */
  public void setRolesGet(Set<String> rolesGet)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES_GET, rolesGet);
  }

  /**
   * the roles the client must have to access the get endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the get endpoint only)
   */
  public void setRolesGet(String... rolesGet)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES_GET, new HashSet<>(Arrays.asList(rolesGet)));
  }

  /**
   * the roles the client must have to access the list endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the list endpoint only)
   */
  public Set<String> getRolesList()
  {
    return getSimpleArrayAttributeSet(AttributeNames.Custom.ROLES_LIST);
  }

  /**
   * the roles the client must have to access the list endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the list endpoint only)
   */
  public void setRolesList(Set<String> rolesList)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES_LIST, rolesList);
  }

  /**
   * the roles the client must have to access the list endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the list endpoint only)
   */
  public void setRolesList(String... rolesList)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES_LIST, new HashSet<>(Arrays.asList(rolesList)));
  }

  /**
   * the roles the client must have to access the update endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the update endpoint only)
   */
  public Set<String> getRolesUpdate()
  {
    return getSimpleArrayAttributeSet(AttributeNames.Custom.ROLES_UPDATE);
  }

  /**
   * the roles the client must have to access the update endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the update endpoint only)
   */
  public void setRolesUpdate(String... rolesUpdate)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES_UPDATE, new HashSet<>(Arrays.asList(rolesUpdate)));
  }

  /**
   * the roles the client must have to access the update endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the update endpoint only)
   */
  public void setRolesUpdate(Set<String> rolesUpdate)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES_UPDATE, rolesUpdate);
  }

  /**
   * the roles the client must have to access the delete endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the delete endpoint only)
   */
  public Set<String> getRolesDelete()
  {
    return getSimpleArrayAttributeSet(AttributeNames.Custom.ROLES_DELETE);
  }

  /**
   * the roles the client must have to access the delete endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the delete endpoint only)
   */
  public void setRolesDelete(Set<String> rolesDelete)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES_DELETE, rolesDelete);
  }

  /**
   * the roles the client must have to access the delete endpoint.(overrides the attribute "{@link #getRoles()}"
   * for the delete endpoint only)
   */
  public void setRolesDelete(String... rolesDelete)
  {
    setStringAttributeList(AttributeNames.Custom.ROLES_DELETE, new HashSet<>(Arrays.asList(rolesDelete)));
  }
}
