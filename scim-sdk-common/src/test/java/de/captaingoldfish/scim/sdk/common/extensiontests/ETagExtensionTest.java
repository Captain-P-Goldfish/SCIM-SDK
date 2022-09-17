package de.captaingoldfish.scim.sdk.common.extensiontests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.etag.ETag;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 17.09.2022 - 12:34 <br>
 * <br>
 */
public class ETagExtensionTest
{

  @Test
  public void testExtendObject()
  {
    Assertions.assertDoesNotThrow(() -> ETagExtension.builder().weak(true).tag("1").testValue("hello").build());
  }

  public static class ETagExtension extends ETag
  {

    @Builder
    public ETagExtension(Boolean weak, String tag, String testValue)
    {
      super(weak, tag);
    }

    /**
     * override lombok builder
     */
    public static class ETagExtensionBuilder extends ETag.ETagBuilder
    {}
  }
}
