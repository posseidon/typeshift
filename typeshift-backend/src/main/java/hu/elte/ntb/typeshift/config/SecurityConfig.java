package hu.elte.ntb.typeshift.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Form login security for TypeShift: anonymous shell views, signup, API contract,
 * and HTTP healthcare; everything else requires an authenticated session.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Wires the form login chain with anonymous access to the shell views, signup,
   * static assets, and the OpenAPI contract, so the typing shell never asks an
   * unauthenticated user to log in.
   *
   * @param http the security builder
   * @return the configured filter chain
   * @throws Exception when the chain cannot be built
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/",
                        "/login",
                        "/signup",
                        "/css/**",
                        "/js/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/"))
        .logout(logout -> logout.logoutSuccessUrl("/"));
    return http.build();
  }

  /**
   * Provides the BCrypt password encoder used at signup and for login verification.
   *
   * @return the shared BCrypt encoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
