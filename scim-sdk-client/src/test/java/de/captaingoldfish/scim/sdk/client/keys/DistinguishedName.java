package de.captaingoldfish.scim.sdk.client.keys;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x500.X500Name;

import de.captaingoldfish.scim.sdk.client.exceptions.UnsupportedDnException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * <br>
 * DN is the distinguished name which helps identifying the owner of a certificate and the issuer who granted
 * the certificate to the subject.
 */
@Slf4j
@EqualsAndHashCode
public class DistinguishedName
{

  /**
   * This string represents the supported characters that can be entered into this DN
   */
  public static final String SUPPORTED_CHARACTER_SET = "[A-Za-z0-9 \\\\()_+,-./:=?<>@äöüÄÖÜß]";

  private static final String COMMON_NAME = "CN=";

  private static final String ORGANIZATION_UNIT = "OU=";

  private static final String ORGANIZATION = "O=";

  private static final String LOCALITY = "L=";

  private static final String STATE = "ST=";

  private static final String STATE_ALTERNATIVE = "S=";

  private static final String DOMAIN_COMPONENT = "DC=";

  private static final String EMAIL = "E=";

  private static final String EMAIL_ADDRESS = "EMAILADDRESS==";

  private static final String STREET = "STREET=";

  private static final String COUNTRY = "COUNTRY=";

  /**
   * the only mandatory field. Describes the name, website or company of the subject.
   */
  private String commonName; // CN

  /**
   * the name of the country. Normally given in a two character representation "US", "DE" etc.<br>
   * null is allowed
   */
  private String country; // C

  /**
   * the state or province of the subject. <br>
   * null is allowed.
   */
  private String stateOrProvince; // ST

  /**
   * the locality name of the subject <br>
   * null is allowed.
   */
  private String locality; // L

  /**
   * the name of the subjects organization <br>
   * null is allowed.
   */
  private String organization; // O

  /**
   * the name of the subjects organization unit. <br>
   * null is allowed
   */
  private List<String> organizationalUnit = new ArrayList<>(); // OU

  /**
   * the name of the domain component of the subject. <br>
   * null is allowed.
   */
  private List<String> domainComponent = new ArrayList<>(); // DC

  /**
   * the address of the subject.<br>
   * null is allowed.
   */
  private String streetAddress; // STREET

  /**
   * the subjects email-address. <br>
   * null is allowed.
   */
  private String email; // E


  /**
   * constructor with the most common field.
   *
   * @param commonName mandatory field. {@link #commonName}
   * @param countryName may be null
   * @param stateOrProvince may be null
   * @param localityName may be null
   * @param organizationName may be null
   * @param organizationalUnitName may be null
   */
  @Builder
  public DistinguishedName(String commonName,
                           String countryName,
                           String stateOrProvince,
                           String localityName,
                           String organizationName,
                           String organizationalUnitName)
  {
    this.commonName = commonName;
    this.country = countryName;
    this.stateOrProvince = stateOrProvince;
    this.locality = localityName;
    this.organization = organizationName;
    if (StringUtils.isNotBlank(organizationalUnitName))
    {
      this.organizationalUnit.add(organizationalUnitName);
    }
  }

  /**
   * Constructor with all listed fields in this class
   *
   * @param commonName mandatory field. {@link #commonName}
   * @param countryName may be null
   * @param stateOrProvince may be null
   * @param localityName may be null
   * @param organizationName may be null
   * @param organizationalUnitName may be null
   * @param domainComponent may be null
   * @param streetAddress may be null
   * @param email may be null
   */
  @Builder
  public DistinguishedName(String commonName,
                           String countryName,
                           String stateOrProvince,
                           String localityName,
                           String organizationName,
                           String organizationalUnitName,
                           String domainComponent,
                           String streetAddress,
                           String email)
  {
    this.commonName = commonName;
    this.country = countryName;
    this.stateOrProvince = stateOrProvince;
    this.locality = localityName;
    this.organization = organizationName;
    if (StringUtils.isNotBlank(organizationalUnitName))
    {
      this.organizationalUnit.add(organizationalUnitName);
    }
    if (StringUtils.isNotBlank(domainComponent))
    {
      this.domainComponent.add(domainComponent);
    }
    this.streetAddress = streetAddress;
    this.email = email;
  }

