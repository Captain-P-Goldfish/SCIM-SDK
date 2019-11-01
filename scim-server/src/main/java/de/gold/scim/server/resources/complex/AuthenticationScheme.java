package de.gold.scim.server.resources.complex;

import java.util.Objects;
import java.util.Optional;

import de.gold.scim.server.constants.AttributeNames;
import de.gold.scim.server.exceptions.InvalidConfigException;
import de.gold.scim.server.resources.base.ScimObjectNode;
import lombok.Builder;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 10:41 <br>
 * <br>
 * A multi-valued complex type that specifies supported authentication scheme properties. To enable seamless
 * discovery of configurations, the service provider SHOULD, with the appropriate security considerations,
 * make the authenticationSchemes attribute publicly accessible without prior authentication. REQUIRED.
 */
public class AuthenticationScheme extends ScimObjectNode
{

  @Builder
  public AuthenticationScheme(String name, String description, String type, String specUri, String documentationUri)
  {
    super(null);
    setName(name);
    setDescription(description);
    setType(type);
    setSpecUri(specUri);
    setDocumentationUri(documentationUri);
  }

  /**
   * The common authentication scheme name, e.g., HTTP Basic. REQUIRED.
   */
  public String getName()
  {
    return getStringAttribute(AttributeNames.RFC7643.NAME).orElseThrow(() -> {
      return new InvalidConfigException("the 'name' attribute is required");
    });
  }

  /**
   * The common authentication scheme name, e.g., HTTP Basic. REQUIRED.
   */
  public void setName(String name)
  {
    setAttribute(AttributeNames.RFC7643.NAME, Objects.requireNonNull(name));
  }

  /**
   * A description of the authentication scheme. REQUIRED.
   */
  public String getDescription()
  {
    return getStringAttribute(AttributeNames.RFC7643.DESCRIPTION).orElseThrow(() -> {
      return new InvalidConfigException("the 'description' attribute is required");
    });
  }

  /**
   * A description of the authentication scheme. REQUIRED.
   */
  public void setDescription(String description)
  {
    setAttribute(AttributeNames.RFC7643.DESCRIPTION, Objects.requireNonNull(description));
  }

  /**
   * The authentication scheme. This specification defines the values "oauth", "oauth2", "oauthbearertoken",
   * "httpbasic", and "httpdigest". REQUIRED.
   */
  public String getType()
  {
    return getStringAttribute(AttributeNames.RFC7643.TYPE).orElseThrow(() -> {
      return new InvalidConfigException("the 'type' attribute is required");
    });
  }

  /**
   * The authentication scheme. This specification defines the values "oauth", "oauth2", "oauthbearertoken",
   * "httpbasic", and "httpdigest". REQUIRED.
   */
  public void setType(String type)
  {
    setAttribute(AttributeNames.RFC7643.TYPE, Objects.requireNonNull(type));
  }

  /**
   * An HTTP-addressable URL pointing to the authentication scheme's specification. OPTIONAL.
   */
  public Optional<String> getSpecUri()
  {
    return getStringAttribute(AttributeNames.RFC7643.SPEC_URI);
  }

  /**
   * An HTTP-addressable URL pointing to the authentication scheme's specification. OPTIONAL.
   */
  public void setSpecUri(String specUri)
  {
    setAttribute(AttributeNames.RFC7643.SPEC_URI, specUri);
  }

  /**
   * An HTTP-addressable URL pointing to the authentication scheme's usage documentation. OPTIONAL.
   */
  public Optional<String> getDocumentationUri()
  {
    return getStringAttribute(AttributeNames.RFC7643.DOCUMENTATION_URI);
  }

  /**
   * An HTTP-addressable URL pointing to the authentication scheme's usage documentation. OPTIONAL.
   */
  public void setDocumentationUri(String documentationUri)
  {
    setAttribute(AttributeNames.RFC7643.DOCUMENTATION_URI, documentationUri);
  }

}
