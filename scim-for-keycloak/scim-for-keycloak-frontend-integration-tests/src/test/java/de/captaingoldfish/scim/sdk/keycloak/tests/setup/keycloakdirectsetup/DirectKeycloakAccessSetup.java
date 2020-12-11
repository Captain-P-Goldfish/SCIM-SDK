package de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup;

import java.math.BigInteger;
import java.util.Map;

import javax.persistence.EntityManager;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
@Slf4j
public class DirectKeycloakAccessSetup
{

  /**
   * initializes the database
   */
  private final DatabaseSetup databaseSetup;

  /**
   * creates a default configuration that we are using in our unit tests
   */
  private final KeycloakMockSetup keycloakMockSetup;

  public DirectKeycloakAccessSetup(Map<String, Object> databaseProperties)
  {
    this.databaseSetup = new DatabaseSetup(databaseProperties);
    this.keycloakMockSetup = new KeycloakMockSetup(databaseSetup.getKeycloakSession(),
                                                   databaseSetup.getEntityManager());
  }

  /**
   * the custom realm for our unit tests
   */
  public RealmModel getRealmModel()
  {
    return keycloakMockSetup.getRealmModel();
  }

  /**
   * @return the mocked keycloak session
   */
  public KeycloakSession getKeycloakSession()
  {
    return databaseSetup.getKeycloakSession();
  }

  /**
   * @return the mocked keycloak session factory
   */
  public KeycloakSessionFactory getKeycloakSessionFactory()
  {
    return keycloakMockSetup.getKeycloakSessionFactory();
  }

  /**
   * @return the entity manager that we and the keycloak tools will use to read and store entities within the
   *         database
   */
  public EntityManager getEntityManager()
  {
    return databaseSetup.getEntityManager();
  }

  /**
   * used to start a new JPA transaction
   */
  public void beginTransaction()
  {
    if (!getEntityManager().getTransaction().isActive())
    {
      getEntityManager().getTransaction().begin();
    }
  }

  /**
   * commits a transaction if any is active
   */
  public void commitTransaction()
  {
    if (getEntityManager().getTransaction().isActive())
    {
      getEntityManager().getTransaction().commit();
    }
  }

  /**
   * counts the number of entries within the given table
   *
   * @param entityClass the class-type of the entity whose entries should be counted
   * @return the number of entries within the database of the given entity-type
   */
  public int countEntriesInTable(Class<?> entityClass)
  {
    return ((Long)getEntityManager().createQuery("select count(entity) from " + entityClass.getSimpleName() + " entity")
                                    .getSingleResult()).intValue();
  }

  /**
   * counts the number of entries within the given table
   *
   * @param tableName the name of the table from which the entries should be counted
   * @return the number of entries within the database of the given entity-type
   */
  public int countEntriesInMappingTable(String tableName)
  {
    return ((BigInteger)getEntityManager().createNativeQuery("select count(*) from " + tableName)
                                          .getSingleResult()).intValue();
  }
}
