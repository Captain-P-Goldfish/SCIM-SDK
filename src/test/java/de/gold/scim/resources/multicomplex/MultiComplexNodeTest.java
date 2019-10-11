package de.gold.scim.resources.multicomplex;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 23:49 <br>
 * <br>
 */
public class MultiComplexNodeTest
{

  /**
   * verifies that no exception is thrown on empty builder creation
   */
  @Test
  public void testUseBuilderWithoutParameters()
  {
    MultiComplexNode instance = Assertions.assertDoesNotThrow(() -> new MultiComplexNode(null, null, null, null, null));
    Assertions.assertTrue(instance.isEmpty());
  }

  /**
   * will test that a new instance has no attributes at all
   */
  @Test
  public void testCleanObjectCreation()
  {
    Assertions.assertTrue(new MultiComplexNode().isEmpty());
  }

  /**
   * will test if the attributes are correctly added into the json object
   */
  @Test
  public void testSetAndGetAttributes()
  {
    final String value = UUID.randomUUID().toString();
    final String display = UUID.randomUUID().toString();
    final boolean primary = true;
    final String type = UUID.randomUUID().toString();
    final String ref = UUID.randomUUID().toString();
    MultiComplexNode multiComplexNode = new MultiComplexNode(type, primary, display, value, ref);
    Assertions.assertEquals(value, multiComplexNode.getValue().get());
    Assertions.assertEquals(display, multiComplexNode.getDisplay().get());
    Assertions.assertEquals(primary, multiComplexNode.isPrimary());
    Assertions.assertEquals(type, multiComplexNode.getScimType().get());
    Assertions.assertEquals(ref, multiComplexNode.getRef().get());
  }

  /**
   * will test if the attributes are correctly added into the json object
   */
  @Test
  public void testSetAndGetAttributesWithoutPrimaryValue()
  {
    final String value = UUID.randomUUID().toString();
    final String display = UUID.randomUUID().toString();
    final String type = UUID.randomUUID().toString();
    final String ref = UUID.randomUUID().toString();
    MultiComplexNode multiComplexNode = new MultiComplexNode(type, null, display, value, ref);
    Assertions.assertEquals(value, multiComplexNode.getValue().get());
    Assertions.assertEquals(display, multiComplexNode.getDisplay().get());
    Assertions.assertFalse(multiComplexNode.isPrimary());
    Assertions.assertEquals(type, multiComplexNode.getScimType().get());
    Assertions.assertEquals(ref, multiComplexNode.getRef().get());
  }
}
