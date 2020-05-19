package de.captaingoldfish.scim.sdk.client.tests.projectsetup;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import de.captaingoldfish.scim.sdk.client.tests.constants.Profiles;
import de.captaingoldfish.scim.sdk.client.tests.constants.TestConstants;


/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 12:53 <br>
 * <br>
 * spring security configuration for this test that will enable mutual client authentication to test the http
 * tls client authentication
 */
@Profile(Profiles.X509_PROFILE)
@Order(X509SecurityConfig.RANDOM_ORDER_NUMBER)
@Configuration
@EnableWebSecurity
public class X509SecurityConfig extends WebSecurityConfigurerAdapter
{

  /**
   * a order number that is given to this configuration that should not have any conflicts with other
   * spring-security configurations
   */
  public static final int RANDOM_ORDER_NUMBER = 499;

  /**
   * configure the endpoints that require mutual client authentication and add the regular expression to match
   * the username within the certificates distinguished name
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception
  {
    http.csrf()
        .disable()
        .antMatcher("/**")
        .authorizeRequests()
        .anyRequest()
        .authenticated()
        .and()
        .x509()
        .subjectPrincipalRegex("CN=(.*)"); // the regular expression to parse the username from the DN
  }

  /**
   * will do the authentication if a request comes in. The CN of the certificate must match "test" to
   * successfully authenticate
   */
  @Bean
  public UserDetailsService userDetailsService()
  {
    return username -> {
      if (TestConstants.AUTHORIZED_USERNAME.equals(username))
      {
        return new User(username, "", AuthorityUtils.commaSeparatedStringToAuthorityList(TestConstants.ADMIN_ROLE));
      }
      else if (TestConstants.UNAUTHORIZED_USERNAME.equals(username))
      {
        return new User(username, "", AuthorityUtils.createAuthorityList(TestConstants.USER_ROLE));
      }
      else
      {
        return null;
      }
    };
  }
}
