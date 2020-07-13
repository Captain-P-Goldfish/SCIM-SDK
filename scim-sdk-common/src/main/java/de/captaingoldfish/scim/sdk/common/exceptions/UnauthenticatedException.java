package de.captaingoldfish.scim.sdk.common.exceptions;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.AuthenticationScheme;


/**
 * author Pascal Knueppel <br>
 * created at: 13.07.2020 - 09:05 <br>
 * <br>
 */
public class UnauthenticatedException extends ScimException
{

  public UnauthenticatedException(String message, AuthenticationScheme authenticationScheme, String realm)
  {
    this(message, Collections.singletonList(authenticationScheme), realm);
  }

  public UnauthenticatedException(String message, List<AuthenticationScheme> authenticationScheme, String realm)
  {
    super(message, null, HttpStatus.UNAUTHORIZED, ScimType.Custom.UNAUTENTICATED);
    getResponseHeaders().put(HttpHeader.WWW_AUTHENTICATE,
                             authenticationScheme.stream()
                                                 .map(scheme -> scheme.getWwwAuthenticateHeaderRepresentation(realm))
                                                 .collect(Collectors.joining(", ")));
  }
}
