package de.gold.scim.common.resources.multicomplex;

import java.util.Optional;

import de.gold.scim.common.constants.AttributeNames;
import lombok.Builder;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 11.10.2019 - 12:36 <br>
 * <br>
 * A physical mailing address for this user. Canonical type values of "work", "home", and "other". This
 * attribute is a complex type with the following sub-attributes. All sub-attributes are OPTIONAL.
 */
@NoArgsConstructor
public class Address extends MultiComplexNode
{

  @Builder
  public Address(String type,
                 Boolean primary,
                 String display,
                 String value,
                 String ref,
                 String formatted,
                 String streetAddress,
                 String locality,
                 String region,
                 String postalCode,
                 String country)
  {
    super(type, primary, display, value, ref);
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
    return getStringAttribute(AttributeNames.RFC7643.FORMATTED);
  }

  /**
   * The full mailing address, formatted for display or use with a mailing label. This attribute MAY contain
   * newlines.
   */
  public void setFormatted(String formatted)
  {
    setAttribute(AttributeNames.RFC7643.FORMATTED, formatted);
  }

  /**
   * The full street address component, which may include house number, street name, P.O. box, and multi-line
   * extended street address information. This attribute MAY contain newlines.
   */
  public Optional<String> getStreetAddress()
  {
    return getStringAttribute(AttributeNames.RFC7643.STREET_ADDRESS);
  }

  /**
   * The full street address component, which may include house number, street name, P.O. box, and multi-line
   * extended street address information. This attribute MAY contain newlines.
   */
  public void setStreetAddress(String streetAddress)
  {
    setAttribute(AttributeNames.RFC7643.STREET_ADDRESS, streetAddress);
  }

  /**
   * The city or locality component.
   */
  public Optional<String> getLocality()
  {
    return getStringAttribute(AttributeNames.RFC7643.LOCALITY);
  }

  /**
   * The city or locality component.
   */
  public void setLocality(String locality)
  {
    setAttribute(AttributeNames.RFC7643.LOCALITY, locality);
  }

  /**
   * The state or region component.
   */
  public Optional<String> getRegion()
  {
    return getStringAttribute(AttributeNames.RFC7643.REGION);
  }

  /**
   * The state or region component.
   */
  public void setRegion(String region)
  {
    setAttribute(AttributeNames.RFC7643.REGION, region);
  }

  /**
   * The zip code or postal code component.
   */
  public Optional<String> getPostalCode()
  {
    return getStringAttribute(AttributeNames.RFC7643.POSTAL_CODE);
  }

  /**
   * The zip code or postal code component.
   */
  public void setPostalCode(String postalCode)
  {
    setAttribute(AttributeNames.RFC7643.POSTAL_CODE, postalCode);
  }

  /**
   * The country name component. When specified, the value MUST be in ISO 3166-1 "alpha-2" code format
   * [ISO3166]; e.g., the United States and Sweden are "US" and "SE", respectively.
   */
  public Optional<String> getCountry()
  {
    return getStringAttribute(AttributeNames.RFC7643.COUNTRY);
  }

  /**
   * The country name component. When specified, the value MUST be in ISO 3166-1 "alpha-2" code format
   * [ISO3166]; e.g., the United States and Sweden are "US" and "SE", respectively.
   */
  public void setCountry(String country)
  {
    setAttribute(AttributeNames.RFC7643.COUNTRY, country);
  }
}
