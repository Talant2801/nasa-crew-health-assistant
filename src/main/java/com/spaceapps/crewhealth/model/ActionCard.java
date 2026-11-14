package com.spaceapps.crewhealth.model;

import java.util.List;

public record ActionCard(
        String id,
        String title,
        String what,
        String whySpace,
        List<String> doNow,
        String escalateLevel,
        String source) {

    public boolean isUrgent() {
        return "URGENT".equalsIgnoreCase(escalateLevel);
    }
}
