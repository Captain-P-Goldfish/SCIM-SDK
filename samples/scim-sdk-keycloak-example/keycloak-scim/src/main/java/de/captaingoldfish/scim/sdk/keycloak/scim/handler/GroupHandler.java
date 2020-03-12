package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
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
public class GroupHandler extends ResourceHandler<Group>
{

  /**
   * an attribute that is added to groups created by the scim protocol
   */
  private static final String SCIM_GROUP = "scim-group";

  /**
   * {@inheritDoc}
   */
  @Override
  public Group createResource(Group group, Authorization authorization)
  {
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    final String groupName = group.getDisplayName().get();
    if (!keycloakSession.realms()
                        .searchForGroupByName(keycloakSession.getContext().getRealm(), groupName, null, null)
                        .isEmpty())
    {
      throw new ConflictException("a group with name '" + groupName + "' does already exist");
    }
    GroupModel groupModel = keycloakSession.getContext().getRealm().createGroup(groupName);
    groupModel = groupToModel(keycloakSession, group, groupModel);
    return modelToGroup(keycloakSession, groupModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group getResource(String id, Authorization authorization)
  {
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    GroupModel groupModel = keycloakSession.getContext().getRealm().getGroupById(id);
    if (groupModel == null || !Boolean.parseBoolean(groupModel.getFirstAttribute(SCIM_GROUP)))
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    return modelToGroup(keycloakSession, groupModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PartialListResponse<Group> listResources(long startIndex,
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
    List<GroupModel> groupModels = keycloakSession.getContext().getRealm().getGroups();
    List<Group> groupList = groupModels.stream()
                                       .filter(groupModel -> Boolean.parseBoolean(groupModel.getFirstAttribute(SCIM_GROUP)))
                                       .map(groupModel -> modelToGroup(keycloakSession, groupModel))
                                       .collect(Collectors.toList());
    return PartialListResponse.<Group> builder()
                              .totalResults(keycloakSession.getContext().getRealm().getGroupsCount(false))
                              .resources(groupList)
                              .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group updateResource(Group groupToUpdate, Authorization authorization)
  {
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    GroupModel groupModel = keycloakSession.getContext().getRealm().getGroupById(groupToUpdate.getId().get());
    if (groupModel == null || !Boolean.parseBoolean(groupModel.getFirstAttribute(SCIM_GROUP)))
    {
      return null; // causes a resource not found exception you may also throw it manually
    }
    groupModel = groupToModel(keycloakSession, groupToUpdate, groupModel);
    return modelToGroup(keycloakSession, groupModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteResource(String id, Authorization authorization)
  {
    KeycloakSession keycloakSession = ((ScimAuthorization)authorization).getKeycloakSession();
    GroupModel groupModel = keycloakSession.getContext().getRealm().getGroupById(id);
    if (groupModel == null || !Boolean.parseBoolean(groupModel.getFirstAttribute(SCIM_GROUP)))
    {
      throw new ResourceNotFoundException("group with id '" + id + "' does not exist");
    }
    keycloakSession.getContext().getRealm().removeGroup(groupModel);
  }

  /**
   * writes the group values into the keycloak group representation
   *
   * @param group the scim group representation
   * @param groupModel the keycloak group representation
   * @return the overridden keycloak group representation
   */
  private GroupModel groupToModel(KeycloakSession keycloakSession, Group group, GroupModel groupModel)
  {
    RealmModel realmModel = keycloakSession.getContext().getRealm();
    group.getDisplayName().ifPresent(groupModel::setName);
    group.getMembers()
         .stream()
         .filter(groupMember -> groupMember.getType().isPresent()
                                && groupMember.getType().get().equalsIgnoreCase("User"))
         .forEach(groupMember -> {
           UserModel userMember = keycloakSession.users().getUserById(groupMember.getValue().get(), realmModel);
           userMember.joinGroup(groupModel);
         });

    group.getMembers()
         .stream()
         .filter(groupMember -> groupMember.getType().isPresent()
                                && groupMember.getType().get().equalsIgnoreCase("Group"))
         .forEach(groupMember -> {
           GroupModel groupModelMember = realmModel.getGroupById(groupMember.getValue().get());
           groupModel.addChild(groupModelMember);
         });
    groupModel.setSingleAttribute(SCIM_GROUP, String.valueOf(true));
    return groupModel;
  }

  /**
   * converts a keycloak {@link GroupModel} into a SCIM representation of {@link Group}
   *
   * @param groupModel the keycloak group representation
   * @return the SCIM group representation
   */
  public static Group modelToGroup(KeycloakSession keycloakSession, GroupModel groupModel)
  {
    return Group.builder()
                .id(groupModel.getId())
                .displayName(groupModel.getName())
                .members(getMembers(keycloakSession, groupModel))
                .meta(Meta.builder().created(getCreated(groupModel)).lastModified(getLastModified(groupModel)).build())
                .build();
  }

  /**
   * gets the members of the given group
   *
   * @param groupModel the group from which the members should be extracted
   * @return a list of the group members
   */
  private static List<Member> getMembers(KeycloakSession keycloakSession, GroupModel groupModel)
  {
    List<Member> members = new ArrayList<>();

    keycloakSession.users()
                   .getGroupMembers(keycloakSession.getContext().getRealm(), groupModel)
                   .stream()
                   .map(groupMember -> Member.builder().value(groupMember.getId()).type("User").build())
                   .forEach(members::add);

    groupModel.getSubGroups()
              .stream()
              .map(subgroup -> Member.builder().value(subgroup.getId()).type("Group").build())
              .forEach(members::add);

    return members;
  }

  /**
   * gets the created value of the group
   *
   * @param groupModel the group model from which the created value should be extracted
   * @return the created value of the given group
   */
  public static Instant getCreated(GroupModel groupModel)
  {
    String createdString = groupModel.getFirstAttribute(AttributeNames.RFC7643.CREATED);
    if (StringUtils.isNotBlank(createdString))
    {
      return Instant.ofEpochMilli(Long.parseLong(createdString));
    }
    else
    {
      return Instant.now();
    }
  }

  /**
   * gets the lastModified value of the group
   *
   * @param groupModel the group model from which the last modified value should be extracted
   * @return the last modified value of the given group
   */
  public static Instant getLastModified(GroupModel groupModel)
  {
    String lastModifiedString = groupModel.getFirstAttribute(AttributeNames.RFC7643.LAST_MODIFIED);
    if (StringUtils.isNotBlank(lastModifiedString))
    {
      return Instant.ofEpochMilli(Integer.parseInt(lastModifiedString));
    }
    else
    {
      return getCreated(groupModel);
    }
  }
}
