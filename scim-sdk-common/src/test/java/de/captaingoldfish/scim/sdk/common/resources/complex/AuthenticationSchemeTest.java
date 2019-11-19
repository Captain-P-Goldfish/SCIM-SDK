package de.captaingoldfish.scim.sdk.common.resources.complex;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidConfigException;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.AuthenticationScheme;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 12:15 <br>
 * <br>
 */
public class AuthenticationSchemeTest
{

  /**
   * verifies that a new created instance is not empty
   */
  @Test
  public void testNewCreatedInstanceIsNotEmpty()
  {
    Assertions.assertThrows(NullPointerException.class, () -> AuthenticationScheme.builder().build());
    Assertions.assertThrows(NullPointerException.class, () -> AuthenticationScheme.builder().name("name").build());
    AuthenticationScheme authenticationScheme = Assertions.assertDoesNotThrow(() -> {
      return AuthenticationScheme.builder().name("name").description("description").type("type").build();
    });
    MatcherAssert.assertThat(authenticationScheme, Matchers.not(Matchers.emptyIterable()));
    Assertions.assertEquals(3, authenticationScheme.size());
    Assertions.assertDoesNotThrow(authenticationScheme::getAttributeName);
    Assertions.assertDoesNotThrow(authenticationScheme::getDescription);
    Assertions.assertDoesNotThrow(authenticationScheme::getAuthenticationType);
    Assertions.assertDoesNotThrow(authenticationScheme::getDocumentationUri);
    Assertions.assertDoesNotThrow(authenticationScheme::getSpecUri);
  }

  /**
   * verifies that the configurations are not empty on getter methods even if the configurations have been
   * removed from the json structure
   */
  @Test
  public void testGetterMethods()
  {
    AuthenticationScheme authenticationScheme = Assertions.assertDoesNotThrow(() -> {
      return AuthenticationScheme.builder().name("name").description("description").type("type").build();
    });

    authenticationScheme.remove(AttributeNames.RFC7643.NAME);
    authenticationScheme.remove(AttributeNames.RFC7643.DESCRIPTION);
    authenticationScheme.remove(AttributeNames.RFC7643.TYPE);
    authenticationScheme.remove(AttributeNames.RFC7643.DOCUMENTATION_URI);
    authenticationScheme.remove(AttributeNames.RFC7643.SPEC_URI);

    Assertions.assertThrows(InvalidConfigException.class, authenticationScheme::getName);
    Assertions.assertThrows(InvalidConfigException.class, authenticationScheme::getDescription);
    Assertions.assertThrows(InvalidConfigException.class, authenticationScheme::getAuthenticationType);
    Assertions.assertDoesNotThrow(authenticationScheme::getDocumentationUri);
    Assertions.assertDoesNotThrow(authenticationScheme::getSpecUri);
    Assertions.assertFalse(authenticationScheme.getDocumentationUri().isPresent());
    Assertions.assertFalse(authenticationScheme.getSpecUri().isPresent());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testSetterMethods()
  {
    final String name = "OAuth Bearer Token";
    final String description = "Authentication scheme using the OAuth Bearer Token Standard";
    final String type = "oauthbearertoken";
    final String documentationUri = "http://example.com/help/oauth.html";
    final String specUri = "http://www.rfc-editor.org/info/rfc6750";

    AuthenticationScheme authenticationScheme = Assertions.assertDoesNotThrow(() -> {
      return AuthenticationScheme.builder().name("name").description("description").type("type").build();
    });


    authenticationScheme.setName(name);
    authenticationScheme.setDescription(description);
    authenticationScheme.setAuthenticationType(type);
    authenticationScheme.setDocumentationUri(documentationUri);
    authenticationScheme.setSpecUri(specUri);

    Assertions.assertEquals(name, authenticationScheme.getName());
    Assertions.assertEquals(description, authenticationScheme.getDescription());
    Assertions.assertEquals(type, authenticationScheme.getAuthenticationType());
    Assertions.assertEquals(documentationUri, authenticationScheme.getDocumentationUri().get());
    Assertions.assertEquals(specUri, authenticationScheme.getSpecUri().get());
  }

  /**
   * verifies that the values can successfully be overridden
   */
  @Test
  public void testBuilderParameterSet()
  {
    final String name = "OAuth Bearer Token";
    final String description = "Authentication scheme using the OAuth Bearer Token Standard";
    final String type = "oauthbearertoken";
    final String documentationUri = "http://example.com/help/oauth.html";
    final String specUri = "http://www.rfc-editor.org/info/rfc6750";
    AuthenticationScheme authenticationScheme = AuthenticationScheme.builder()
                                                                    .name(name)
                                                                    .description(description)
                                                                    .type(type)
                                                                    .documentationUri(documentationUri)
                                                                    .specUri(specUri)
                                                                    .build();

    Assertions.assertEquals(name, authenticationScheme.getName());
    Assertions.assertEquals(description, authenticationScheme.getDescription());
    Assertions.assertEquals(type, authenticationScheme.getAuthenticationType());
    Assertions.assertEquals(documentationUri, authenticationScheme.getDocumentationUri().get());
    Assertions.assertEquals(specUri, authenticationScheme.getSpecUri().get());
  }
}
