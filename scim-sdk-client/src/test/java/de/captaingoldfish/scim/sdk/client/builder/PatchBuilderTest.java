package de.captaingoldfish.scim.sdk.client.builder;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Email;
import lombok.extern.slf4j.Slf4j;


/**
 * <br>
 * <br>
 * created at: 02.05.2020
 *
 * @author Pascal Knüppel
 */
@Slf4j
public class PatchBuilderTest extends HttpServerMockup
{

  /**
   * the user on which we will perform patch operations
   */
  private User currentUser;

  /**
   * the patch builder that is used to perform patch-updates on the currentUser
   */
  private PatchBuilder<User> patchBuilder;

  @BeforeEach
  public void initializeTest()
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder().build();
    ScimHttpClient scimHttpClient = new ScimHttpClient(scimClientConfig);

    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    // get the id of an existing user
    final String userId = userHandler.getInMemoryMap().keySet().iterator().next();
    currentUser = userHandler.getInMemoryMap().get(userId);
    patchBuilder = new PatchBuilder<>(getServerUrl(), EndpointPaths.USERS, userId, User.class, scimHttpClient);
  }

  /**
   * verifies that a patch operation can successfully be applied to the current user
   */
  @Test
  public void testBuildPatchOperation()
  {
    final String emailValue = "happy.day@scim-sdk.de";
    final String emailType = "fun";
    final boolean emailPrimary = true;
    final String givenName = "Link";
    final String locale = "JAP";

    User addingResource = User.builder()
                              .emails(Collections.singletonList(Email.builder()
                                                                     .value(emailValue)
                                                                     .type(emailType)
                                                                     .primary(emailPrimary)
                                                                     .build()))
                              .build();
    ServerResponse<User> response = patchBuilder.addOperation()
                                                .path("name.givenname")
                                                .op(PatchOp.ADD)
                                                .value(givenName)
                                                .next()
                                                .path("locale")
                                                .op(PatchOp.REPLACE)
                                                .value(locale)
                                                .next()
                                                .op(PatchOp.ADD)
                                                .valueNode(addingResource)
                                                .build()
                                                .sendRequest();
    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertNotNull(response.getResource());
    User patchedUser = response.getResource();
    Assertions.assertEquals(givenName, patchedUser.getName().flatMap(Name::getGivenName).orElse(null));
    Assertions.assertEquals(locale, patchedUser.getLocale().orElse(null));
    Assertions.assertEquals(1, patchedUser.getEmails().size());
    Assertions.assertEquals(emailValue, patchedUser.getEmails().get(0).getValue().orElse(null));
    Assertions.assertEquals(emailType, patchedUser.getEmails().get(0).getType().orElse(null));
    Assertions.assertEquals(emailPrimary, patchedUser.getEmails().get(0).isPrimary());
  }
}
