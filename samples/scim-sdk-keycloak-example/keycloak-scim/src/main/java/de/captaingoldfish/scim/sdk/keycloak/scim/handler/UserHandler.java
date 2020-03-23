package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.GroupNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.MultiComplexNode;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.keycloak.auth.ScimAuthorization;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@Slf4j
public class UserHandler extends ResourceHandler<User>
{

  private static final String SCIM_DEPARTMENT = "department";

  private static final String SCIM_DIVISION = "division";

  private static final String SCIM_DISPLAY_NAME = "displayName";

  private static final String SCIM_FORMATTED_NAME = "formattedName";

  private static final String SCIM_HONORIC_PREFIX = "honoricPrefix";

  private static final String SCIM_HONORIC_SUFFIX = "honoricSuffix";

  private static final String SCIM_LDAP_ID = "LDAP_ID";

  private static final String SCIM_MIDDLE_NAME = "middleName";

  private static final String SCIM_ORGANIZATION = "company";

  private static final String SCIM_TITLE = "title";

  /**
   * an attribute that is added to users created by the scim protocol
   */
  private static final String SCIM_USER = "scim-user";


  private static String KEYCLOAK_DEBUG = System.getProperty("keycloak.debug");

  /**
   * {@inheritDoc}
   */
  @Override
  public User createResource(User user, Authorization authorization)
  {
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    final String username = user.getUserName().get();
    log.info(this.getClass().getName() + " createResource: " + username + "\n" + user.toPrettyString());
    if (KEYCLOAK_DEBUG != null)
    {
      log.info(this.getClass().getName() + " createResource input: " + user.toPrettyString());
    }
    if (keycloakSession.users().getUserByUsername(username, keycloakSession.getContext().getRealm()) != null)
    {
      throw new ConflictException("the username '" + username + "' is already taken");
    }
    UserModel userModel = keycloakSession.users().addUser(keycloakSession.getContext().getRealm(), username);
    userModel = userToModel(user, userModel, keycloakSession);
    User ret = modelToUser(userModel, keycloakSession);
    if (KEYCLOAK_DEBUG != null)
    {
      log.info(this.getClass().getName() + " createResource returns: " + ret.toPrettyString());
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User getResource(String id, Authorization authorization)
  {
    log.info(this.getClass().getName() + " getResource: " + id);

    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    UserModel userModel = keycloakSession.users().getUserById(id, keycloakSession.getContext().getRealm());

    if (userModel == null || !Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    User ret = modelToUser(userModel, keycloakSession);
    if (KEYCLOAK_DEBUG != null)
    {
      log.info(this.getClass().getName() + " getResource returns: " + ret.toPrettyString());
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PartialListResponse<User> listResources(long startIndex,
                                                 int count,
                                                 FilterNode filter,
                                                 SchemaAttribute sortBy,
                                                 SortOrder sortOrder,
                                                 List<SchemaAttribute> attributes,
                                                 List<SchemaAttribute> excludedAttributes,
                                                 Authorization authorization)
  {
    log.info(this.getClass().getName() + " listResources");

    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    // TODO in order to filter on database level the feature "autoFiltering" must be disabled and the JPA criteria
    // api should be used
    RealmModel realmModel = keycloakSession.getContext().getRealm();
    List<UserModel> userModels = keycloakSession.users().getUsers(realmModel);
    List<User> userList = new ArrayList<>();

    for ( UserModel userModel : userModels )
    {
      if (Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
      {
        User add = modelToUser(userModel, keycloakSession);
        if (KEYCLOAK_DEBUG != null)
        {
          log.info(this.getClass().getName() + " listResources User " + add.toPrettyString());
        }
        userList.add(add);
      }
    }
    // List<User> userList = userModels.stream()
    // .filter(userModel -> Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    // .map(this::modelToUser)
    // .collect(Collectors.toList());
    return PartialListResponse.<User> builder().totalResults(userList.size()).resources(userList).build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User updateResource(User userToUpdate, Authorization authorization)
  {
    log.info(this.getClass().getName() + " updateResource " + userToUpdate.getName());
    if (KEYCLOAK_DEBUG != null)
    {
      log.info(this.getClass().getName() + " updateResource input: " + userToUpdate.toPrettyString());
    }
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    UserModel userModel = keycloakSession.users()
                                         .getUserById(userToUpdate.getId().get(),
                                                      keycloakSession.getContext().getRealm());

    if (userModel == null || !Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    userModel = userToModel(userToUpdate, userModel, keycloakSession);
    User ret = modelToUser(userModel, keycloakSession);
    log.info(this.getClass().getName() + " updateResource returns: " + ret.toPrettyString());
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteResource(String id, Authorization authorization)
  {
    log.info(this.getClass().getName() + " deleteResource " + id);

    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    UserModel userModel = keycloakSession.users().getUserById(id, keycloakSession.getContext().getRealm());
    if (userModel == null || !Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    {
      throw new ResourceNotFoundException("resource with id '" + id + "' does not exist");
    }
    if (KEYCLOAK_DEBUG != null)
    {
      log.info(this.getClass().getName() + " deleteResource " + userModel.toString());
    }
    keycloakSession.users().removeUser(keycloakSession.getContext().getRealm(), userModel);
  }

  /**
   * writes the values of the scim user instance into the keycloak user instance
   *
   * @param user the scim user instance
   * @param userModel the keycloak user instance
   * @param keycloakSession keycloak session
   * @return the updated keycloak user instance
   */
  private UserModel userToModel(User user, UserModel userModel, KeycloakSession keycloakSession)
  {
    user.getName().ifPresent(name -> {
      name.getGivenName().ifPresent(userModel::setFirstName);
      name.getFamilyName().ifPresent(userModel::setLastName);
    });
    if (user.isActive().isPresent())
    {
      userModel.setEnabled(user.isActive().get());
    }
    if (user.getLdapId().isPresent())
    {
      userModel.setSingleAttribute(SCIM_LDAP_ID, user.getLdapId().get());
    }
    List<GroupNode> newGroups = user.getGroups();
    Set<GroupModel> groupModelSet = userModel.getGroups();
    // Gruppen verlassen
    for ( GroupModel groupModel : groupModelSet )
    {
      String name = groupModel.getName();
      boolean found = false;
      for ( GroupNode newGroup : newGroups )
      {
        if (name.equals(newGroup.getDisplay().get()))
        {
          found = true;
          break;
        }
      }
      if (!found)
      {
        userModel.leaveGroup(groupModel);
      }
    }
    // Neue Gruppen joinen
    for ( GroupNode newGroup : newGroups )
    {
      String name = newGroup.getDisplay().orElseThrow(() -> new BadRequestException("Groupname must be set"));
      String id = newGroup.getValue().orElseThrow(() -> new BadRequestException("Group ID must be set"));
      boolean found = false;
      for ( GroupModel groupModel : groupModelSet )
      {
        if (groupModel.getName().equals(name))
        {
          found = true;
          break;
        }
      }
      if (!found)
      {
        List<GroupModel> lg = keycloakSession.realms()
                                             .searchForGroupByName(keycloakSession.getContext().getRealm(),
                                                                   name,
                                                                   null,
                                                                   null);
        if (lg.size() > 0)
        {
          userModel.joinGroup(lg.get(0));
        }
        else
        {
          GroupModel group = keycloakSession.getContext().getRealm().createGroup(name);
          userModel.joinGroup(group);
        }
      }
    }
    user.getEmails()
        .stream()
        .filter(MultiComplexNode::isPrimary)
        .findAny()
        .flatMap(MultiComplexNode::getValue)
        .ifPresent(userModel::setEmail);
    user.isActive().ifPresent(userModel::setEnabled);
    userModel.setSingleAttribute(SCIM_USER, String.valueOf(true));
    if (user.getTitle().isPresent())
    {
      userModel.setSingleAttribute(SCIM_TITLE, user.getTitle().get());
    }
    if (user.getName().get().getFormatted().isPresent())
    {
      userModel.setSingleAttribute(SCIM_FORMATTED_NAME, user.getName().get().getFormatted().get());
    }
    if (user.getDisplayName().isPresent())
    {
      userModel.setSingleAttribute(SCIM_DISPLAY_NAME, user.getDisplayName().get());
    }
    if (user.getName().get().getHonorificPrefix().isPresent())
    {
      userModel.setSingleAttribute(SCIM_HONORIC_PREFIX, user.getName().get().getHonorificPrefix().get());
    }

    if (user.getName().get().getHonorificSuffix().isPresent())
    {
      userModel.setSingleAttribute(SCIM_HONORIC_SUFFIX, user.getName().get().getHonorificSuffix().get());
    }

    if (user.getName().get().getMiddleName().isPresent())
    {
      userModel.setSingleAttribute(SCIM_MIDDLE_NAME, user.getName().get().getMiddleName().get());
    }

    if (user.getEnterpriseUser().isPresent())
    {
      EnterpriseUser enterpriseUser = user.getEnterpriseUser().get();
      if (enterpriseUser.getDepartment().isPresent())
      {
        userModel.setSingleAttribute(SCIM_DEPARTMENT, enterpriseUser.getDepartment().get());
      }
      if (enterpriseUser.getDivision().isPresent())
      {
        userModel.setSingleAttribute(SCIM_DIVISION, enterpriseUser.getDivision().get());
      }
      if (enterpriseUser.getOrganization().isPresent())
      {
        userModel.setSingleAttribute(SCIM_ORGANIZATION, enterpriseUser.getOrganization().get());
      }
    }
    // user.getPhotos().stream().findAny()
    // .flatMap(MultiComplexNode::getValue)
    // .ifPresent(userModel::setSingleAttribute);
    return userModel;
  }

  /**
   * converts a keycloak {@link UserModel} into a SCIM representation of {@link User}
   *
   * @param userModel the keycloak user representation
   * @param keycloakSession the keycloak session
   * @return the SCIM user representation
   */
  private User modelToUser(UserModel userModel, KeycloakSession keycloakSession)
  {
    EnterpriseUser enterpriseUser = null;
    String department = userModel.getFirstAttribute(SCIM_DEPARTMENT);
    String division = userModel.getFirstAttribute(SCIM_DIVISION);
    String organization = userModel.getFirstAttribute(SCIM_ORGANIZATION);
    if (department != null || division != null || organization != null)
    {
      enterpriseUser = new EnterpriseUser();
      enterpriseUser.setDepartment(department);
      enterpriseUser.setDivision(division);
      enterpriseUser.setOrganization(organization);
    }

    List<GroupNode> groups = new ArrayList<GroupNode>();
    Set<GroupModel> groupsOfUser = userModel.getGroups();
    for ( GroupModel groupModel : groupsOfUser )
    {
      Group g = GroupHandler.modelToGroup(keycloakSession, groupModel);
      GroupNode gn = GroupNode.builder()
                              .value(g.getId().orElse(""))
                              .display(g.getDisplayName().get())
                              .type("Group")
                              .build();
      groups.add(gn);
    }
    return User.builder()
               .id(userModel.getId())
               .ldapId(userModel.getFirstAttribute(SCIM_LDAP_ID))
               .userName(userModel.getUsername())
               .groups(groups)
               .active(userModel.isEnabled())
               .title(userModel.getFirstAttribute(SCIM_TITLE))
               .emails(Collections.singletonList(Email.builder().value(userModel.getEmail()).primary(true).build()))
               .name(Name.builder()
                         .givenName(userModel.getFirstName())
                         .familyName(userModel.getLastName())
                         .middlename(userModel.getFirstAttribute(SCIM_MIDDLE_NAME))
                         .honorificPrefix(userModel.getFirstAttribute(SCIM_HONORIC_PREFIX))
                         .honorificSuffix(userModel.getFirstAttribute(SCIM_HONORIC_SUFFIX))
                         .formatted(userModel.getFirstAttribute(SCIM_FORMATTED_NAME))
                         .build())
               .displayName(userModel.getFirstAttribute(SCIM_DISPLAY_NAME))
               .enterpriseUser(enterpriseUser)
               .meta(Meta.builder()
                         .created(Instant.ofEpochMilli(userModel.getCreatedTimestamp()))
                         .lastModified(getLastModified(userModel))
                         .build())
               .build();
  }

  /**
   * gets the lastModified value of the user
   *
   * @param userModel the user model from which the last modified value should be extracted
   * @return the last modified value of the given user
   */
  private Instant getLastModified(UserModel userModel)
  {
    String lastModifiedString = userModel.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED);
    if (StringUtils.isNotBlank(lastModifiedString))
    {
      return Instant.ofEpochMilli(Integer.parseInt(lastModifiedString));
    }
    else
    {
      return Instant.ofEpochMilli(userModel.getCreatedTimestamp());
    }
  }
}
