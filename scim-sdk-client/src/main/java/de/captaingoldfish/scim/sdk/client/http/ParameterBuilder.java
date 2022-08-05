package de.captaingoldfish.scim.sdk.client.http;

import java.util.HashMap;
import java.util.Map;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 22:22 <br>
 * <br>
 * this class is used to add parameters to http requests with the {@link ScimHttpClient}
 */
public class ParameterBuilder
{

  /**
   * a map holding the parameters that should be added to an http reqeust
   */
  private Map<String, String> parameterMap = new HashMap<>();

  /**
   * creates a builder instance
   */
  public static ParameterBuilder builder()
  {
    return new ParameterBuilder();
  }

  /**
   * adds the given name value pair and returns the builder instance
   *
   * @param name the name of the parameter
   * @param value the value of the parameter
   * @return the builder instance
   */
  public ParameterBuilder addParameter(String name, String value)
  {
    parameterMap.put(name, value);
    return this;
  }

  /**
   * builds the parameters as map
   */
  public Map<String, String> build()
  {
    return parameterMap;
  }
}
