package de.captaingoldfish.scim.sdk.keycloak.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.WebDriver;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
@Slf4j
public abstract class FrontendTests
{

  /**
   * the test setup that should be executed. It represents either a docker setup or a already running local
   * setup
   */
  private final TestSetup testSetup;

  /**
   * creates a direct connection to the currently running database with a mocked keycloak session setup
   */
  private DirectKeycloakAccessSetup directKeycloakAccessSetup;

  public FrontendTests(TestSetup testSetup)
  {
    this.testSetup = testSetup;
  }

  /**
   * initializes the test setup
   */
  @BeforeEach
  public void initializeSetup()
  {
    testSetup.start();
    this.directKeycloakAccessSetup = new DirectKeycloakAccessSetup(testSetup.getDbSetup().getDatabaseProperties());
  }

  /**
   * tears down the test setup
   */
  @AfterEach
  public void tearDownSetup()
  {
    testSetup.stop();
  }

  /**
   * defines the frontend tests for the scim-for-keycloak application
   */
  @Tag("integration-tests")
  @TestFactory
  public List<DynamicTest> testScimForKeycloakFrontend()
  {
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(DynamicTest.dynamicTest("open keycloak web admin console and do login", () -> {
      WebDriver webDriver = testSetup.createNewWebDriver();
      webDriver.get(testSetup.getBrowserAccessUrl());
      log.warn("test");
    }));
    return dynamicTests;
  }
}
