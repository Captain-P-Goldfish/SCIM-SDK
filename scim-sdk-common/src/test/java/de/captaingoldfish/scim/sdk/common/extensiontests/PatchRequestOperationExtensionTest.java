package de.captaingoldfish.scim.sdk.common.extensiontests;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 17.09.2022 - 12:54 <br>
 * <br>
 */
public class PatchRequestOperationExtensionTest
{

  @Test
  public void testExtendObject()
  {
    Assertions.assertDoesNotThrow(() -> PatchRequestOperationExtension.builder().path("/test").test("hello").build());
  }

  public static class PatchRequestOperationExtension extends PatchRequestOperation
  {

    @Builder
    public PatchRequestOperationExtension(String path, PatchOp op, List<String> values, JsonNode valueNode, String test)
    {
      super(path, op, values, valueNode);
    }

    /**
     * override lombok builder
     */
    public static class PatchRequestOperationExtensionBuilder extends PatchRequestOperation.PatchRequestOperationBuilder
    {}
  }
}
