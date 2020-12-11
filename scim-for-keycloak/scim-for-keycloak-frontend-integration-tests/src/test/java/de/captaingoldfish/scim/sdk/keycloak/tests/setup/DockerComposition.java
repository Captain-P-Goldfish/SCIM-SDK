package de.captaingoldfish.scim.sdk.keycloak.tests.setup;

import org.testcontainers.containers.Network;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.database.DbSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.database.DockerDatabaseSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloak.KeycloakDockerSetup;


/**
 * this class is used to start a new docker-setup with a keycloak and a database
 *
 * @author Pascal Knüppel
 * @since 11.12.2020
 */
public class DockerComposition implements TestSetup
{

  /**
   * the docker network to use
   */
  private static final Network NETWORK = Network.builder()
                                                .driver("bridge")
                                                .createNetworkCmdModifier(cmd -> cmd.withName("keycloak-tests"))
                                                .build();

  /**
   * the docker database setup that is being used
   */
  private final DbSetup dbSetup;

  /**
   * the keycloak docker setup that is being used
   */
  private final KeycloakDockerSetup keycloakDockerSetup;

  public DockerComposition()
  {
    this.dbSetup = new DockerDatabaseSetup(NETWORK);
    this.keycloakDockerSetup = new KeycloakDockerSetup(NETWORK, dbSetup);
  }

  /**
   * starts the docker container in the correct order
   */
  @Override
  public void start()
  {
    dbSetup.start();
    keycloakDockerSetup.start();
  }

  /**
   * stops the docker container in the correct order
   */
  @Override
  public void stop()
  {
    keycloakDockerSetup.stop();
    dbSetup.stop();
  }

  /**
   * @see KeycloakDockerSetup#getContainerServerUrl()
   */
  @Override
  public String getBrowserAccessUrl()
  {
    return keycloakDockerSetup.getContainerServerUrl();
  }

  /**
   * @see KeycloakDockerSetup#getAdminUser()
   */
  @Override
  public String getAdminUserName()
  {
    return keycloakDockerSetup.getAdminUser();
  }

  /**
   * @see KeycloakDockerSetup#getAdminPassword()
   */
  @Override
  public String getAdminUserPassword()
  {
    return keycloakDockerSetup.getAdminPassword();
  }

  /**
   * @return the database setup
   */
  @Override
  public DbSetup getDbSetup()
  {
    return dbSetup;
  }
}
