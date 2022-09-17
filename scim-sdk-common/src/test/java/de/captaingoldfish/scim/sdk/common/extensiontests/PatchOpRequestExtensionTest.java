package de.captaingoldfish.scim.sdk.common.extensiontests;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 17.09.2022 - 12:51 <br>
 * <br>
 */
public class PatchOpRequestExtensionTest
{

  @Test
  public void testExtendObject()
  {
    Assertions.assertDoesNotThrow(() -> PatchOpRequestExtension.builder()
                                                               .operations(Collections.emptyList())
                                                               .test("hello")
                                                               .build());
  }

  public static class PatchOpRequestExtension extends PatchOpRequest
  {

    @Builder
    public PatchOpRequestExtension(List<PatchRequestOperation> operations, String test)
    {
      super(operations);
    }

    /**
     * override lombok builder
     */
    public static class PatchOpRequestExtensionBuilder extends PatchOpRequest.PatchOpRequestBuilder
    {}
  }
}
