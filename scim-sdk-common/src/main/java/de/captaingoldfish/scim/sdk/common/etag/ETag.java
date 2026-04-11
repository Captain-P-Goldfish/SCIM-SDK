package de.captaingoldfish.scim.sdk.common.etag;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimTextNode;
import lombok.Builder;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 19.11.2019 - 20:03 <br>
 * <br>
 * this class is used as etag representation
 */
@Getter
public class ETag extends ScimTextNode
{

  public static final String WEAK_IDENTIFIER = "W/";

  /**
   * tells us if this representation is a weak etag or not
   */
  private boolean weak;

  /**
   * the string character representation for etag
   */
  private String tag;

  @Builder
  public ETag(Boolean weak, String tag)
  {
    super(null, getEntityTag(weak == null ? true : weak, tag));
    if (Strings.CS.contains(tag, "\""))
    {
      throw new BadRequestException("Please omit the quotes in the entity tag value '" + tag + "'", null,
                                    ScimType.RFC7644.INVALID_VALUE);
    }
    this.weak = weak == null ? true : weak;
    this.tag = tag;
  }

  /**
   * a creation method especially used with the method
   * {@link de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode#getStringAttribute(String, Class)}
   *
   * @param version an eTag that should be parsed
   * @return a new ETag instance
   */
  public static ETag newInstance(String version)
  {
    return parseETag(version);
  }

  /**
   * checks the given string and parses it into an entity tag
   *
   * @param version the version string
   * @return the entity tag instance representation
   */
  public static ETag parseETag(String version)
  {
    int numberOfQuotes = StringUtils.countMatches(version, "\"");
    if (numberOfQuotes != 0 && numberOfQuotes != 2)
    {
      throw new BadRequestException("invalid entity tag value. Value was '" + version + "'. Value has a irregular "
                                    + "number of quotes '" + numberOfQuotes + "'. Please take a look into "
                                    + "RFC7232 for more detailed information of entity tags", null,
                                    ScimType.RFC7644.INVALID_VALUE);
    }
    boolean weak = Strings.CS.startsWith(version, WEAK_IDENTIFIER) || !Strings.CS.startsWith(version, "\"");
    String tag = Optional.ofNullable(StringUtils.stripToNull(version))
                         .map(s -> s.replaceFirst("^(" + WEAK_IDENTIFIER + ")?\"(.*?)\"", "$2"))
                         .orElse(null);
    return ETag.builder().weak(weak).tag(StringUtils.isBlank(tag) ? null : tag).build();
  }

  /**
   * workaround method to put the entity tag directly into the constructor of the superclass
   *
   * @return the entity tag that is represented by this instance
   */
  private static String getEntityTag(boolean weak, String tag)
  {
    final String eTag = "\"" + StringUtils.stripToEmpty(tag) + "\"";
    return Optional.ofNullable(tag)
                   .map(s -> weak ? WEAK_IDENTIFIER + eTag : eTag)
                   .orElse(weak ? WEAK_IDENTIFIER + "\"\"" : "\"\"");
  }

  /**
   * @return the entity tag that is represented by this instance
   */
  public String getEntityTag()
  {
    return textValue();
  }

  @Override
  public String toString()
  {
    return getEntityTag();
  }

  public String toPrettyString()
  {
    return getEntityTag();
  }

  /**
   * comparison of ETag's must be done due to the following rules
   *
   * <pre>
   *    +--------+--------+-------------------+-----------------+
   *    | ETag 1 | ETag 2 | Strong Comparison | Weak Comparison |
   *    +--------+--------+-------------------+-----------------+
   *    | W/"1"  | W/"1"  | no match          | match           |
   *    | W/"1"  | W/"2"  | no match          | no match        |
   *    | W/"1"  | "1"    | no match          | match           |
   *    | "1"    | "1"    | match             | match           |
   *    +--------+--------+-------------------+-----------------+
   * </pre>
   */
  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof ETag))
    {
      return false;
    }
    ETag other = (ETag)o;
    if (isWeak())
    {
      return tag.equals(other.getTag());
    }
    else
    {
      return getEntityTag().equals(other.getEntityTag());
    }
  }

  /**
   * override lombok builder with public constructor
   */
  public static class ETagBuilder
  {

    public ETagBuilder()
    {}
  }
}
