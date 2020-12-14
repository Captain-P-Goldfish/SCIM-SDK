package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DynamicTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;


/**
 * @author Pascal Knueppel
 * @since 12.12.2020
 */
public abstract class AbstractTestBuilder
{

  /**
   * helper object to wait for conditions that should be fulfilled
   */
  protected final WebDriverWait wait;

  /**
   * the current web driver that is used to test the web admin console
   */
  protected final WebDriver webDriver;

  /**
   * a keycloak mock setup that grants direct database access with keycloak objects
   */
  protected final DirectKeycloakAccessSetup directKeycloakAccessSetup;

  /**
   * the test setup that holds information of the currently running server instances
   */
  protected final TestSetup testSetup;

  public AbstractTestBuilder(WebDriver webDriver,
                             TestSetup testSetup,
                             DirectKeycloakAccessSetup directKeycloakAccessSetup)
  {
    this.webDriver = webDriver;
    this.testSetup = testSetup;
    this.directKeycloakAccessSetup = directKeycloakAccessSetup;
    this.wait = new WebDriverWait(webDriver, 5);
  }

  /**
   * @return the tests that should be created by this implementation
   */
  public abstract List<DynamicTest> buildDynamicTests();

  /**
   * @return the username to login to the web admin console
   */
  public String getWebAdminConsoleUsername()
  {
    return testSetup.getAdminUserName();
  }

  /**
   * @return the password to login to the web admin console
   */
  public String getWebAdminConsolePassword()
  {
    return testSetup.getAdminUserPassword();
  }

  /**
   * the keycloak checkbox elements have their id values on an element that is not clickable. The clickable
   * element is the parent element of the element that is being selected by the id
   * 
   * @param idSelector the id selector for the checkbox element
   * @return the clickable element
   */
  protected WebElement getKeycloakCheckboxElement(By idSelector)
  {
    WebElement enabledButton = wait.until(d -> d.findElement(idSelector));
    return enabledButton.findElement(By.xpath("./.."));
  }

  /**
   * thx to <a href=
   * "https://sqa.stackexchange.com/questions/26299/staleelementreferenceexception-with-explicit-wait">Stack
   * Exchange</a>
   */
  protected WebElement untilClickable(By what)
  {
    return wait.until(ExpectedConditions.elementToBeClickable(what));
  }

  /**
   * @return a test that will simply load the service provider configuration in the web admin console
   */
  public DynamicTest getClickScimMenuTest()
  {
    return DynamicTest.dynamicTest("load SCIM Service Provider configuration", () -> {
      final By xpathScimMenuItem = By.xpath("//li[@id= 'scim-menu']/a");
      WebElement scimMenuLink = wait.until(d -> d.findElement(xpathScimMenuItem));
      MatcherAssert.assertThat(scimMenuLink.getAttribute("href"),
                               Matchers.matchesPattern(".*?#/realms/[\\w-]+?/scim/service-provider/settings"));
      untilClickable(xpathScimMenuItem).click();
      wait.until(d -> d.findElement(By.id("enabled")));
    });
  }

}
