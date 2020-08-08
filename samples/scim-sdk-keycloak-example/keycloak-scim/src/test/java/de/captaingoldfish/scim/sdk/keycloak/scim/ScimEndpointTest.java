package de.captaingoldfish.scim.sdk.keycloak.scim;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.BooleanNode;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.scim.administration.ServiceProviderResource;
import de.captaingoldfish.scim.sdk.keycloak.setup.KeycloakScimManagementTest;
import de.captaingoldfish.scim.sdk.keycloak.setup.RequestBuilder;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 07.08.2020
 */
@Slf4j
public class ScimEndpointTest extends KeycloakScimManagementTest
{

  /**
   * verifies that a second instantiation of the endpoint does not create additional unwanted database entries
   */
  @Test
  public void testRecreateEndpoint()
  {
    // the endpoint is already initialized by method super.initializeEndpoint()
    new ScimEndpoint(getKeycloakSession(), getAuthentication());
    Assertions.assertEquals(1, countEntriesInTable(ScimServiceProviderEntity.class));
    Assertions.assertEquals(2, countEntriesInTable(ScimResourceTypeEntity.class));
  }

  /**
   * verify that the scim endpoint is accessible
   */
  @Test
  public void testScimEndpointTest()
  {
    ScimEndpoint scimEndpoint = getScimEndpoint();
    HttpServletRequest request = RequestBuilder.builder(scimEndpoint).endpoint(EndpointPaths.USERS).build();

    Response response = scimEndpoint.handleScimRequest(request);

    Assertions.assertEquals(HttpStatus.OK, response.getStatus());
  }

  /**
   * this test verifies that the scim endpoint cannot be accessed if scim was disabled for the specific realm
   */
  @Test
  public void testDisableScim()
  {
    ServiceProvider serviceProvider = getScimEndpoint().getResourceEndpoint().getServiceProvider();
    serviceProvider.set("enabled", BooleanNode.valueOf(false));
    ServiceProviderResource serviceProviderResource = getScimEndpoint().administerResources()
                                                                       .getServiceProviderResource();
    serviceProviderResource.updateServiceProviderConfig(serviceProvider.toString());

    ScimEndpoint scimEndpoint = getScimEndpoint();
    HttpServletRequest request = RequestBuilder.builder(scimEndpoint).endpoint(EndpointPaths.USERS).build();

    try
    {
      scimEndpoint.handleScimRequest(request);
      Assertions.fail("this point must not be reached");
    }
    catch (NotFoundException ex)
    {
      log.trace("everything's fine", ex);
    }
  }

  /**
   * creates a second realm and verifies that entries for both realms are present within the database
   */
  @Test
  public void testSetupScimForTwoRealms()
  {

    // try to load the users from the default realm. A single user should be returned
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint()).endpoint(EndpointPaths.USERS).build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      String responseString = (String)response.getEntity();
      ListResponse listResponse = JsonHelper.readJsonDocument(responseString, ListResponse.class);
      Assertions.assertEquals(1, listResponse.getTotalResults());
    }

    KeycloakContext context = getKeycloakSession().getContext();
    RealmModel newRealm = getKeycloakSession().realms().createRealm("2ndRealm");
    Mockito.doReturn(newRealm).when(context).getRealm();
    ScimConfiguration.getScimEndpoint(getKeycloakSession());
    Assertions.assertEquals(2, ScimConfigurationBridge.getScimResourceEndpoints().size());

    // now try to load the users from the other realm. An empty list should be returned
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint()).endpoint(EndpointPaths.USERS).build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      String responseString = (String)response.getEntity();
      ListResponse listResponse = JsonHelper.readJsonDocument(responseString, ListResponse.class);
      Assertions.assertEquals(0, listResponse.getTotalResults());
    }
  }

  /**
   * creates a second realm, disables the default realm and shows that the new realm is still accessible while
   * the default realm is not
   */
  @Test
  public void testDisableDefaultRealmAndLeaveSecondRealmOpen()
  {
    KeycloakContext context = getKeycloakSession().getContext();
    RealmModel newRealm = getKeycloakSession().realms().createRealm("2ndRealm");
    // first thing: create the new realm and initialize the scim configuration
    {
      Mockito.doReturn(newRealm).when(context).getRealm();
      ScimConfiguration.getScimEndpoint(getKeycloakSession());
      Assertions.assertEquals(2, ScimConfigurationBridge.getScimResourceEndpoints().size());
    }

    // switch back to default realm
    {
      Mockito.doReturn(getRealmModel()).when(context).getRealm();
    }

    // disable SCIM for default realm
    {
      ServiceProvider serviceProvider = getScimEndpoint().getResourceEndpoint().getServiceProvider();
      serviceProvider.set("enabled", BooleanNode.valueOf(false));
      ServiceProviderResource serviceProviderResource = getScimEndpoint().administerResources()
                                                                         .getServiceProviderResource();
      serviceProviderResource.updateServiceProviderConfig(serviceProvider.toString());
    }

    // try to load the users from the default realm. An exception should be thrown
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint()).endpoint(EndpointPaths.USERS).build();
      try
      {
        getScimEndpoint().handleScimRequest(request);
        Assertions.fail("this point must not be reached");
      }
      catch (NotFoundException ex)
      {
        log.trace("everything's fine", ex);
      }
    }

    // switch back to the new realm
    {
      Mockito.doReturn(newRealm).when(context).getRealm();
    }

    // now try to load the users from the new realm. An empty list should be returned
    {
      HttpServletRequest request = RequestBuilder.builder(getScimEndpoint()).endpoint(EndpointPaths.USERS).build();
      Response response = getScimEndpoint().handleScimRequest(request);
      Assertions.assertEquals(HttpStatus.OK, response.getStatus());

      String responseString = (String)response.getEntity();
      ListResponse listResponse = JsonHelper.readJsonDocument(responseString, ListResponse.class);
      Assertions.assertEquals(0, listResponse.getTotalResults());
    }
    // that proves that SCIM can be enabled and disabled for specific realms
  }


}
