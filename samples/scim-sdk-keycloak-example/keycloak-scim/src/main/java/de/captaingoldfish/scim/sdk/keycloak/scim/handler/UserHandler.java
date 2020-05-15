package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 04.02.2020 <br>
 * <br>
 */
@Slf4j
public class UserHandler extends ResourceHandler<User>
{

  /**
   * an attribute that is added to users created by the scim protocol
   */
  private static final String SCIM_USER = "scim-user";

  /**
   * {@inheritDoc}
   */
  @Override
  public User createResource(User user, Authorization authorization)
  {
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    final String username = user.getUserName().get();
    if (keycloakSession.users().getUserByUsername(username, keycloakSession.getContext().getRealm()) != null)
    {
      throw new ConflictException("the username '" + username + "' is already taken");
    }
    UserModel userModel = keycloakSession.users().addUser(keycloakSession.getContext().getRealm(), username);
    userModel = userToModel(user, userModel);
    log.info("created user with username: {}", userModel.getUsername());
    return modelToUser(userModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User getResource(String id, Authorization authorization)
  {
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    UserModel userModel = keycloakSession.users().getUserById(id, keycloakSession.getContext().getRealm());
    if (userModel == null || !Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    return modelToUser(userModel);
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
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    // TODO in order to filter on database level the feature "autoFiltering" must be disabled and the JPA criteria
    // api should be used
    RealmModel realmModel = keycloakSession.getContext().getRealm();
    List<UserModel> userModels = keycloakSession.users().getUsers(realmModel);
    List<User> userList = userModels.stream()
                                    .filter(userModel -> Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
                                    .map(this::modelToUser)
                                    .collect(Collectors.toList());
    return PartialListResponse.<User> builder().totalResults(userList.size()).resources(userList).build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User updateResource(User userToUpdate, Authorization authorization)
  {
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    UserModel userModel = keycloakSession.users()
                                         .getUserById(userToUpdate.getId().get(),
                                                      keycloakSession.getContext().getRealm());
    if (userModel == null || !Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    userModel = userToModel(userToUpdate, userModel);
    log.info("updated user with username: {}", userModel.getUsername());
    return modelToUser(userModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteResource(String id, Authorization authorization)
  {
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    UserModel userModel = keycloakSession.users().getUserById(id, keycloakSession.getContext().getRealm());
    if (userModel == null || !Boolean.parseBoolean(userModel.getFirstAttribute(SCIM_USER)))
    {
      throw new ResourceNotFoundException("resource with id '" + id + "' does not exist");
    }
    keycloakSession.users().removeUser(keycloakSession.getContext().getRealm(), userModel);
    log.info("deleted user with username: {}", userModel.getUsername());
  }

  /**
   * writes the values of the scim user instance into the keycloak user instance
   *
   * @param user the scim user instance
   * @param userModel the keycloak user instance
   * @return the updated keycloak user instance
   */
  private UserModel userToModel(User user, UserModel userModel)
  {
    user.getName().ifPresent(name -> {
      name.getGivenName().ifPresent(userModel::setFirstName);
      name.getFamilyName().ifPresent(userModel::setLastName);
    });
    user.getEmails()
        .stream()
        .filter(MultiComplexNode::isPrimary)
        .findAny()
        .flatMap(MultiComplexNode::getValue)
        .ifPresent(userModel::setEmail);
    user.isActive().ifPresent(userModel::setEnabled);
    userModel.setSingleAttribute(SCIM_USER, String.valueOf(true));
    return userModel;
  }

  /**
   * converts a keycloak {@link UserModel} into a SCIM representation of {@link User}
   *
   * @param userModel the keycloak user representation
   * @return the SCIM user representation
   */
  private User modelToUser(UserModel userModel)
  {
    return User.builder()
               .id(userModel.getId())
               .userName(userModel.getUsername())
               .active(userModel.isEnabled())
               .emails(Collections.singletonList(Email.builder().value(userModel.getEmail()).primary(true).build()))
               .name(Name.builder().givenName(userModel.getFirstName()).familyName(userModel.getLastName()).build())
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
