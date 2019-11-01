package de.gold.scim.server.constants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 21:56 <br>
 * <br>
 */
public class ScimTypeTest
{

  /**
   * will verify that the ScimType values defined by RFC7644 do have the correct values
   */
  @Test
  public void testValuesDefinedByScim()
  {
    Assertions.assertEquals("invalidFilter", ScimType.RFC7644.INVALID_FILTER);
    Assertions.assertEquals("tooMany", ScimType.RFC7644.TOO_MANY);
    Assertions.assertEquals("uniqueness", ScimType.RFC7644.UNIQUENESS);
    Assertions.assertEquals("mutability", ScimType.RFC7644.MUTABILITY);
    Assertions.assertEquals("invalidSyntax", ScimType.RFC7644.INVALID_SYNTAX);
    Assertions.assertEquals("invalidPath", ScimType.RFC7644.INVALID_PATH);
    Assertions.assertEquals("noTarget", ScimType.RFC7644.NO_TARGET);
    Assertions.assertEquals("invalidValue", ScimType.RFC7644.INVALID_VALUE);
    Assertions.assertEquals("invalidVers", ScimType.RFC7644.INVALID_VERSION);
    Assertions.assertEquals("sensitive", ScimType.RFC7644.SENSITIVE);
  }

  /**
   * this test will verify that the ScimTypes that were defined by this application do have the correct values
   */
  @Test
  public void testCustomDefinedValues()
  {
    Assertions.assertEquals("required", ScimType.Custom.REQUIRED);
    Assertions.assertEquals("unknownResource", ScimType.Custom.UNKNOWN_RESOURCE);
    Assertions.assertEquals("missingExtension", ScimType.Custom.MISSING_EXTENSION);
    Assertions.assertEquals("invalidParameters", ScimType.Custom.INVALID_PARAMETERS);
    Assertions.assertEquals("unparseableRequest", ScimType.Custom.UNPARSEABLE_REQUEST);
  }
}
