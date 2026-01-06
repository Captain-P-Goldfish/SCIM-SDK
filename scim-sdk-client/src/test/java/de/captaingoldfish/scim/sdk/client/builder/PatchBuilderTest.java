package de.captaingoldfish.scim.sdk.client.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.client.ScimClientConfig;
import de.captaingoldfish.scim.sdk.client.http.ScimHttpClient;
import de.captaingoldfish.scim.sdk.client.response.ServerResponse;
import de.captaingoldfish.scim.sdk.client.setup.HttpServerMockup;
import de.captaingoldfish.scim.sdk.client.setup.scim.handler.UserHandler;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
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

  /**
   * verifies that a patch operation can successfully be applied to the current user even if the id needs to be
   * url encoded
   */
  @Test
  public void testBuildPatchOperationWithUrlEncodedId()
  {
    UserHandler userHandler = (UserHandler)scimConfig.getUserResourceType().getResourceHandlerImpl();
    // get the id of an existing user
    final String userId = "abc|def";

    User currentUser = User.builder().id(userId).userName("patch-test").build();
    userHandler.getInMemoryMap().put(userId, currentUser);

    PatchBuilder<User> patchBuilder = new PatchBuilder<>(getServerUrl(), EndpointPaths.USERS, userId, User.class,
                                                         scimHttpClient);
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

  /**
   * verifies that the method
   * {@link de.captaingoldfish.scim.sdk.client.builder.PatchBuilder.PatchOperationBuilder#valueNodes(List)} will
   * produce the expected correct result by adding the list-entries into an array-node that will then be sent
   * within the request.
   */
  @Test
  public void testBuildPatchOperationWithListOfValues()
  {
    final String userId = currentUser.getId().get();
    PatchBuilder<User> patchBuilder = new PatchBuilder<>(getServerUrl(), EndpointPaths.USERS, userId, User.class,
                                                         scimHttpClient);

    Email email1 = Email.builder().value("happy.day@scim-sdk.de").type("fun").primary(true).build();
    Email email2 = Email.builder().value("hello-world@scim-sdk.de").type("work").build();

    List<Email> emailList = Arrays.asList(email1, email2);

    ServerResponse<User> response = patchBuilder.addOperation()
                                                .path("emails")
                                                .op(PatchOp.ADD)
                                                .valueNodes(emailList)
                                                .build()
                                                .sendRequest();

    Assertions.assertEquals(HttpStatus.OK, response.getHttpStatus());
    Assertions.assertNotNull(response.getResource());
    User patchedUser = response.getResource();
    Assertions.assertEquals(2, patchedUser.getEmails().size(), patchedUser.toPrettyString());
    Assertions.assertTrue(patchedUser.getEmails().stream().anyMatch(email -> email.equals(email1)));
    Assertions.assertTrue(patchedUser.getEmails().stream().anyMatch(email -> email.equals(email2)));
  }

  /**
   * this test will verify that the patch operations are split correctly into several lists and that these
   * operations are all sent in different requests to the remote provider
   */
  @DisplayName("Patch operations are split into several requests")
  @Test
  public void testBuildPatchOperationsWithMaxNumberExceeded()
  {
    final String userId = currentUser.getId().get();

    int maxNumberOfOperations = 3;
    ScimClientConfig scimClientConfig = ScimClientConfig.builder()
                                                        .maxPatchOperationsPerRequest(maxNumberOfOperations)
                                                        .build();
    ScimHttpClient scimHttpClient = Mockito.spy(new ScimHttpClient(scimClientConfig));

    PatchBuilder<User> patchBuilder = new PatchBuilder<>(getServerUrl(), EndpointPaths.USERS, userId, User.class,
                                                         scimHttpClient);

    final int expectedNumberOfLists = 10;
    List<PatchRequestOperation> requestOperations = new ArrayList<>();
    for ( int i = 1 ; i < maxNumberOfOperations * expectedNumberOfLists ; i++ )
    {
      requestOperations.add(PatchRequestOperation.builder()
                                                 .path("displayName")
                                                 .op(PatchOp.ADD)
                                                 .value("displayName-" + i)
                                                 .build());
    }
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(requestOperations).build();
    patchBuilder.setPatchResource(patchOpRequest);

    patchBuilder = Mockito.spy(patchBuilder);
    AtomicReference<List<List<PatchRequestOperation>>> splittedOperationListRef = new AtomicReference<>();
    Mockito.doAnswer(invocation -> {
      splittedOperationListRef.set((List<List<PatchRequestOperation>>)invocation.callRealMethod());
      return splittedOperationListRef.get();
    }).when(patchBuilder).splitOperations(Mockito.anyInt());

    ServerResponse<User> patchResponse = patchBuilder.sendRequest();
    Assertions.assertEquals(HttpStatus.OK, patchResponse.getHttpStatus());
    Mockito.verify(patchBuilder).sendRequest();
    Mockito.verify(scimHttpClient, Mockito.times(splittedOperationListRef.get().size())).sendRequest(Mockito.any());

    Assertions.assertEquals(expectedNumberOfLists, splittedOperationListRef.get().size());
    List<PatchRequestOperation> mergedOperationsList = splittedOperationListRef.get()
                                                                               .stream()
                                                                               .flatMap(List::stream)
                                                                               .collect(Collectors.toList());
    for ( int i = 0 ; i < mergedOperationsList.size() ; i++ )
    {
      Assertions.assertEquals("displayName-" + (i + 1), mergedOperationsList.get(i).getValue().get().textValue());
    }

    for ( int i = 0 ; i < splittedOperationListRef.get().size() ; i++ )
    {
      List<PatchRequestOperation> patchRequestOperations = splittedOperationListRef.get().get(i);
      if (i < expectedNumberOfLists - 1)
      {
        Assertions.assertEquals(maxNumberOfOperations, patchRequestOperations.size());
      }
      else
      {
        Assertions.assertEquals(mergedOperationsList.size() % maxNumberOfOperations, patchRequestOperations.size());
      }
    }
  }
}
