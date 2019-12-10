package de.captaingoldfish.scim.sdk.client.springboot;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.web.server.LocalServerPort;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * author: Pascal Knueppel <br>
 * created at: 09.12.2019 15:44 <br>
 * <br>
 * an abstract testclass that that is used as helper class to access the current port of the application and
 * to build URLs that can be used to access the application
 */
@Slf4j
public abstract class AbstractSpringBootWebTest
{


  /**
   * if spring boot test uses a random port the port will be injected into this variable
   */
  @Getter
  @LocalServerPort
  private int localServerPort;

  /**
   * contains the URL to which the requests must be sent
   */
  private String defaultUrl;

  /**
   * will initialize the url under which the locally started tomcat can be reached
   */
  @BeforeEach
  public void initializeUrl()
  {
    defaultUrl = "https://localhost:" + localServerPort;
  }

  /**
   * this method will create a request url with the given path
   * 
   * @param path the context path to the method that should be used
   * @return the complete server-url with the given context path to the method that should be used
   */
  public String getRequestUrl(String path)
  {
    return defaultUrl + (path.charAt(0) == '/' ? path : "/" + path);
  }
}
