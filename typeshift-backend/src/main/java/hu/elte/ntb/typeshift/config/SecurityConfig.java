package hu.elte.ntb.typeshift.config;

import hu.elte.ntb.typeshift.security.JwtAuthenticationFilter;
import hu.elte.ntb.typeshift.service.JwtTokenService;
import hu.elte.ntb.typeshift.service.RevocationService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT cookie security for TypeShift (ADR-001, Story 0.3): the filter validates
 * the auth session cookie before the authorisation rules; login, signup,
 * logout, static assets, healthcare, and docs stay anonymous. CSRF is off
 * because the session lives in a SameSite=Lax httpOnly cookie of a
 * single-owner tool and templates then need no CSRF plumbing.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Wires the JWT filter ahead of the authorisation checks; signup, login,
   * logout, static assets, healthcare, and docs remain anonymous; everything
   * else needs the validated token context.
   *
   * @param http the security builder
   * @param authFilter the token validation filter
   * @return the configured filter chain
   * @throws Exception when the chain cannot be built
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter authFilter)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .logout(logout -> logout.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/login",
                        "/signup",
                        "/logout",
                        "/css/**",
                        "/js/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * Keeps the JWT filter out of the container-level filter registration so it
   * runs exactly once, inside the security chain.
   *
   * @param authFilter the token validation filter
   * @return the disabled registration wrapper
   */
  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
      JwtAuthenticationFilter authFilter) {
    FilterRegistrationBean<JwtAuthenticationFilter> registration =
        new FilterRegistrationBean<>(authFilter);
    registration.setEnabled(false);
    return registration;
  }

  /**
   * Registers the token validation filter.
   *
   * @param tokens the JWT verifier
   * @param revocations the blocklist probe
   * @return the configured filter
   */
  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      JwtTokenService tokens, RevocationService revocations) {
    return new JwtAuthenticationFilter(tokens, revocations);
  }

  /**
   * Provides the BCrypt password encoder with cost factor 12 or higher.
   *
   * @return the shared BCrypt encoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
