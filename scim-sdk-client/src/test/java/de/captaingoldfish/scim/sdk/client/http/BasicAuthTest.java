package de.captaingoldfish.scim.sdk.client.http;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;


/**
 * @author Pascal Knueppel
 * @since 19.02.2021
 */
public class BasicAuthTest
{

  /**
   * verifis that basic auth is setting the correct expected values
   */
  @TestFactory
  public List<DynamicTest> testBasicAuthBuilder()
  {
    List<DynamicTest> dynamicTestList = new ArrayList<>();
    dynamicTestList.add(DynamicTest.dynamicTest("Test no values in basic auth", () -> {
      BasicAuth basicAuth = BasicAuth.builder().build();
      Assertions.assertEquals("Basic Og==", basicAuth.getAuthorizationHeaderValue());
    }));
    dynamicTestList.add(DynamicTest.dynamicTest("Test only username in basic auth", () -> {
      BasicAuth basicAuth = BasicAuth.builder().username("1").build();
      Assertions.assertEquals("Basic MTo=", basicAuth.getAuthorizationHeaderValue());
    }));
    dynamicTestList.add(DynamicTest.dynamicTest("Test only password in basic auth", () -> {
      BasicAuth basicAuth = BasicAuth.builder().password("1").build();
      Assertions.assertEquals("Basic OjE=", basicAuth.getAuthorizationHeaderValue());
    }));
    dynamicTestList.add(DynamicTest.dynamicTest("Test username and password in basic auth", () -> {
      BasicAuth basicAuth = BasicAuth.builder().username("1").password("1").build();
      Assertions.assertEquals("Basic MTox", basicAuth.getAuthorizationHeaderValue());
    }));
    return dynamicTestList;
  }
}
