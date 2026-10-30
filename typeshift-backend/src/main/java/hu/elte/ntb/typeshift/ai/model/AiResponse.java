package hu.elte.ntb.typeshift.ai.model;

/**
 * Response envelope shared by every AI provider (ADR-004).
 *
 * @param contentType PROSE or MATH
 * @param displayText text the user types
 * @param expectedAnswer PROSE = displayText, MATH = decimal result
 * @param contentHash SHA-256 of displayText
 * @param estimatedDurationSeconds expected typing duration
 */
public record AiResponse(
    ContentType contentType,
    String displayText,
    String expectedAnswer,
    String contentHash,
    int estimatedDurationSeconds) {}
