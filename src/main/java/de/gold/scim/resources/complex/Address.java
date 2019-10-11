package de.gold.scim.resources.complex;

import java.util.Optional;

import de.gold.scim.constants.AttributeNames;
import de.gold.scim.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 12:36 <br>
 * <br>
 * A physical mailing address for this user. Canonical type values of "work", "home", and "other". This
 * attribute is a complex type with the following sub-attributes. All sub-attributes are OPTIONAL.
 */
public class Address extends ScimObjectNode
{

  public Address()
  {
    super(null);
  }

  @Builder
  public Address(String formatted,
                 String streetAddress,
                 String locality,
                 String region,
                 String postalCode,
                 String country)
  {
    this();
    setFormatted(formatted);
    setStreetAddress(streetAddress);
    setLocality(locality);
    setRegion(region);
    setPostalCode(postalCode);
    setCountry(country);
  }

  /**
   * The full mailing address, formatted for display or use with a mailing label. This attribute MAY contain
   * newlines.
   */
  public Optional<String> getFormatted()
  {
    return getStringAttribute(AttributeNames.FORMATTED);
  }

  /**
   * The full mailing address, formatted for display or use with a mailing label. This attribute MAY contain
   * newlines.
   */
  public void setFormatted(String formatted)
  {
    setAttribute(AttributeNames.FORMATTED, formatted);
  }

  /**
   * The full street address component, which may include house number, street name, P.O. box, and multi-line
   * extended street address information. This attribute MAY contain newlines.
   */
  public Optional<String> getStreetAddress()
  {
    return getStringAttribute(AttributeNames.STREET_ADDRESS);
  }

  /**
   * The full street address component, which may include house number, street name, P.O. box, and multi-line
   * extended street address information. This attribute MAY contain newlines.
   */
  public void setStreetAddress(String streetAddress)
  {
    setAttribute(AttributeNames.STREET_ADDRESS, streetAddress);
  }

  /**
   * The city or locality component.
   */
  public Optional<String> getLocality()
  {
    return getStringAttribute(AttributeNames.LOCALITY);
  }

  /**
   * The city or locality component.
   */
  public void setLocality(String locality)
  {
    setAttribute(AttributeNames.LOCALITY, locality);
  }

  /**
   * The state or region component.
   */
  public Optional<String> getRegion()
  {
    return getStringAttribute(AttributeNames.REGION);
  }

  /**
   * The state or region component.
   */
  public void setRegion(String region)
  {
    setAttribute(AttributeNames.REGION, region);
  }

  /**
   * The zip code or postal code component.
   */
  public Optional<String> getPostalCode()
  {
    return getStringAttribute(AttributeNames.POSTAL_CODE);
  }

  /**
   * The zip code or postal code component.
   */
  public void setPostalCode(String postalCode)
  {
    setAttribute(AttributeNames.POSTAL_CODE, postalCode);
  }

  /**
   * The country name component. When specified, the value MUST be in ISO 3166-1 "alpha-2" code format
   * [ISO3166]; e.g., the United States and Sweden are "US" and "SE", respectively.
   */
  public Optional<String> getCountry()
  {
    return getStringAttribute(AttributeNames.COUNTRY);
  }

  /**
   * The country name component. When specified, the value MUST be in ISO 3166-1 "alpha-2" code format
   * [ISO3166]; e.g., the United States and Sweden are "US" and "SE", respectively.
   */
  public void setCountry(String country)
  {
    setAttribute(AttributeNames.COUNTRY, country);
  }
}
