package de.captaingoldfish.scim.sdk.client.tests.projectsetup;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


/**
 * <br>
 * <br>
 * created at: 17.05.2020
 *
 * @author Pascal Knüppel
 */
@Order(1)
@EnableWebMvc
@Configuration
@ComponentScan({"de.captaingoldfish.scim.sdk.client.tests"})
public class WebAppConfig
{


}
