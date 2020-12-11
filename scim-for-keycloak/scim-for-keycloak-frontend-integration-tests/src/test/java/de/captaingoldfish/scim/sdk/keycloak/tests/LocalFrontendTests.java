package de.captaingoldfish.scim.sdk.keycloak.tests;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;

import de.captaingoldfish.scim.sdk.keycloak.tests.setup.LocalComposition;


/**
 * execute frontend tests with a local already running keycloak setup. (the local tests are simply for *
 * increasing development performance)
 * 
 * @author Pascal Knueppel
 * @since 11.12.2020
 */
public class LocalFrontendTests extends FrontendTests
{

  public LocalFrontendTests()
  {
    super(new LocalComposition());
  }

  @Disabled("for local setup use only")
  @Override
  public List<DynamicTest> testScimForKeycloakFrontend()
  {
    return super.testScimForKeycloakFrontend();
  }
}
