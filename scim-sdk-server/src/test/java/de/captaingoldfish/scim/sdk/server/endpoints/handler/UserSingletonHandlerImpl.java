package de.captaingoldfish.scim.sdk.server.endpoints.handler;

import java.util.List;

import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.Context;


/**
 * <br>
 * <br>
 * created at: 05.05.2020
 *
 * @author Pascal Knüppel
 */
public class UserSingletonHandlerImpl extends UserHandlerImpl
{

  public UserSingletonHandlerImpl()
  {
    super(true);
  }

  /**
   * handles the user endpoint as singleton endpoint
   *
   * @param id will be null if list-method is called
   * @param attributes
   * @param excludedAttributes
   * @return the singleton instance or rather the first instance of the map
   */
  @Override
  public User getResource(String id,
                          List<SchemaAttribute> attributes,
                          List<SchemaAttribute> excludedAttributes,
                          Context context)
  {
    return getInMemoryMap().get(getInMemoryMap().keySet().iterator().next());
  }
}
