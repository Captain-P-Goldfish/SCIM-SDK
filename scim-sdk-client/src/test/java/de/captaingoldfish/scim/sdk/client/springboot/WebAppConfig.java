package de.captaingoldfish.scim.sdk.client.springboot;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * author Pascal Knueppel <br>
 * created at: 09.12.2019 - 15:26 <br>
 * configuration for the springboot integration test
 */
@Configuration
@EnableWebMvc
@ComponentScan({"de.captaingoldfish.scim.sdk.client"})
public class WebAppConfig implements WebMvcConfigurer
{

}
