package de.captaingoldfish.scim.sdk.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import de.captaingoldfish.scim.sdk.keycloak.services.ScimResourceTypeService;
import de.captaingoldfish.scim.sdk.keycloak.services.ScimServiceProviderService;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 02.08.2020
 */
@Slf4j
public class ScimJpaEntityProviderFactory implements JpaEntityProviderFactory
{

  public static final String ID = "scim-jpa-entity-provider";

  @Override
  public JpaEntityProvider create(KeycloakSession session)
  {
    return new ScimJpaEntityProvider();
  }

  @Override
  public void init(Config.Scope config)
  {

  }

  @Override
  public void postInit(KeycloakSessionFactory factory)
  {
    factory.register((event) -> {
      if (event instanceof RealmModel.RealmRemovedEvent)
        realmRemoved(((RealmModel.RealmRemovedEvent)event).getKeycloakSession());
    });
  }

  @Override
  public void close()
  {

  }

  @Override
  public String getId()
  {
    return ID;
  }

  /**
   * calls the services and removes the setups for the deleted realms
   */
  public void realmRemoved(KeycloakSession keycloakSession)
  {
    new ScimServiceProviderService(keycloakSession).deleteProvider();
    new ScimResourceTypeService(keycloakSession).deleteResourceTypes();
  }
}
