package de.captaingoldfish.scim.sdk.keycloak.scim.handler;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserModel;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;


/**
 * @author Pascal Knueppel
 * @since 27.08.2020
 */
public class GroupHandlerTest extends KeycloakScimManagementTest
{

  /**
   * will verify that a member is being removed from a group if no longer present in the members section
   * 
   * @see <a href="https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/54">
   *      https://github.com/Captain-P-Goldfish/SCIM-SDK/issues/54 </a>
   */
  @Test
  public void testMembersAreRemovedFromGroup()
  {
    UserModel superMario = getKeycloakSession().users().addUser(getRealmModel(), "supermario");
    UserModel bowser = getKeycloakSession().users().addUser(getRealmModel(), "bowser");

    GroupModel nintendo = getKeycloakSession().realms().createGroup(getRealmModel(), "nintendo");
    GroupModel retroStudios = getKeycloakSession().realms().createGroup(getRealmModel(), "retro studios");
    GroupModel marioClub = getKeycloakSession().realms().createGroup(getRealmModel(), "mario club");


    {
      Member memberMario = Member.builder().value(superMario.getId()).type("User").build();
      Member memberBowser = Member.builder().value(bowser.getId()).type("User").build();
      Member memberRetroStudios = Member.builder().value(retroStudios.getId()).type("Group").build();
      Member memberMarioClub = Member.builder().value(marioClub.getId()).type("Group").build();

      PatchOpRequest patchOpRequest = new PatchOpRequest();
      List<PatchRequestOperation> operations = new ArrayList<>();
      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.ADD)
                                          .path("members")
                                          .value(memberMario.toString())
                                          .build());
      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.ADD)
                                          .path("members")
                                          .value(memberBowser.toString())
                                          .build());
      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.ADD)
                                          .path("members")
                                          .value(memberRetroStudios.toString())
                                          .build());
      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.ADD)
                                          .path("members")
                                          .value(memberMarioClub.toString())
                                          .build());
      patchOpRequest.setOperations(operations);

      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                                 .endpoint(EndpointPaths.GROUPS + "/" + nintendo.getId())
                                                 .method(HttpMethod.PATCH)
                                                 .requestBody(patchOpRequest.toString())
                                                 .build();

      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      Assertions.assertTrue(superMario.isMemberOf(nintendo));
      Assertions.assertTrue(bowser.isMemberOf(nintendo));
      Assertions.assertTrue(nintendo.getSubGroups()
                                    .stream()
                                    .map(GroupModel::getName)
                                    .anyMatch(name -> name.equals(retroStudios.getName())));
      Assertions.assertTrue(nintendo.getSubGroups()
                                    .stream()
                                    .map(GroupModel::getName)
                                    .anyMatch(name -> name.equals(marioClub.getName())));
    }

    // now remove bowser and mario club as member from groups
    {
      PatchOpRequest patchOpRequest = new PatchOpRequest();
      List<PatchRequestOperation> operations = new ArrayList<>();

      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.REMOVE)
                                          .path("members[value eq \"" + bowser.getId() + "\"]")
                                          .build());
      operations.add(PatchRequestOperation.builder()
                                          .op(PatchOp.REMOVE)
                                          .path("members[value eq \"" + marioClub.getId() + "\"]")
                                          .build());
      patchOpRequest.setOperations(operations);
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint())
                                                 .endpoint(EndpointPaths.GROUPS + "/" + nintendo.getId())
                                                 .method(HttpMethod.PATCH)
                                                 .requestBody(patchOpRequest.toString())
                                                 .build();

      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      Assertions.assertTrue(superMario.isMemberOf(nintendo));
      Assertions.assertFalse(bowser.isMemberOf(nintendo));
      Assertions.assertTrue(nintendo.getSubGroups()
                                    .stream()
                                    .map(GroupModel::getName)
                                    .anyMatch(name -> name.equals(retroStudios.getName())));
      Assertions.assertFalse(nintendo.getSubGroups()
                                     .stream()
                                     .map(GroupModel::getName)
                                     .anyMatch(name -> name.equals(marioClub.getName())));
    }
  }
}
