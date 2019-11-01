package de.gold.scim.server.resources.complex;

import java.util.Optional;

import de.gold.scim.server.constants.AttributeNames;
import de.gold.scim.server.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 12:36 <br>
 * <br>
 * The user's manager. A complex type that optionally allows service providers to represent organizational
 * hierarchy by referencing the "id" attribute of another User.
 */
public class Manager extends ScimObjectNode
{

  public Manager()
  {
    super(null);
  }

  @Builder
  public Manager(String value, String displayName, String ref)
  {
    this();
    setValue(value);
    setDisplayName(displayName);
    setRef(ref);
  }

  /**
   * The displayName of the user's manager. This attribute is OPTIONAL, and mutability is "readOnly".
   */
  public Optional<String> getDisplayName()
  {
    return getStringAttribute(AttributeNames.RFC7643.DISPLAY_NAME);
  }

  /**
   * The displayName of the user's manager. This attribute is OPTIONAL, and mutability is "readOnly".
   */
  public void setDisplayName(String displayName)
  {
    setAttribute(AttributeNames.RFC7643.DISPLAY_NAME, displayName);
  }

  /**
   * The "id" of the SCIM resource representing the user's manager. RECOMMENDED.
   */
  public Optional<String> getValue()
  {
    return getStringAttribute(AttributeNames.RFC7643.VALUE);
  }

  /**
   * The "id" of the SCIM resource representing the user's manager. RECOMMENDED.
   */
  public void setValue(String value)
  {
    setAttribute(AttributeNames.RFC7643.VALUE, value);
  }

  /**
   * $ref The URI of the SCIM resource representing the User's manager. RECOMMENDED.
   */
  public Optional<String> getRef()
  {
    return getStringAttribute(AttributeNames.RFC7643.REF);
  }

  /**
   * $ref The URI of the SCIM resource representing the User's manager. RECOMMENDED.
   */
  public void setRef(String ref)
  {
    setAttribute(AttributeNames.RFC7643.REF, ref);
  }
}
