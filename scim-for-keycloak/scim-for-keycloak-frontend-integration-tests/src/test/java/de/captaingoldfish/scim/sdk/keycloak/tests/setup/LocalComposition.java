package de.captaingoldfish.scim.sdk.keycloak.tests.setup;

import java.util.Properties;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.database.DbSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.database.LocalDatabaseSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.utils.PropertyReader;


/**
 * this class is used to use a currently running local keycloak setup (the local tests are simply for
 * increasing development performance)
 * 
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
public class LocalComposition implements TestSetup
{

  /**
   * contains the properties of the ./config/local-setup.properties file
   */
  private static final Properties LOCAL_SETUP_PROPERTIES = PropertyReader.getLocalSetupProperties();

  private final DbSetup dbSetup;

  public LocalComposition()
  {
    this.dbSetup = new LocalDatabaseSetup();
  }

  @Override
  public void start()
  {
    // do nothing
  }

  @Override
  public void stop()
  {
    // do nothing
  }

  @Override
  public String getBrowserAccessUrl()
  {
    return LOCAL_SETUP_PROPERTIES.getProperty("keycloak.url");
  }

  @Override
  public String getAdminUserName()
  {
    return LOCAL_SETUP_PROPERTIES.getProperty("keycloak.admin.user");
  }

  @Override
  public String getAdminUserPassword()
  {
    return LOCAL_SETUP_PROPERTIES.getProperty("keycloak.admin.password");
  }

  @Override
  public DbSetup getDbSetup()
  {
    return dbSetup;
  }
}
