package de.gold.scim.resources.multicomplex;

import java.util.Optional;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.resources.base.ScimObjectNode;


// @formatter:off
/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 13:01 <br>
 * <br>
 *
 * Multi-valued attributes contain a list of elements using the JSON
 * array format defined in Section 5 of [RFC7159].  Elements can be
 * either of the following:
 *
 * o  primitive values, or
 *
 * o  objects with a set of sub-attributes and values, using the JSON
 *    object format defined in Section 4 of [RFC7159], in which case
 *    they SHALL be considered to be complex attributes.  As with
 *    complex attributes, the order of sub-attributes is not
 *    significant.  The predefined sub-attributes listed in this section
 *    can be used with multi-valued attribute objects, but these
 *    sub-attributes MUST be used with the meanings defined here.
 *
 * If not otherwise defined, the default set of sub-attributes for a
 * multi-valued attribute is as follows:
 *
 *    type
 *       A label indicating the attribute's function, e.g., "work" or
 *       "home".
 *
 *    primary
 *       A Boolean value indicating the 'primary' or preferred attribute
 *       value for this attribute, e.g., the preferred mailing address or
 *       the primary email address.  The primary attribute value "true"
 *       MUST appear no more than once.  If not specified, the value of
 *       "primary" SHALL be assumed to be "false".
 *
 *    display
 *       A human-readable name, primarily used for display purposes and
 *       having a mutability of "immutable".
 *
 *    value
 *       The attribute's significant value, e.g., email address, phone
 *       number.
 *
 *    $ref
 *       The reference URI of a target resource, if the attribute is a
 *       reference.  URIs are canonicalized per Section 6.2 of [RFC3986].
 *       While the representation of a resource may vary in different SCIM
 *       protocol API versions (see Section 3.13 of [RFC7644]), URIs for
 *       SCIM resources with an API version SHALL be considered comparable
 *       to URIs without a version or with a different version.  For
 *       example, "https://example.com/Users/12345" is equivalent to
 *       "https://example.com/v2/Users/12345".
 *
 * When returning multi-valued attributes, service providers SHOULD
 * canonicalize the value returned (e.g., by returning a value for the
 * sub-attribute "type", such as "home" or "work") when appropriate
 * (e.g., for email addresses and URLs).
 *
 * Service providers MAY return element objects with the same "value"
 * sub-attribute more than once with a different "type" sub-attribute
 * (e.g., the same email address may be used for work and home) but
 * SHOULD NOT return the same (type, value) combination more than once
 * per attribute, as this complicates processing by the client.
 *
 * When defining schema for multi-valued attributes, it is considered a
 * good practice to provide a type attribute that MAY be used for the
 * purpose of canonicalization of values.  In the schema definition for
 * an attribute, the service provider MAY define the recommended
 * canonical values (see Section 7).
 */
// @formatter:on
public class MultiComplexNode extends ScimObjectNode
{

  protected MultiComplexNode()
  {
    super(null);
  }

  public MultiComplexNode(String type, Boolean primary, String display, String value, String ref)
  {
    this();
    setType(type);
    setPrimary(primary);
    setDisplay(display);
    setValue(value);
    setRef(ref);
  }

  /**
   * A label indicating the attribute's function, e.g., "work" or "home".
   */
  public Optional<String> getType()
  {
    return getStringAttribute(AttributeNames.RFC7643.TYPE);
  }

  /**
   * A label indicating the attribute's function, e.g., "work" or "home".
   */
  public void setType(String type)
  {
    setAttribute(AttributeNames.RFC7643.TYPE, type);
  }

  /**
   * A Boolean value indicating the 'primary' or preferred attribute value for this attribute, e.g., the
   * preferred mailing address or the primary email address. The primary attribute value "true" MUST appear no
   * more than once. If not specified, the value of "primary" SHALL be assumed to be "false".
   */
  public boolean isPrimary()
  {
    return getBooleanAttribute(AttributeNames.RFC7643.PRIMARY).orElse(false);
  }

  /**
   * A Boolean value indicating the 'primary' or preferred attribute value for this attribute, e.g., the
   * preferred mailing address or the primary email address. The primary attribute value "true" MUST appear no
   * more than once. If not specified, the value of "primary" SHALL be assumed to be "false".
   */
  public void setPrimary(Boolean primary)
  {
    setAttribute(AttributeNames.RFC7643.PRIMARY, primary);
  }

  /**
   * A human-readable name, primarily used for display purposes and having a mutability of "immutable".
   */
  public Optional<String> getDisplay()
  {
    return getStringAttribute(AttributeNames.RFC7643.DISPLAY);
  }

  /**
   * A human-readable name, primarily used for display purposes and having a mutability of "immutable".
   */
  public void setDisplay(String display)
  {
    setAttribute(AttributeNames.RFC7643.DISPLAY, display);
  }

  /**
   * The attribute's significant value, e.g., email address, phone number.
   */
  public Optional<String> getValue()
  {
    return getStringAttribute(AttributeNames.RFC7643.VALUE);
  }

  /**
   * The attribute's significant value, e.g., email address, phone number.
   */
  public void setValue(String value)
  {
    setAttribute(AttributeNames.RFC7643.VALUE, value);
  }

  /**
   * The reference URI of a target resource, if the attribute is a reference. URIs are canonicalized per Section
   * 6.2 of [RFC3986]. While the representation of a resource may vary in different SCIM protocol API versions
   * (see Section 3.13 of [RFC7644]), URIs for SCIM resources with an API version SHALL be considered comparable
   * to URIs without a version or with a different version. For example, "https://example.com/Users/12345" is
   * equivalent to "https://example.com/v2/Users/12345".
   */
  public Optional<String> getRef()
  {
    return getStringAttribute(AttributeNames.RFC7643.REF);
  }

  /**
   * The reference URI of a target resource, if the attribute is a reference. URIs are canonicalized per Section
   * 6.2 of [RFC3986]. While the representation of a resource may vary in different SCIM protocol API versions
   * (see Section 3.13 of [RFC7644]), URIs for SCIM resources with an API version SHALL be considered comparable
   * to URIs without a version or with a different version. For example, "https://example.com/Users/12345" is
   * equivalent to "https://example.com/v2/Users/12345".
   */
  public void setRef(String ref)
  {
    setAttribute(AttributeNames.RFC7643.REF, ref);
  }

}
