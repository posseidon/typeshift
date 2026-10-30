package hu.elte.ntb.typeshift.model;

/**
 * Read-only snapshot of the owner profile for the view (Story 0.5).
 *
 * @param username unique login name
 * @param displayName shown name
 * @param memberSince account creation date formatted yyyy-MM-dd
 * @param currentStage current stage number from the learning profile
 */
public record ProfileView(String username, String displayName, String memberSince, int currentStage) {}
