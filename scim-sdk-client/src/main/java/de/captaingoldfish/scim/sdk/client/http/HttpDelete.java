package de.captaingoldfish.scim.sdk.client.http;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;


/**
 * @author Pascal Knueppel
 * @since 03.04.2023
 */
public class HttpDelete extends HttpEntityEnclosingRequestBase
{

  private static final String METHOD_NAME = "DELETE";

  public HttpDelete(final String uri)
  {
    super();
    setURI(URI.create(uri));
  }

  public String getMethod()
  {
    return METHOD_NAME;
  }
}

