package de.captaingoldfish.scim.sdk.client.tests.setup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import de.captaingoldfish.scim.sdk.client.tests.constants.Profiles;


/**
 * <br>
 * <br>
 * created at: 05.05.2020
 *
 * @author Pascal Knueppel
 */
@ActiveProfiles(Profiles.X509_PROFILE)
@ClientSdkTest
@TestPropertySource(properties = "server.ssl.client-auth=need")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface X509AuthTest
{

}
