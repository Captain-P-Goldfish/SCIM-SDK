package de.captaingoldfish.scim.sdk.server.endpoints.features;

/**
 * author Pascal Knueppel <br>
 * created at: 26.11.2019 - 09:52 <br>
 * <br>
 * represents the different endpoint types. UPDATE and PATCH are both represented by UPDATE
 */
public enum EndpointType
{
  CREATE,
  GET,
  LIST,
  /**
   * represents UPDATE and PATCH
   */
  UPDATE,
  DELETE
}
