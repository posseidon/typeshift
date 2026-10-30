package hu.elte.ntb.typeshift.login.model;

import org.springframework.http.ResponseCookie;

/**
 * Result of a login attempt: the session cookie to set on success.
 *
 * @param success whether the credentials were accepted
 * @param cookie the session cookie carrying the signed token, or null on failure
 */
public record LoginResult(boolean success, ResponseCookie cookie) {}
