package hu.elte.ntb.typeshift.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the JWT session cookie on every request outside the anonymous set
 * (Story 0.3). Missing, malformed, expired, or revoked tokens are rejected
 * with a reason code: API requests get a 401 JSON body, page requests a
 * redirect to the login view. The raw token is never logged.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  /** Mirror of SecurityConfig#permitAll — keep the two lists in sync. */
  static final List<String> ANONYMOUS_PREFIXES =
      List.of(
          "/login",
          "/signup",
          "/logout",
          "/css/",
          "/js/",
          "/v3/api-docs",
          "/swagger-ui",
          "/swagger-ui.html",
          "/api/health",
          "/favicon.ico");

  private final JwtTokenService tokens;
  private final RevocationService revocations;

  /**
   * Creates the filter.
   *
   * @param tokens the JWT verifier
   * @param revocations the blocklist probe
   */
  public JwtAuthenticationFilter(JwtTokenService tokens, RevocationService revocations) {
    this.tokens = tokens;
    this.revocations = revocations;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return ANONYMOUS_PREFIXES.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String token = AuthCookie.read(request);
    if (token == null) {
      reject(request, response, "MISSING");
      return;
    }
    Claims claims;
    try {
      claims = tokens.parse(token);
    } catch (ExpiredJwtException e) {
      reject(request, response, "EXPIRED");
      return;
    } catch (MalformedJwtException e) {
      reject(request, response, "MALFORMED");
      return;
    } catch (JwtException | IllegalArgumentException e) {
      reject(request, response, "INVALID");
      return;
    }
    if (revocations.isRevoked(claims.getId())) {
      reject(request, response, "REVOKED");
      return;
    }
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, List.of()));
    chain.doFilter(request, response);
  }

  /**
   * Rejects the request with the reason code: 401 JSON for API paths, a
   * redirect to the login view for everything else. Logs the reason only.
   */
  private void reject(
      HttpServletRequest request, HttpServletResponse response, String reason)
      throws IOException {
    LOG.warn(
        "Auth rejected: {} {} reason={}",
        request.getMethod(),
        request.getRequestURI(),
        reason);
    SecurityContextHolder.clearContext();
    if (request.getRequestURI().startsWith("/api/")) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"" + reason + "\"}");
      return;
    }
    response.sendRedirect("/login?auth=" + reason);
  }
}
