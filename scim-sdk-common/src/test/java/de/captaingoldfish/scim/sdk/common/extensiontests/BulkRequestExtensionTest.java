package de.captaingoldfish.scim.sdk.common.extensiontests;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.request.BulkRequest;
import de.captaingoldfish.scim.sdk.common.request.BulkRequestOperation;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 17.09.2022 - 12:36 <br>
 * <br>
 */
public class BulkRequestExtensionTest
{

  @Test
  public void testExtendObject()
  {
    Assertions.assertDoesNotThrow(() -> BulkRequestExtension.builder().failOnErrors(1).test("hello").build());
  }

  public static class BulkRequestExtension extends BulkRequest
  {

    @Builder
    public BulkRequestExtension(Integer failOnErrors, List<BulkRequestOperation> bulkRequestOperation, String test)
    {
      super(failOnErrors, bulkRequestOperation);
    }

    /**
     * override lombok builder
     */
    public static class BulkRequestExtensionBuilder extends BulkRequest.BulkRequestBuilder
    {}
  }
}
