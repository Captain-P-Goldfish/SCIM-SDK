package de.captaingoldfish.scim.sdk.common.constants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


/**
 * Test suite for {@link JavaStringUnescaper}.
 * <p>
 * These tests validate the supported escape sequences and the explicitly defined edge-case behavior of the
 * custom unescape implementation.
 * </p>
 * <p>
 * The goal is to verify the behavior required by the filter grammar and matching logic, not to replicate
 * every detail of Apache Commons Text.
 * </p>
 */
class JavaStringUnescaperTest
{

  /**
   * Verifies that {@code null} input is returned unchanged.
   * <p>
   * This ensures callers do not need to add explicit null checks.
   * </p>
   */
  @Test
  @DisplayName("Should return null when input is null")
  void shouldReturnNull()
  {
    Assertions.assertNull(JavaStringUnescaper.unescapeJava(null));
  }

  /**
   * Ensures that strings without escape sequences remain unchanged.
   */
  @Test
  @DisplayName("Should return unchanged string when no escape sequences are present")
  void shouldReturnUnchangedString()
  {
    String input = "hello world";

    String result = JavaStringUnescaper.unescapeJava(input);

    Assertions.assertEquals("hello world", result);
  }

  /**
   * Validates common control character escapes.
   */
  @Test
  @DisplayName("Should correctly unescape common control sequences")
  void shouldUnescapeControlSequences()
  {
    Assertions.assertEquals("hello\nworld", JavaStringUnescaper.unescapeJava("hello\\nworld"));
    Assertions.assertEquals("a\tb", JavaStringUnescaper.unescapeJava("a\\tb"));
    Assertions.assertEquals("line1\rline2", JavaStringUnescaper.unescapeJava("line1\\rline2"));
  }

  /**
   * Validates less frequently used control characters.
   */
  @Test
  @DisplayName("Should correctly unescape backspace and form feed")
  void shouldUnescapeBackspaceAndFormFeed()
  {
    Assertions.assertEquals("a\bb", JavaStringUnescaper.unescapeJava("a\\bb"));
    Assertions.assertEquals("a\fb", JavaStringUnescaper.unescapeJava("a\\fb"));
  }

  /**
   * Ensures correct handling of quotes and backslashes.
   */
  @Test
  @DisplayName("Should correctly unescape quotes and backslash")
  void shouldUnescapeQuotesAndBackslash()
  {
    Assertions.assertEquals("\"test\"", JavaStringUnescaper.unescapeJava("\\\"test\\\""));
    Assertions.assertEquals("'", JavaStringUnescaper.unescapeJava("\\'"));
    Assertions.assertEquals("\\", JavaStringUnescaper.unescapeJava("\\\\"));
  }

  /**
   * Ensures JSON-style escaped forward slashes are supported.
   */
  @Test
  @DisplayName("Should correctly unescape forward slash")
  void shouldUnescapeForwardSlash()
  {
    Assertions.assertEquals("foo/bar", JavaStringUnescaper.unescapeJava("foo\\/bar"));
    Assertions.assertEquals("/", JavaStringUnescaper.unescapeJava("\\/"));
  }

  /**
   * Verifies correct decoding of unicode escape sequences.
   */
  @Test
  @DisplayName("Should correctly unescape unicode sequences")
  void shouldUnescapeUnicode()
  {
    Assertions.assertEquals("A", JavaStringUnescaper.unescapeJava("\\u0041"));
    Assertions.assertEquals("ö", JavaStringUnescaper.unescapeJava("\\u00F6"));
    Assertions.assertEquals("!", JavaStringUnescaper.unescapeJava("\\u0021"));
  }

  /**
   * Ensures incomplete unicode escapes fail fast.
   */
  @Test
  @DisplayName("Should throw exception on incomplete unicode escape sequence")
  void shouldThrowOnIncompleteUnicode()
  {
    IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                                                                 () -> JavaStringUnescaper.unescapeJava("\\u12"));

    Assertions.assertTrue(exception.getMessage().contains("Incomplete unicode"));
  }

  /**
   * Ensures invalid unicode escapes fail fast.
   */
  @Test
  @DisplayName("Should throw exception on invalid unicode escape sequence")
  void shouldThrowOnInvalidUnicode()
  {
    IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                                                                 () -> JavaStringUnescaper.unescapeJava("\\uZZZZ"));

    Assertions.assertTrue(exception.getMessage().contains("Invalid unicode"));
  }

  /**
   * Verifies that unknown escape sequences are preserved.
   */
  @Test
  @DisplayName("Should preserve unknown escape sequences as-is")
  void shouldPreserveUnknownEscapes()
  {
    Assertions.assertEquals("\\q", JavaStringUnescaper.unescapeJava("\\q"));
    Assertions.assertEquals("\\x", JavaStringUnescaper.unescapeJava("\\x"));
  }

  /**
   * Ensures trailing backslashes are preserved.
   */
  @Test
  @DisplayName("Should preserve trailing backslash")
  void shouldPreserveTrailingBackslash()
  {
    Assertions.assertEquals("test\\", JavaStringUnescaper.unescapeJava("test\\"));
  }

  /**
   * Validates mixed escape usage in a realistic input.
   */
  @Test
  @DisplayName("Should handle mixed content with multiple escape types")
  void shouldHandleMixedContent()
  {
    String input = "Hello\\nWorld\\t\\u0021 \\\"test\\\" foo\\/bar";

    String result = JavaStringUnescaper.unescapeJava(input);

    Assertions.assertEquals("Hello\nWorld\t! \"test\" foo/bar", result);
  }

  /**
   * Verifies SCIM filter-style usage: escaped quotes are correctly converted before matching.
   */
  @Test
  @DisplayName("Should unescape filter-style quoted content for matching")
  void shouldUnescapeFilterStyleQuotedContentForMatching()
  {
    String input = "This is \\\"test\\\" user";

    String result = JavaStringUnescaper.unescapeJava(input);

    Assertions.assertEquals("This is \"test\" user", result);
  }
}
