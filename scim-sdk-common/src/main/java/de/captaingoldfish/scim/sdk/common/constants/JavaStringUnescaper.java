package de.captaingoldfish.scim.sdk.common.constants;

/**
 * Utility class for unescaping Java/JSON-style escape sequences within strings.
 * <p>
 * This implementation is intentionally lightweight and independent of external libraries. It is designed for
 * use cases such as SCIM filter parsing, where quoted string values may contain escaped characters that must
 * be converted back into their literal form before comparison.
 * </p>
 * <p>
 * The following escape sequences are supported:
 * </p>
 * <ul>
 * <li>{@code \b} -> backspace</li>
 * <li>{@code \t} -> tab</li>
 * <li>{@code \n} -> newline</li>
 * <li>{@code \f} -> form feed</li>
 * <li>{@code \r} -> carriage return</li>
 * <li>{@code \"} -> double quote</li>
 * <li>{@code \'} -> single quote</li>
 * <li>{@code \\} -> backslash</li>
 * <li>{@code \/} -> forward slash</li>
 * <li>{@code \\uXXXX} -> unicode escape with exactly four hexadecimal digits</li>
 * </ul>
 * <p>
 * Unknown escape sequences (for example {@code \x}) are preserved as-is to avoid unintended data loss. A
 * trailing backslash is also preserved literally.
 * </p>
 * <p>
 * This class does not aim to be a full drop-in replacement for {@code StringEscapeUtils.unescapeJava(...)}.
 * Instead, it deliberately supports the escape sequences required by the filter grammar and a few closely
 * related variants that are commonly expected by developers.
 * </p>
 */
public final class JavaStringUnescaper
{

  private JavaStringUnescaper()
  {
    // Utility class
  }

  /**
   * Unescapes supported Java/JSON-style escape sequences in the given input string.
   * <p>
   * If the input is {@code null}, this method returns {@code null}.
   * </p>
   * <p>
   * Supported examples:
   * </p>
   *
   * <pre>
   * {@code
   * unescapeJava("hello\\nworld")   -> "hello\nworld"
   * unescapeJava("\\\"test\\\"")    -> "\"test\""
   * unescapeJava("foo\\/bar")       -> "foo/bar"
   * unescapeJava("\\u0041")         -> "A"
   * }
   * </pre>
   *
   * @param input the input string that may contain escape sequences
   * @return the unescaped string, or {@code null} if the input is {@code null}
   * @throws IllegalArgumentException if an incomplete or invalid unicode escape sequence is encountered
   */
  public static String unescapeJava(String input)
  {
    // Preserve null semantics so callers do not need an additional null check.
    if (input == null)
    {
      return null;
    }

    // Pre-size the builder to roughly the input length to reduce resizing overhead.
    StringBuilder result = new StringBuilder(input.length());

    // Walk through the input one character at a time.
    for ( int i = 0 ; i < input.length() ; i++ )
    {
      char current = input.charAt(i);

      // Fast path for ordinary characters: append directly.
      if (current != '\\')
      {
        result.append(current);
        continue;
      }

      // A trailing backslash cannot form a valid escape sequence.
      // We preserve it literally instead of throwing an exception.
      if (i + 1 >= input.length())
      {
        result.append('\\');
        break;
      }

      // Consume the next character to determine the escape sequence.
      char next = input.charAt(++i);

      switch (next)
      {
        case 'b':
          result.append('\b'); // backspace
          break;

        case 't':
          result.append('\t'); // horizontal tab
          break;

        case 'n':
          result.append('\n'); // newline
          break;

        case 'f':
          result.append('\f'); // form feed
          break;

        case 'r':
          result.append('\r'); // carriage return
          break;

        case '"':
          result.append('\"'); // escaped double quote
          break;

        case '\'':
          result.append('\''); // escaped single quote
          break;

        case '\\':
          result.append('\\'); // escaped backslash
          break;

        case '/':
          result.append('/'); // escaped forward slash (JSON-style)
          break;

        case 'u':
          // Unicode escape sequence: \\uXXXX
          // Exactly four hexadecimal digits must follow.
          if (i + 4 >= input.length())
          {
            throw new IllegalArgumentException("Incomplete unicode escape sequence at index " + (i - 1));
          }

          String hex = input.substring(i + 1, i + 5);

          try
          {
            int codePoint = Integer.parseInt(hex, 16);
            result.append((char)codePoint);
          }
          catch (NumberFormatException ex)
          {
            throw new IllegalArgumentException("Invalid unicode escape sequence: \\u" + hex, ex);
          }

          // Skip the four hex digits because they were already consumed.
          i += 4;
          break;

        default:
          // Preserve unknown escape sequences literally.
          // Example: "\x" remains "\x".
          result.append('\\').append(next);
          break;
      }
    }

    return result.toString();
  }
}