  public DistinguishedName(Principal dn)
  {
    this(Objects.requireNonNull(dn).toString());
  }

  /**
   * This constructor takes a DN in stringform and expects the seperator of the fields to be a colon. It will
   * parse all values that are known to this class into the given fields. <br>
   * The primary goal of this constructor is to keep a balance in the order of the fields. For example if a
   * certificate should be signed by issuer A for subject B and A tries to parse the DN of B but does not keep
   * the order of the fields. The signature will become invalid since the hash-algorithm calculates another
   * value if the string is altered. So it is important that the order of the DN-fields are kept.
   *
   * @param completeDN the complete DN of a certificate that should be parsed.
   */
  public DistinguishedName(String completeDN)
  {
    String[] dnParts = completeDN.split(",");
    for ( String dnPart : dnParts )
    {
      dnPart = dnPart.trim();
      if (dnPart.toUpperCase().startsWith(COMMON_NAME))
      {
        commonName = dnPart.replaceFirst(COMMON_NAME, "").trim();
      }
      else if (dnPart.toUpperCase().startsWith(ORGANIZATION_UNIT))
      {
        organizationalUnit.add(dnPart.replaceFirst(ORGANIZATION_UNIT, "").trim());
      }
      else if (dnPart.toUpperCase().startsWith(ORGANIZATION))
      {
        organization = dnPart.replaceFirst(ORGANIZATION, "").trim();
      }
      else if (dnPart.toUpperCase().startsWith(LOCALITY))
      {
        locality = dnPart.replaceFirst(LOCALITY, "").trim();
      }
      else if (dnPart.toUpperCase().startsWith(COUNTRY))
      {
        country = dnPart.replaceFirst(COUNTRY, "").trim();
      }
      else if (dnPart.toUpperCase().startsWith(STATE) || dnPart.toUpperCase().startsWith(STATE_ALTERNATIVE))
      {
        stateOrProvince = dnPart.replaceFirst(STATE, "").replaceFirst(STATE_ALTERNATIVE, "").trim();
      }
      else if (dnPart.toUpperCase().startsWith(DOMAIN_COMPONENT))
      {
        domainComponent.add(dnPart.replaceFirst(DOMAIN_COMPONENT, "").trim());
      }
      else if (dnPart.toUpperCase().startsWith(STREET))
      {
        streetAddress = dnPart.replaceFirst(STREET, "").trim();
      }
      else if (dnPart.toUpperCase().startsWith(EMAIL))
      {
        email = dnPart.replaceFirst(EMAIL, "").trim();
      }
      else if (dnPart.toUpperCase().startsWith(EMAIL_ADDRESS))
      {
        email = dnPart.replaceFirst(EMAIL_ADDRESS, "").trim();
      }
    }
  }

  /**
   * gets the country name
   *
   * @return the country name
   */
  public String getCountry()
  {
    if (StringUtils.isBlank(country))
    {
      return "";
    }
    return country;
  }

  /**
   * gets the common name
   *
   * @return the common name
   */
  public String getCommonName()
  {
    if (StringUtils.isBlank(commonName))
    {
      return "";
    }
    return commonName;
  }

  /**
   * gets the state or province name
   *
   * @return the state or province name
   */
  public String getStateOrProvince()
  {
    if (StringUtils.isBlank(stateOrProvince))
    {
      return "";
    }
    return stateOrProvince;
  }

  /**
   * gets the locality name
   *
   * @return the locality name
   */
  public String getLocality()
  {
    if (StringUtils.isBlank(locality))
    {
      return "";
    }
    return locality;
  }

  /**
   * gets the organization name
   *
   * @return the organization name
   */
  public String getOrganization()
  {
    if (StringUtils.isBlank(organization))
    {
      return "";
    }
    return organization;
  }

  /**
   * gets the organizational unit name
   *
   * @return the organizational unit name
   */
  public List<String> getOrganizationalUnit()
  {
    return organizationalUnit;
  }

