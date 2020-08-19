package de.captaingoldfish.scim.sdk.client.tests.setup;

import javax.servlet.Filter;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;

import lombok.extern.slf4j.Slf4j;


/**
 * author: Pascal Knueppel <br>
 * created at: 09.12.2019 15:44 <br>
 * <br>
 * this class is the springboot startup class for the JUnit tests. This class must be loaded in the annotation
 * {@link SpringBootTest} to load springboot properly
 */
@Slf4j
@Configuration
@EnableAutoConfiguration
public class SpringBootInitializer extends SpringBootServletInitializer
{

  /**
   * this bean will automatically be added into the servlet context and ensure that the request- and
   * response-encoding is set to UTF-8
   *
   * @return the UTF-8 filter
   */
  @Bean
  public Filter utf8Filter()
  {
    log.info("adding UTF-8 encoding filter");
    final CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
    encodingFilter.setEncoding("UTF-8");
    encodingFilter.setForceEncoding(true);
    return encodingFilter;
  }
}
