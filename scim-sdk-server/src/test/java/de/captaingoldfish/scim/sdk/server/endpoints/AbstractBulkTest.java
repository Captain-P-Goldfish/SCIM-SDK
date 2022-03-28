package de.captaingoldfish.scim.sdk.server.endpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import de.captaingoldfish.scim.sdk.common.resources.User;


/**
 * author Pascal Knueppel <br>
 * created at: 08.11.2019 - 23:06 <br>
 * <br>
 */
public abstract class AbstractBulkTest
{


  /**
   * creates update requests for the create operations
   *
   * @param createdUsers the create operations to access the ids
   */
  protected List<BulkRequestOperation> getUpdateUserBulkOperations(Collection<User> createdUsers)
  {
    return getUpdateUserBulkOperations(createdUsers, HttpMethod.PUT);
  }

  /**
   * creates update requests for the create operations
   *
   * @param createdUsers the create operations to access the ids
   */
  protected List<BulkRequestOperation> getUpdateUserBulkOperations(Collection<User> createdUsers, HttpMethod httpMethod)
  {
    List<BulkRequestOperation> operations = new ArrayList<>();
    for ( User createdUser : createdUsers )
    {
      final String id = createdUser.getId().get();
      final String newUserName = UUID.randomUUID().toString();
      final User user = User.builder().userName(newUserName).nickName(newUserName).build();
      operations.add(BulkRequestOperation.builder()
                                         .method(httpMethod)
                                         .path(EndpointPaths.USERS + "/" + id)
                                         .data(user.toString())
                                         .build());
    }
    return operations;
  }

  /**
   * creates delete requests for the create operations
   *
   * @param createUsers the create operations to access the ids
   */
  protected List<BulkRequestOperation> getDeleteUserBulkOperations(Collection<User> createUsers)
  {
    return getDeleteUserBulkOperations(createUsers, HttpMethod.DELETE);
  }

  /**
   * creates delete requests for the create operations
   *
   * @param createUsers the create operations to access the ids
   */
  protected List<BulkRequestOperation> getDeleteUserBulkOperations(Collection<User> createUsers, HttpMethod httpMethod)
  {
    List<BulkRequestOperation> operations = new ArrayList<>();
    for ( User createdUser : createUsers )
    {
      final String id = createdUser.getId().get();
      operations.add(BulkRequestOperation.builder()
                                         .bulkId(UUID.randomUUID().toString())
                                         .method(httpMethod)
                                         .path(EndpointPaths.USERS + "/" + id)
                                         .build());
    }
    return operations;
  }

  /**
   * creates several create operations for a bulk operations
   *
   * @param numberOfOperations number of operations to create
   */
  protected List<BulkRequestOperation> getCreateUserBulkOperations(int numberOfOperations)
  {
    return getCreateUserBulkOperations(numberOfOperations, null);
  }

  /**
   * creates several create operations for a bulk operations
   *
   * @param numberOfOperations number of operations to create
   */
  protected List<BulkRequestOperation> getCreateUserBulkOperations(int numberOfOperations, Boolean returnResource)
  {
    List<BulkRequestOperation> operations = new ArrayList<>();
    for ( int i = 0 ; i < numberOfOperations ; i++ )
    {
      final String username = UUID.randomUUID().toString();
      final User user = User.builder().userName(username).build();
      operations.add(BulkRequestOperation.builder()
                                         .bulkId(UUID.randomUUID().toString())
                                         .method(HttpMethod.POST)
                                         .path(EndpointPaths.USERS)
                                         .data(user.toString())
                                         .returnResource(returnResource)
                                         .build());
    }
    return operations;
  }
}
