package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;


/**
 * @author Pascal Knueppel
 * @since 12.12.2020
 */
public class WebAdminLoginTestBuilder extends AbstractTestBuilder
{

  public WebAdminLoginTestBuilder(WebDriver webDriver,
                                  TestSetup testSetup,
                                  DirectKeycloakAccessSetup directKeycloakAccessSetup)
  {
    super(webDriver, testSetup, directKeycloakAccessSetup);
  }

  /**
   * loads the login page of the keycloak web admin and executes a login
   */
  @Override
  public List<DynamicTest> buildDynamicTests()
  {
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(DynamicTest.dynamicTest("login to web admin console", () -> {
      final String loginAddress = testSetup.getBrowserAccessUrl() + "/auth/admin/";
      webDriver.get(loginAddress);

      WebElement usernameInput = wait.until(d -> d.findElement(By.id("username")));
      WebElement passwordInput = webDriver.findElement(By.id("password"));
      WebElement loginForm = webDriver.findElement(By.id("kc-login"));

      usernameInput.sendKeys(getWebAdminConsoleUsername());
      passwordInput.sendKeys(getWebAdminConsolePassword());
      loginForm.click();
      // keycloak is acting differently based on the case that the server is running on windows or in a docker
      // environment. Therefore we are calling the master realm directly to ensure that we start on the master
      // realms overview page
      final String masterRealmUrl = loginAddress + "master/console/#/realms/master";
      webDriver.get(masterRealmUrl);
    }));
    return dynamicTests;
  }
}
