package de.captaingoldfish.scim.sdk.common.constants;

import de.captaingoldfish.scim.sdk.common.constants.enums.HttpMethod;
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

  // --- 1xx Informational ---

  /** {@code 100 Continue} (HTTP/1.1 - RFC 2616) */
  public static final int CONTINUE = 100;

  /** {@code 101 Switching Protocols} (HTTP/1.1 - RFC 2616) */
  public static final int SWITCHING_PROTOCOLS = 101;

  /** {@code 102 Processing} (WebDAV - RFC 2518) */
  public static final int PROCESSING = 102;

  // --- 2xx Success ---

  /** {@code 200 OK} (HTTP/1.0 - RFC 1945) */
  public static final int OK = 200;

  /** {@code 201 Created} (HTTP/1.0 - RFC 1945) */
  public static final int CREATED = 201;

  /** {@code 202 Accepted} (HTTP/1.0 - RFC 1945) */
  public static final int ACCEPTED = 202;

  /** {@code 203 Non Authoritative Information} (HTTP/1.1 - RFC 2616) */
  public static final int NON_AUTHORITATIVE_INFORMATION = 203;

  /** {@code 204 No Content} (HTTP/1.0 - RFC 1945) */
  public static final int NO_CONTENT = 204;

  /** {@code 205 Reset Content} (HTTP/1.1 - RFC 2616) */
  public static final int RESET_CONTENT = 205;

  /** {@code 206 Partial Content} (HTTP/1.1 - RFC 2616) */
  public static final int PARTIAL_CONTENT = 206;

  /**
   * {@code 207 Multi-Status} (WebDAV - RFC 2518) or {@code 207 Partial Update OK} (HTTP/1.1 -
   * draft-ietf-http-v11-spec-rev-01?)
   */
  public static final int MULTI_STATUS = 207;

  // --- 3xx Redirection ---

  /** {@code 300 Mutliple Choices} (HTTP/1.1 - RFC 2616) */
  public static final int MULTIPLE_CHOICES = 300;

  /** {@code 301 Moved Permanently} (HTTP/1.0 - RFC 1945) */
  public static final int MOVED_PERMANENTLY = 301;

  /** {@code 302 Moved Temporarily} (Sometimes {@code Found}) (HTTP/1.0 - RFC 1945) */
  public static final int MOVED_TEMPORARILY = 302;

  /** {@code 303 See Other} (HTTP/1.1 - RFC 2616) */
  public static final int SEE_OTHER = 303;

  /** {@code 304 Not Modified} (HTTP/1.0 - RFC 1945) */
  public static final int NOT_MODIFIED = 304;

  /** {@code 305 Use Proxy} (HTTP/1.1 - RFC 2616) */
  public static final int USE_PROXY = 305;

  /** {@code 307 Temporary Redirect} (HTTP/1.1 - RFC 2616) */
  public static final int TEMPORARY_REDIRECT = 307;

  // --- 4xx Client Error ---

  /** {@code 400 Bad Request} (HTTP/1.1 - RFC 2616) */
  public static final int BAD_REQUEST = 400;

  /** {@code 401 Unauthorized} (HTTP/1.0 - RFC 1945) */
  public static final int UNAUTHORIZED = 401;

  /** {@code 402 Payment Required} (HTTP/1.1 - RFC 2616) */
  public static final int PAYMENT_REQUIRED = 402;

  /** {@code 403 Forbidden} (HTTP/1.0 - RFC 1945) */
  public static final int FORBIDDEN = 403;

  /** {@code 404 Not Found} (HTTP/1.0 - RFC 1945) */
  public static final int NOT_FOUND = 404;

  /** {@code 405 Method Not Allowed} (HTTP/1.1 - RFC 2616) */
  public static final int METHOD_NOT_ALLOWED = 405;

  /** {@code 406 Not Acceptable} (HTTP/1.1 - RFC 2616) */
  public static final int NOT_ACCEPTABLE = 406;

  /** {@code 407 Proxy Authentication Required} (HTTP/1.1 - RFC 2616) */
  public static final int PROXY_AUTHENTICATION_REQUIRED = 407;

  /** {@code 408 Request Timeout} (HTTP/1.1 - RFC 2616) */
  public static final int REQUEST_TIMEOUT = 408;

  /** {@code 409 Conflict} (HTTP/1.1 - RFC 2616) */
  public static final int CONFLICT = 409;

  /** {@code 410 Gone} (HTTP/1.1 - RFC 2616) */
  public static final int GONE = 410;

  /** {@code 411 Length Required} (HTTP/1.1 - RFC 2616) */
  public static final int LENGTH_REQUIRED = 411;

  /** {@code 412 Precondition Failed} (HTTP/1.1 - RFC 2616) */
  public static final int PRECONDITION_FAILED = 412;

  /** {@code 413 Request Entity Too Large} (HTTP/1.1 - RFC 2616) */
  public static final int REQUEST_TOO_LONG = 413;

  /** {@code 414 Request-URI Too Long} (HTTP/1.1 - RFC 2616) */
  public static final int REQUEST_URI_TOO_LONG = 414;

  /** {@code 415 Unsupported Media Type} (HTTP/1.1 - RFC 2616) */
  public static final int UNSUPPORTED_MEDIA_TYPE = 415;

  /** {@code 416 Requested Range Not Satisfiable} (HTTP/1.1 - RFC 2616) */
  public static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;

  /** {@code 417 Expectation Failed} (HTTP/1.1 - RFC 2616) */
  public static final int EXPECTATION_FAILED = 417;

  /**
   * Static constant for a 419 error. {@code 419 Insufficient Space on Resource} (WebDAV -
   * draft-ietf-webdav-protocol-05?) or {@code 419 Proxy Reauthentication Required} (HTTP/1.1 drafts?)
   */
  public static final int INSUFFICIENT_SPACE_ON_RESOURCE = 419;

  /**
   * Static constant for a 420 error. {@code 420 Method Failure} (WebDAV - draft-ietf-webdav-protocol-05?)
   */
  public static final int METHOD_FAILURE = 420;

  /** {@code 422 Unprocessable Entity} (WebDAV - RFC 2518) */
  public static final int UNPROCESSABLE_ENTITY = 422;

  /** {@code 423 Locked} (WebDAV - RFC 2518) */
  public static final int LOCKED = 423;

  /** {@code 424 Failed Dependency} (WebDAV - RFC 2518) */
  public static final int FAILED_DEPENDENCY = 424;

  /** {@code 429 Too Many Requests} (Additional HTTP Status Codes - RFC 6585) */
  public static final int TOO_MANY_REQUESTS = 429;

  // --- 5xx Server Error ---

  /** {@code 500 Server Error} (HTTP/1.0 - RFC 1945) */
  public static final int INTERNAL_SERVER_ERROR = 500;

  /** {@code 501 Not Implemented} (HTTP/1.0 - RFC 1945) */
  public static final int NOT_IMPLEMENTED = 501;

  /** {@code 502 Bad Gateway} (HTTP/1.0 - RFC 1945) */
  public static final int BAD_GATEWAY = 502;

  /** {@code 503 Service Unavailable} (HTTP/1.0 - RFC 1945) */
  public static final int SERVICE_UNAVAILABLE = 503;

  /** {@code 504 Gateway Timeout} (HTTP/1.1 - RFC 2616) */
  public static final int GATEWAY_TIMEOUT = 504;

  /** {@code 505 HTTP Version Not Supported} (HTTP/1.1 - RFC 2616) */
  public static final int HTTP_VERSION_NOT_SUPPORTED = 505;

  /** {@code 507 Insufficient Storage} (WebDAV - RFC 2518) */
  public static final int INSUFFICIENT_STORAGE = 507;

  /**
   * checks if the response operation indicates success or failure
   *
   * @param op the response operation from the remote server
   * @return true if the operation seems to be successful, false else
   */
  public static boolean isResponseSuccessful(HttpMethod httpMethod, int status)
  {
    switch (httpMethod)
    {
      case POST:
        return status == HttpStatus.CREATED;
      case PATCH:
        return status == HttpStatus.OK || status == HttpStatus.NO_CONTENT;
      case DELETE:
        return status == HttpStatus.NO_CONTENT;
      default:
        return status == HttpStatus.OK;
    }
  }

}
