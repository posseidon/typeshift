package hu.elte.ntb.typeshift.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Azure model response before the application adds its content hash (ADR-004).
 *
 * @param displayText text the user types
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiResponse(
    String displayText) {}
