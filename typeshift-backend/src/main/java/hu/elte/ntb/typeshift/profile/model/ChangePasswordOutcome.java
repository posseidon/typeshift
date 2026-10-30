package hu.elte.ntb.typeshift.profile.model;

/**
 * Result of an owner password change.
 *
 * @param success whether the password was changed
 * @param error message when the change was refused, otherwise null
 */
public record ChangePasswordOutcome(boolean success, String error) {}
