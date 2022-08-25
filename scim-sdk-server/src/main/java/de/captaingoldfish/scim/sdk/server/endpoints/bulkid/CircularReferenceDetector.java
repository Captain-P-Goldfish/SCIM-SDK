package de.captaingoldfish.scim.sdk.server.endpoints.bulkid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;


/**
 * author Pascal Knueppel <br>
 * created at: 21.08.2022 - 12:36 <br>
 * <br>
 * this class is used to find circular references within bulk requests
 */
class CircularReferenceDetector
{

  /**
   * this map will contain the bulkId paths from one resource to another so that circular references can be
   * detected. Imagine the map like this where the numbers are representing the bulkIds:
   *
   * <pre>
   *   1 : [2, 3]
   *   2 : [1] (circular reference)
   * </pre>
   *
   * <pre>
   *   1 : [2]
   *   2 : [3]
   *   3 : [1] (circular reference)
   * </pre>
   *
   * in a request it could look like this but in any order:
   *
   * <pre>
   * {
   *   "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:BulkRequest" ],
   *   "Operations" : [ {
   *     ...
   *     "bulkId" : "1",
   *     "data" : {    ...
   *                   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *                      "manager": {
   *                         "value": "bulkId:2",
   *                      }
   *                   }
   *               }
   *       }
   *   },{
   *     ...
   *     "bulkId" : "2",
   *     "data" : {    ...
   *                   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *                      "manager": {
   *                         "value": "bulkId:3",
   *                      }
   *                   }
   *               }
   *       }
   *   },{
   *     ...
   *     "bulkId" : "3",
   *     "data" : {    ...
   *                   "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
   *                      "manager": {
   *                         "value": "bulkId:1",
   *                      }
   *                   }
   *               }
   *       }
   *   }]
   * }
   * </pre>
   */
  private final Map<String, Set<String>> circularReferenceMap = new HashMap<>();

  public void checkForCircles(BulkIdResolverAbstract bulkIdResourceResolver)
  {
    circularReferenceMap.put(bulkIdResourceResolver.getOperationBulkId(),
                             bulkIdResourceResolver.getReferencedBulkIds());

    checkForCircles();
  }

  /**
   * tries to find circles within the {@link #circularReferenceMap} by checking the remaining bulkId references
   */
  private void checkForCircles()
  {
    for ( String bulkId : circularReferenceMap.keySet() )
    {
      Set<String> referencedBulkIds = circularReferenceMap.get(bulkId);
      checkForCircles(bulkId, bulkId, referencedBulkIds);
    }
  }

  /**
   * recursive method that will try to analyze the currently retrieved bulkId references for a circular
   * reference
   *
   * @param circleStart the start of the circle
   * @param referencedBulkIds the list
   */
  private void checkForCircles(String circleStart, String lastAccessedBulkId, Set<String> referencedBulkIds)
  {
    for ( String referencedBulkId : referencedBulkIds )
    {
      if (referencedBulkId.equals(circleStart))
      {
        String errorMessage = String.format("the bulkIds '%s' and '%s' form a direct or indirect circular reference "
                                            + "that cannot be resolved.",
                                            circleStart,
                                            lastAccessedBulkId);
        throw new ConflictException(errorMessage);
      }
      Set<String> nextReferencedBulkIds = Optional.ofNullable(circularReferenceMap.get(referencedBulkId))
                                                  .map(HashSet::new)
                                                  .orElse(null);
      if (nextReferencedBulkIds != null)
      {
        checkForCircles(circleStart, referencedBulkId, nextReferencedBulkIds);
      }
    }
  }
}
