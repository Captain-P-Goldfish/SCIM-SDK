package de.captaingoldfish.scim.sdk.common.extensiontests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 17.09.2022 - 12:44 <br>
 * <br>
 */
public class BulkRequestOperationExtensionTest
{

  @Test
  public void testExtendObject()
  {
    Assertions.assertDoesNotThrow(() -> BulkRequestOperationExtension.builder()
                                                                     .method(HttpMethod.GET)
                                                                     .test("hello world")
                                                                     .build());
  }

  public static class BulkRequestOperationExtension extends BulkRequestOperation
  {

    @Builder
    public BulkRequestOperationExtension(HttpMethod method,
                                         String bulkId,
                                         String path,
                                         String data,
                                         ETag version,
                                         Boolean returnResource,
                                         Integer maxResourceLevel,
                                         String test)
    {
      super(method, bulkId, path, data, version, returnResource, maxResourceLevel);
    }

    /**
     * override lombok builder
     */
    public static class BulkRequestOperationExtensionBuilder extends BulkRequestOperation.BulkRequestOperationBuilder
    {}
  }
}
