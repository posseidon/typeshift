package hu.elte.ntb.typeshift.health.model;

/**
 * Reachability of the active AI provider endpoint.
 *
 * @param reachable whether the provider answered a probe
 * @param endpoint the provider base URL that was probed
 */
public record ProviderConnectivityStatus(boolean reachable, String endpoint) {}
