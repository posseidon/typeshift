package hu.elte.ntb.typeshift.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.ResponseCookie;

/**
 * The ADR-001 session cookie: httpOnly, SameSite Lax, Secure in prod, thirty
 * day lifetime; not readable from JavaScript and never placed in URLs.
 */
public final class AuthCookie {

  /** Name of the session cookie carrying the signed JWT. */
  public static final String NAME = "typeshift_token";

  private static final Duration MAX_AGE = Duration.ofDays(30);

  private AuthCookie() {}

  /**
   * Builds a fresh session cookie.
   *
   * @param token the signed compact JWT
   * @param secure whether the Secure flag must be set (prod)
   * @return the cookie value to attach to the response
   */
  public static ResponseCookie create(String token, boolean secure) {
    return ResponseCookie.from(NAME, token)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/")
        .maxAge(MAX_AGE)
        .build();
  }

  /**
   * Builds the expired cookie that clears the session from the browser.
   *
   * @return the clearing cookie
   */
  public static ResponseCookie clear() {
    return ResponseCookie.from(NAME, "")
        .httpOnly(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(0)
        .build();
  }

  /**
   * Reads the session cookie from the request, if present.
   *
   * @param request the incoming request
   * @return the raw token string, or null when no cookie is present
   */
  public static String read(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (NAME.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
