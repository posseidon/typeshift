package hu.elte.ntb.typeshift.health.model;

/**
 * Heartbeat payload for the REST contract anchor endpoint.
 *
 * @param status service status marker, always {@code up} on a healthy service
 */
public record HealthSnapshot(String status) {}
