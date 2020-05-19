package de.captaingoldfish.scim.sdk.client.tests.projectsetup;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import de.captaingoldfish.scim.sdk.client.tests.constants.Profiles;
import de.captaingoldfish.scim.sdk.client.tests.constants.TestConstants;


/**
 * author Pascal Knueppel <br>
 * created at: 11.12.2019 - 12:53 <br>
 * <br>
 * spring security configuration for this test that will enable mutual client authentication to test the http
 * tls client authentication
 */
@Profile(Profiles.BASIC_PROFILE)
@Order(BasicAuthSecurityConfig.ORDER_NUMBER)
@Configuration
@EnableWebSecurity
public class BasicAuthSecurityConfig extends WebSecurityConfigurerAdapter
{

  /**
   * a order number that is given to this configuration that should not have any conflicts with other
   * spring-security configurations
   */
  public static final int ORDER_NUMBER = 498;

  /**
   * configure the endpoints that require mutual client authentication and add the regular expression to match
   * the username within the certificates distinguished name
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception
  {
    // @formatter:off
    http.csrf()
        .disable()
        .antMatcher("/**")
        .authorizeRequests()
        .anyRequest()
        .authenticated()
        .and()
        .httpBasic();
    // @formatter:on
  }

  /**
   * holds the user that can be used to login
   */
  @Bean
  @Override
  public UserDetailsService userDetailsService()
  {
    UserDetails authorizedUser = User.withDefaultPasswordEncoder()
                                     .username(TestConstants.AUTHORIZED_USERNAME)
                                     .password(TestConstants.PASSWORD)
                                     .roles(TestConstants.ADMIN_ROLE)
                                     .build();
    UserDetails unauthorizedUser = User.withDefaultPasswordEncoder()
                                       .username(TestConstants.UNAUTHORIZED_USERNAME)
                                       .password(TestConstants.PASSWORD)
                                       .roles(TestConstants.USER_ROLE)
                                       .build();

    return new InMemoryUserDetailsManager(authorizedUser, unauthorizedUser);
  }
}
