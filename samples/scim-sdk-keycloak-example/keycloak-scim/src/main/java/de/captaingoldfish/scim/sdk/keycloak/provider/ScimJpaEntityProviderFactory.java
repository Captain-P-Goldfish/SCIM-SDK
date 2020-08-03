package de.captaingoldfish.scim.sdk.keycloak.provider;

import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

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
    log.warn("init {}", ID);
  }

  @Override
  public void postInit(KeycloakSessionFactory factory)
  {
    log.warn("postInit {}", ID);
  }

  @Override
  public void close()
  {
    log.warn("close {}", ID);
  }

  @Override
  public String getId()
  {
    return ID;
  }
}
