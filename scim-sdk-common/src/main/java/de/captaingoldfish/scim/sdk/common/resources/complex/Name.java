package de.captaingoldfish.scim.sdk.common.resources.complex;

import java.util.Optional;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 10:19 <br>
 * <br>
 * The components of the user's name. Service providers MAY return just the full name as a single string in
 * the formatted sub-attribute, or they MAY return just the individual component attributes using the other
 * sub-attributes, or they MAY return both. If both variants are returned, they SHOULD be describing the same
 * name, with the formatted name indicating how the component attributes should be combined.
 */
public class Name extends ScimObjectNode
{

  public Name()
  {
    super(null);
  }

  @Builder
  public Name(String formatted,
              String familyName,
              String givenName,
              String middlename,
              String honorificPrefix,
              String honorificSuffix)
  {
    this();
    setFormatted(formatted);
    setFamilyName(familyName);
    setGivenName(givenName);
    setMiddleName(middlename);
    setHonorificPrefix(honorificPrefix);
    setHonorificSuffix(honorificSuffix);
  }

  /**
   * The full name, including all middle names, titles, and suffixes as appropriate, formatted for display
   * (e.g., "Ms. Barbara Jane Jensen, III").
   */
  public Optional<String> getFormatted()
  {
    return getStringAttribute(AttributeNames.RFC7643.FORMATTED);
  }

  /**
   * The full name, including all middle names, titles, and suffixes as appropriate, formatted for display
   * (e.g., "Ms. Barbara Jane Jensen, III").
   */
  public void setFormatted(String formatted)
  {
    setAttribute(AttributeNames.RFC7643.FORMATTED, formatted);
  }

  /**
   * The family name of the User, or last name in most Western languages (e.g., "Jensen" given the full name
   * "Ms. Barbara Jane Jensen, III").
   */
  public Optional<String> getFamilyName()
  {
    return getStringAttribute(AttributeNames.RFC7643.FAMILY_NAME);
  }

  /**
   * The family name of the User, or last name in most Western languages (e.g., "Jensen" given the full name
   * "Ms. Barbara Jane Jensen, III").
   */
  public void setFamilyName(String familyName)
  {
    setAttribute(AttributeNames.RFC7643.FAMILY_NAME, familyName);
  }

  /**
   * The given name of the User, or first name in most Western languages (e.g., "Barbara" given the full name
   * "Ms. Barbara Jane Jensen, III").
   */
  public Optional<String> getGivenName()
  {
    return getStringAttribute(AttributeNames.RFC7643.GIVEN_NAME);
  }

  /**
   * The given name of the User, or first name in most Western languages (e.g., "Barbara" given the full name
   * "Ms. Barbara Jane Jensen, III").
   */
  public void setGivenName(String givenName)
  {
    setAttribute(AttributeNames.RFC7643.GIVEN_NAME, givenName);
  }

  /**
   * The middle name(s) of the User (e.g., "Jane" given the full name "Ms. Barbara Jane Jensen, III").
   */
  public Optional<String> getMiddleName()
  {
    return getStringAttribute(AttributeNames.RFC7643.MIDDLE_NAME);
  }

  /**
   * The middle name(s) of the User (e.g., "Jane" given the full name "Ms. Barbara Jane Jensen, III").
   */
  public void setMiddleName(String middleName)
  {
    setAttribute(AttributeNames.RFC7643.MIDDLE_NAME, middleName);
  }

  /**
   * The honorific prefix(es) of the User, or title in most Western languages (e.g., "Ms." given the full name
   * "Ms. Barbara Jane Jensen, III").
   */
  public Optional<String> getHonorificPrefix()
  {
    return getStringAttribute(AttributeNames.RFC7643.HONORIFIC_PREFIX);
  }

  /**
   * The honorific prefix(es) of the User, or title in most Western languages (e.g., "Ms." given the full name
   * "Ms. Barbara Jane Jensen, III").
   */
  public void setHonorificPrefix(String honorificPrefix)
  {
    setAttribute(AttributeNames.RFC7643.HONORIFIC_PREFIX, honorificPrefix);
  }

  /**
   * The honorific suffix(es) of the User, or suffix in most Western languages (e.g., "III" given the full name
   * "Ms. Barbara Jane Jensen, III").
   */
  public Optional<String> getHonorificSuffix()
  {
    return getStringAttribute(AttributeNames.RFC7643.HONORIFIC_SUFFIX);
  }

  /**
   * The honorific suffix(es) of the User, or suffix in most Western languages (e.g., "III" given the full name
   * "Ms. Barbara Jane Jensen, III").
   */
  public void setHonorificSuffix(String honorificSuffix)
  {
    setAttribute(AttributeNames.RFC7643.HONORIFIC_SUFFIX, honorificSuffix);
  }

}
