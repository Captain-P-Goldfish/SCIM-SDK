package de.captaingoldfish.scim.sdk.client.builder;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
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
   * the scim http client that should be used in the builder
   */
  private ScimHttpClient scimHttpClient;

  /**
   * gets a user from the current userhandler
   */
  @BeforeEach
  public void initializeTest()
  {
    ScimClientConfig scimClientConfig = ScimClientConfig.builder().build();
    scimHttpClient = new ScimHttpClient(scimClientConfig);

    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    // get the id of an existing user
    final String userId = userHandler.getInMemoryMap().keySet().iterator().next();
    currentUser = JsonHelper.copyResourceToObject(userHandler.getInMemoryMap().get(userId).deepCopy(), User.class);
  }

  /**
   * resets the patched user in the userhandler into its original state
   */
  @AfterEach
  public void resetUser()
  {
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    userHandler.getInMemoryMap().put(currentUser.getId().get(), currentUser);
  }

  /**
   * verifies that a patch operation can successfully be applied to the current user
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testBuildPatchOperation(boolean useFullUrl)
  {
    final String userId = currentUser.getId().get();
    PatchBuilder<User> patchBuilder;
    if (useFullUrl)
    {
      patchBuilder = new PatchBuilder<>(getServerUrl() + EndpointPaths.USERS + "/" + userId, User.class,
                                        scimHttpClient);
    }
    else
    {
      patchBuilder = new PatchBuilder<>(getServerUrl(), EndpointPaths.USERS, userId, User.class, scimHttpClient);
    }
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
    Assertions.assertEquals(1, patchedUser.getEmails().size(), patchedUser.toPrettyString());
    Assertions.assertEquals(emailValue, patchedUser.getEmails().get(0).getValue().orElse(null));
    Assertions.assertEquals(emailType, patchedUser.getEmails().get(0).getType().orElse(null));
    Assertions.assertEquals(emailPrimary, patchedUser.getEmails().get(0).isPrimary());
  }
}