  /**
   * gets the organizational unit name
   *
   * @return the organizational unit name as ;-separated string
   */
  public String getOrganizationalUnitAsString()
  {
    return String.join(";", organizationalUnit);
  }

  /**
   * gets the organizational unit name as dn string
   *
   * @return the organizational unit name as DN string: OU=Blubb,OU=Bblub
   */
  public String getOrganizationalUnitAsDnString()
  {
    return buildDnStringFromList(getOrganizationalUnit(), ORGANIZATION_UNIT);
  }

  /**
   * gets the domain component
   *
   * @return the domain component name
   */
  public List<String> getDomainComponent()
  {
    return domainComponent;
  }

  /**
   * gets the domain component
   *
   * @return the domain component name as ;-separated string
   */
  public String getDomainComponentAsString()
  {
    return String.join(";", domainComponent);
  }

  /**
   * gets the domain component as DN string
   *
   * @return the domain component names as DN string: DC=Blubb,DC=Bblub
   */
  public String getDomainComponentAsDnString()
  {
    return buildDnStringFromList(getDomainComponent(), DOMAIN_COMPONENT);
  }

  /**
   * builds a DN string from the given list with the given prefix
   *
   * @param list the list that should be converted to a DN-string
   * @param prefix the prefix to identify the dn-string value
   * @return the dn string of the list
   */
  private String buildDnStringFromList(List<String> list, String prefix)
  {
    StringBuilder stringBuilder = new StringBuilder();
    for ( int i = 0 ; i < list.size() ; i++ )
    {
      String dc = list.get(i);
      stringBuilder.append(prefix).append(dc);
      if (i != list.size() - 1)
      {
        stringBuilder.append(",");
      }
    }
    return stringBuilder.toString();
  }

  /**
   * gets the street
   *
   * @return the street name with "STREET={}"
   */
  public String getStreetAddress()
  {
    if (StringUtils.isBlank(streetAddress))
    {
      return "";
    }
    return streetAddress;
  }

  /**
   * Gets the email
   *
   * @return the email with "E={}"
   */
  public String getEmail()
  {
    if (StringUtils.isBlank(email))
    {
      return "";
    }
    return email;
  }

  /**
   * Gives back the corresponding {@link X500Name} of this DN
   *
   * @return the corresponding X500Name
   */
  public X500Name toX500Name()
  {
    checkValidity();
    return new X500Name(toString());
  }

  /**
   * Gives back the corresponding {@link X500Principal} of this DN
   *
   * @return the corresponding X500Principal
   */
  public X500Principal toX500Principal()
  {
    checkValidity();
    return new X500Principal(toString());
  }

  /**
   * checks if the data of this DN contains only supported characters
   *
   * @throws UnsupportedDnException if this DN does contain unsupported characters.
   */
  public void checkValidity()
  {
    String dn = toString();
    if (log.isTraceEnabled())
    {
      log.trace("checking the DN '{}' against the subset of supported characters '{}'", dn, SUPPORTED_CHARACTER_SET);
    }
    if (!dn.matches(SUPPORTED_CHARACTER_SET + "*"))
    {
      throw new UnsupportedDnException("The given DN seems to contain invalid characters!", dn,
                                       SUPPORTED_CHARACTER_SET);
    }
  }

  @Override
  public String toString()
  {
    return (getEmail().length() > 0 ? ", " + EMAIL_ADDRESS + getEmail() : "")
           + (getCommonName().length() > 0 ? COMMON_NAME + getCommonName() : "")
           + (!getOrganizationalUnit().isEmpty() ? ", " + getOrganizationalUnitAsDnString() : "")
           + (getOrganization().length() > 0 ? ", " + ORGANIZATION + getOrganization() : "")
           + (getStateOrProvince().length() > 0 ? ", " + STATE + getStateOrProvince() : "")
           + (getLocality().length() > 0 ? ", " + LOCALITY + getLocality() : "")
           + (!getDomainComponent().isEmpty() ? ", " + getDomainComponentAsDnString() : "")
           + (getStreetAddress().length() > 0 ? ", " + STREET + getStreetAddress() : "")
           + (getCountry().length() > 0 ? ", " + COUNTRY + getCountry() : "");
  }
}
