package de.captaingoldfish.scim.sdk.keycloak.tests.testbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import de.captaingoldfish.scim.sdk.keycloak.entities.ScimResourceTypeEntity;
import de.captaingoldfish.scim.sdk.keycloak.entities.ScimServiceProviderEntity;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.TestSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.keycloakdirectsetup.DirectKeycloakAccessSetup;
import de.captaingoldfish.scim.sdk.keycloak.tests.setup.utils.WaitStrategy;


/**
 * @author Pascal Knueppel
 * @since 13.12.2020
 */
public class CreateNewRealmTestBuilder extends AbstractTestBuilder
{

  private static final String REALM_ID = UUID.randomUUID().toString();

  public CreateNewRealmTestBuilder(WebDriver webDriver,
                                   TestSetup testSetup,
                                   DirectKeycloakAccessSetup directKeycloakAccessSetup)
  {
    super(webDriver, testSetup, directKeycloakAccessSetup);
  }

  @Override
  public List<DynamicTest> buildDynamicTests()
  {
    List<DynamicTest> dynamicTests = new ArrayList<>();
    dynamicTests.add(DynamicTest.dynamicTest("create a new realm" + REALM_ID, () -> {
      Actions actions = new Actions(webDriver);
      actions = actions.moveByOffset(200, 200);
      actions.perform();
      WebElement realmSelector = wait.until(d -> d.findElement(By.xpath("//div[@class = 'realm-selector']")));
      actions.moveToElement(realmSelector).perform();

      untilClickable(By.xpath("//a[@href = '#/create/realm']")).click();
      wait.until(d -> d.findElement(By.id("name"))).sendKeys(REALM_ID);
      webDriver.findElement(By.xpath("//button[text() = 'Create']")).click();
      WebDriverWait waitForRealmCreate = new WebDriverWait(webDriver, 10);
      waitForRealmCreate.until(d -> d.findElement(By.id("name")));
    }));
    // **********************************************************************************************************
    dynamicTests.add(getClickScimMenuTest());
    // **********************************************************************************************************
    String longTestName = "verify scim database entries for realm '" + REALM_ID + "' are created";
    dynamicTests.add(DynamicTest.dynamicTest(longTestName, () -> {
      new WaitStrategy().waitFor(() -> {
        ScimServiceProviderEntity scimServiceProviderEntity = directKeycloakAccessSetup.getServiceProviderEntity(REALM_ID);
        Assertions.assertNotNull(scimServiceProviderEntity);
        List<ScimResourceTypeEntity> resourceTypeEntities = directKeycloakAccessSetup.getResourceTypeEntities(REALM_ID);
        Assertions.assertEquals(3, resourceTypeEntities.size());
      });
    }));
    // **********************************************************************************************************
    // TODO
    // **********************************************************************************************************
    dynamicTests.add(DynamicTest.dynamicTest("delete realm: " + REALM_ID, () -> {
      By realmSettingsMenuXPath = By.xpath("//a[text()[contains(.,'Realm Settings')]]");
      wait.ignoring(StaleElementReferenceException.class)
          .until(ExpectedConditions.elementToBeClickable(realmSettingsMenuXPath))
          .click();
      WebElement removeRealmIcon = untilClickable(By.id("removeRealm"));
      removeRealmIcon.click();
      final String xPathDeleteButton = "//div[@class='modal-dialog']//button[@ng-click='ok()']";
      WebElement deleteButton = wait.until(d -> d.findElement(By.xpath(xPathDeleteButton)));
      deleteButton.click();
      wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(xPathDeleteButton)));
    }));
    // **********************************************************************************************************
    longTestName = "verify scim database entries for realm '" + REALM_ID + "' are deleted";
    dynamicTests.add(DynamicTest.dynamicTest(longTestName, () -> {
      directKeycloakAccessSetup.clearCache();
      ScimServiceProviderEntity scimServiceProviderEntity = directKeycloakAccessSetup.getServiceProviderEntity(REALM_ID);
      Assertions.assertNull(scimServiceProviderEntity);
      List<ScimResourceTypeEntity> resourceTypeEntities = directKeycloakAccessSetup.getResourceTypeEntities(REALM_ID);
      Assertions.assertEquals(0, resourceTypeEntities.size());
    }));
    // **********************************************************************************************************
    return dynamicTests;
  }
}
