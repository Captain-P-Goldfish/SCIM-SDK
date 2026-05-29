package de.captaingoldfish.scim.sdk.common.resources.complex;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;


/**
 * Unit tests for {@link PaginationConfig}. See <a href="https://www.rfc-editor.org/rfc/rfc9865.html">RFC
 * 9865</a>.
 */
public class PaginationConfigTest
{

  /**
   * A freshly built instance is non-empty and exposes RFC 9865 defaults: cursor disabled, index enabled.
   */
  @Test
  public void testNewCreatedInstanceHoldsDefaults()
  {
    PaginationConfig paginationConfig = PaginationConfig.builder().build();
    MatcherAssert.assertThat(paginationConfig, Matchers.not(Matchers.emptyIterable()));
    Assertions.assertFalse(paginationConfig.isCursor());
    Assertions.assertTrue(paginationConfig.isIndex());
    Assertions.assertFalse(paginationConfig.getDefaultPaginationMethod().isPresent());
    Assertions.assertFalse(paginationConfig.getDefaultPageSize().isPresent());
    Assertions.assertFalse(paginationConfig.getMaxPageSize().isPresent());
    Assertions.assertFalse(paginationConfig.getCursorTimeout().isPresent());
  }

  /**
   * The default-arg constructor also enforces the backwards-compatible defaults (cursor=false, index=true).
   */
  @Test
  public void testNoArgConstructorHoldsDefaults()
  {
    PaginationConfig paginationConfig = new PaginationConfig();
    Assertions.assertFalse(paginationConfig.isCursor());
    Assertions.assertTrue(paginationConfig.isIndex());
  }

  /**
   * Reading getters when the underlying attributes have been removed must still return the documented defaults.
   */
  @Test
  public void testGetterDefaultsWhenAttributesRemoved()
  {
    PaginationConfig paginationConfig = PaginationConfig.builder().build();
    paginationConfig.remove(AttributeNames.RFC7643.CURSOR);
    paginationConfig.remove(AttributeNames.RFC7643.INDEX);
    Assertions.assertFalse(paginationConfig.isCursor());
    Assertions.assertTrue(paginationConfig.isIndex());
  }

  /**
   * Setters drive the JSON representation and the getters reflect the new values.
   */
  @Test
  public void testSetterMethods()
  {
    PaginationConfig paginationConfig = PaginationConfig.builder().build();
    paginationConfig.setCursor(true);
    paginationConfig.setIndex(false);
    paginationConfig.setDefaultPaginationMethod(PaginationConfig.METHOD_CURSOR);
    paginationConfig.setDefaultPageSize(50);
    paginationConfig.setMaxPageSize(500);
    paginationConfig.setCursorTimeout(120);

    Assertions.assertTrue(paginationConfig.isCursor());
    Assertions.assertFalse(paginationConfig.isIndex());
    Assertions.assertEquals(PaginationConfig.METHOD_CURSOR, paginationConfig.getDefaultPaginationMethod().orElse(null));
    Assertions.assertEquals(50, paginationConfig.getDefaultPageSize().orElse(0).intValue());
    Assertions.assertEquals(500, paginationConfig.getMaxPageSize().orElse(0).intValue());
    Assertions.assertEquals(120, paginationConfig.getCursorTimeout().orElse(0).intValue());
  }

  /**
   * Setting an integer attribute to {@code null} removes it from the JSON representation so the OPTIONAL
   * attribute is genuinely absent on the wire.
   */
  @Test
  public void testIntegerSettersWithNullRemoveAttribute()
  {
    PaginationConfig paginationConfig = PaginationConfig.builder()
                                                        .defaultPageSize(10)
                                                        .maxPageSize(100)
                                                        .cursorTimeout(60)
                                                        .build();
    paginationConfig.setDefaultPageSize(null);
    paginationConfig.setMaxPageSize(null);
    paginationConfig.setCursorTimeout(null);
    Assertions.assertFalse(paginationConfig.getDefaultPageSize().isPresent());
    Assertions.assertFalse(paginationConfig.getMaxPageSize().isPresent());
    Assertions.assertFalse(paginationConfig.getCursorTimeout().isPresent());
    Assertions.assertFalse(paginationConfig.has(AttributeNames.RFC7643.DEFAULT_PAGE_SIZE));
    Assertions.assertFalse(paginationConfig.has(AttributeNames.RFC7643.MAX_PAGE_SIZE));
    Assertions.assertFalse(paginationConfig.has(AttributeNames.RFC7643.CURSOR_TIMEOUT));
  }

  /**
   * The builder accepts the full set of RFC 9865 fields and produces an equivalent config.
   */
  @Test
  public void testBuilderParameterSet()
  {
    PaginationConfig paginationConfig = PaginationConfig.builder()
                                                        .cursor(true)
                                                        .index(true)
                                                        .defaultPaginationMethod(PaginationConfig.METHOD_INDEX)
                                                        .defaultPageSize(25)
                                                        .maxPageSize(250)
                                                        .cursorTimeout(3600)
                                                        .build();
    Assertions.assertTrue(paginationConfig.isCursor());
    Assertions.assertTrue(paginationConfig.isIndex());
    Assertions.assertEquals(PaginationConfig.METHOD_INDEX, paginationConfig.getDefaultPaginationMethod().orElse(null));
    Assertions.assertEquals(25, paginationConfig.getDefaultPageSize().orElse(0).intValue());
    Assertions.assertEquals(250, paginationConfig.getMaxPageSize().orElse(0).intValue());
    Assertions.assertEquals(3600, paginationConfig.getCursorTimeout().orElse(0).intValue());
  }
}
