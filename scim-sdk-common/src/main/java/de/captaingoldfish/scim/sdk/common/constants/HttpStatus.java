package de.captaingoldfish.scim.sdk.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 28.09.2019 - 11:37 <br>
 * <br>
 * this class holds the relevant http status codes that we need here
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpStatus
{

  public static final int OK = 200;

  public static final int CREATED = 201;

  public static final int NO_CONTENT = 204;

  public static final int MOVED_PERMANENTLY = 301;

  public static final int MOVED_TEMPORARILY = 302;

  public static final int NOT_MODIFIED = 304;

  public static final int BAD_REQUEST = 400;

  public static final int UNAUTHORIZED = 401;

  public static final int FORBIDDEN = 403;

  public static final int NOT_FOUND = 404;

  public static final int METHOD_NOT_ALLOWED = 405;

  public static final int NOT_ACCEPTABLE = 406;

  public static final int CONFLICT = 409;

  public static final int PRECONDITION_FAILED = 412;

  public static final int REQUEST_TOO_LONG = 413;

  public static final int REQUEST_URI_TOO_LONG = 414;

  public static final int UNSUPPORTED_MEDIA_TYPE = 415;

  public static final int INTERNAL_SERVER_ERROR = 500;

  public static final int NOT_IMPLEMENTED = 501;

}
