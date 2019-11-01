package de.gold.scim.server.filter.antlr;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.gold.scim.common.constants.SchemaUris;
import lombok.EqualsAndHashCode;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 12:24 <br>
 * <br>
 * represents the name of an attribute and will also hold its meta data
 */
@Getter
@EqualsAndHashCode
public class FilterAttributeName
{

  /**
   * the resource uri to which this attribute belongs e.g. {@link SchemaUris#ENTERPRISE_USER_URI}
   */
  private String resourceUri;

  private String attributeName;

  /**
   * the fully qualified name of this attribute {@link #resourceUri} + {@link #getShortName()}
   */
  private String fullName;

  /**
   * the name of the parent attribute in case of bracket filter notation
   */
  private String parentAttributeName;

  /**
   * the name of the sub attribute in case of bracket filter notation
   */
  private String complexSubAttributeName;

  public FilterAttributeName(ScimFilterParser.ValuePathContext valuePathContext,
                             ScimFilterParser.AttributePathContext attributePathContext)
  {
    this(valuePathContext == null ? null : valuePathContext.attributePath().attribute.getText(), attributePathContext);
  }

  public FilterAttributeName(String parentName, ScimFilterParser.AttributePathContext attributePathContext)
  {
    this.parentAttributeName = parentName == null ? null : (parentName + ".");
    this.attributeName = attributePathContext.attribute.getText();
    this.complexSubAttributeName = attributePathContext.subattribute == null ? null
      : attributePathContext.subattribute.getText();
    this.resourceUri = resolveResourceUri(attributePathContext).orElse(null);
    this.fullName = (resourceUri == null ? "" : StringUtils.stripToEmpty(resourceUri) + ":") + getShortName();
  }

  public FilterAttributeName(String attributeName)
  {
    Pattern pattern = Pattern.compile("(([\\w:.]+):)?(\\w+)(\\.)?(\\w+)?");
    Matcher matcher = pattern.matcher(attributeName);
    if (matcher.matches())
    {
      this.resourceUri = matcher.group(2);
      this.attributeName = matcher.group(3);
      this.complexSubAttributeName = matcher.group(5);
    }
    this.fullName = attributeName;
  }

  /**
   * tries to resolve the resourceUri value
   *
   * @param attributePathContext the antlr attribute path context
   * @return the resourceUri
   */
  private Optional<String> resolveResourceUri(ScimFilterParser.AttributePathContext attributePathContext)
  {
    if (attributePathContext.resourceUri == null)
    {
      return Optional.empty();
    }
    String resourceUri = StringUtils.stripToEmpty(attributePathContext.resourceUri.getText());
    return Optional.of(StringUtils.stripToNull(resourceUri.replaceFirst(":$", "")));
  }

  /**
   * the short name of the attribute e.g. 'userName' or 'name.givenName'
   */
  public String getShortName()
  {
    return StringUtils.stripToNull(StringUtils.stripToEmpty(parentAttributeName)
                                   + StringUtils.stripToEmpty(attributeName)
                                   + StringUtils.stripToEmpty(this.complexSubAttributeName == null ? null
                                     : "." + this.complexSubAttributeName));
  }

  @Override
  public String toString()
  {
    return fullName;
  }
}
