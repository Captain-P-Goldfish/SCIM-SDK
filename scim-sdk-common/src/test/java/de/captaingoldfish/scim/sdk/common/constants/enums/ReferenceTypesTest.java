package de.captaingoldfish.scim.sdk.common.constants.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Pascal Knueppel
 * @since 02.01.2021
 */
public class ReferenceTypesTest
{

  @Test
  public void testGetReferenceTypeByValue()
  {
    Assertions.assertEquals(ReferenceTypes.EXTERNAL, ReferenceTypes.getByValue(ReferenceTypes.EXTERNAL.getValue()));
    Assertions.assertEquals(ReferenceTypes.EXTERNAL, ReferenceTypes.getByValue("external"));
    Assertions.assertEquals(ReferenceTypes.EXTERNAL, ReferenceTypes.getByValue("EXTERNAL"));

    Assertions.assertEquals(ReferenceTypes.RESOURCE, ReferenceTypes.getByValue(ReferenceTypes.RESOURCE.getValue()));
    Assertions.assertEquals(ReferenceTypes.RESOURCE, ReferenceTypes.getByValue("resource"));
    Assertions.assertEquals(ReferenceTypes.RESOURCE, ReferenceTypes.getByValue("RESOURCE"));

    Assertions.assertEquals(ReferenceTypes.URI, ReferenceTypes.getByValue(ReferenceTypes.URI.getValue()));
    Assertions.assertEquals(ReferenceTypes.URI, ReferenceTypes.getByValue("uri"));
    Assertions.assertEquals(ReferenceTypes.URI, ReferenceTypes.getByValue("URI"));
  }
}
