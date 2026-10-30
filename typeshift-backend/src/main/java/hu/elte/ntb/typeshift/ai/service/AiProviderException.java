package hu.elte.ntb.typeshift.ai.service;

/** A provider call failed; the cause carries the full stack trace (ADR-017). */
public final class AiProviderException extends RuntimeException {

  public AiProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
