package hu.elte.ntb.typeshift.model;

/**
 * Result of a signup attempt.
 *
 * @param created whether the account was created
 * @param error refusal message when the account was not created, otherwise null
 */
public record SignupOutcome(boolean created, String error) {}
