package de.captaingoldfish.scim.sdk.translator;

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
    SpringApplication.run(SchemaToPojoTranslator.class, args);
  }

}
