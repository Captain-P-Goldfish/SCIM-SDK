package de.gold.scim.response;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 03.10.2019 - 19:29 <br>
 * <br>
 */
@AllArgsConstructor
public abstract class ScimResponse
{

  /**
   * the validated json document response
   */
  private JsonNode resourceNode;

  public abstract String toJsonDocument();
}
