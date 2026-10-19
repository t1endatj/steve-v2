package com.steve.v2.core.perception;

/** Text snapshot captured on the game thread. No live world objects cross into the HTTP client. */
public record WorldSnapshot(String agentName, String dimension, String situation) {}
