package de.captaingoldfish.scim.sdk.client.tests.setup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.captaingoldfish.scim.sdk.client.tests.projectsetup.WebAppConfig;


/**
 * <br>
 * <br>
 * created at: 05.05.2020
 *
 * @author Pascal Knueppel
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {WebAppConfig.class,
                                                                                       SpringBootInitializer.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ClientSdkTest
{

}
