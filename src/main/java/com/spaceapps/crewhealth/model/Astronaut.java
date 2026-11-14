package com.spaceapps.crewhealth.model;

import java.util.Map;

/** A (fictional) crew member with a personal pre-flight baseline per indicator code. */
public record Astronaut(String id, String name, String role, Map<String, Double> baseline) {
}
