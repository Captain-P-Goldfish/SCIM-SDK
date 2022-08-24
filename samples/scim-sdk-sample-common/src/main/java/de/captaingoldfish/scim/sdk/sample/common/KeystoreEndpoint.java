package de.captaingoldfish.scim.sdk.sample.common;

import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.EndpointDefinition;


/**
 * <br>
 * <br>
 * created at: 02.05.2020
 *
 * @author Pascal Knüppel
 */
public class KeystoreEndpoint extends EndpointDefinition
{

  /**
   * the basepath to the resources
   */
  private static final String RESOURCE_BASE_PATH = "/de/captaingoldfish/scim/sdk/sample/common";

  /**
   * the location of the keystore resource type definition
   */
  private static final String KEYSTORE_RESOURCE_TYPE_LOCATION = RESOURCE_BASE_PATH
                                                                + "/resourcetypes/keystore-resource-type.json";

  /**
   * the location of the keystore schema definition
   */
  private static final String KEYSTORE_SCHEMA_LOCATION = RESOURCE_BASE_PATH + "/schemas/keystore.json";

  public KeystoreEndpoint()
  {
    super(JsonHelper.loadJsonDocument(KEYSTORE_RESOURCE_TYPE_LOCATION),
          JsonHelper.loadJsonDocument(KEYSTORE_SCHEMA_LOCATION), null, new KeystoreHandler());
  }
}
