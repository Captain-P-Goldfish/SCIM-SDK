package de.captaingoldfish.scim.sdk.translator.shell;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * main class that starts the spring boot application
 *
 * @author Pascal Knueppel
 * @since 28.01.2022
 */
@SpringBootApplication
public class SchemaToPojoTranslator
{

  public static void main(String[] args)
  {
    SpringApplication application = new SpringApplication(SchemaToPojoTranslator.class);

    if (args.length > 0)
    {
      Map<String, Object> properties = new HashMap<>();
      properties.put("spring.shell.interactive.enabled", "false");
      application.setDefaultProperties(properties);
    }

    application.run(args);
  }

}
