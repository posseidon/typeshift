package hu.elte.ntb.typeshift.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentication wiring loaded from configuration: the JWT signing secret
 * (injected via the {@code JWT_SECRET} environment variable, never
 * committed) and cookie flags that differ between dev and prod.
 *
 * @param jwtSecret HS256 signing secret, at least 32 characters
 * @param cookieSecure whether session cookies set the Secure flag (prod only)
 */
@ConfigurationProperties(prefix = "typeshift")
public record AuthProperties(String jwtSecret, boolean cookieSecure) {}
