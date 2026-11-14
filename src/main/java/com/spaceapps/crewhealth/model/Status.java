package com.spaceapps.crewhealth.model;

/** Ordered from best to worst, so ordinal comparison gives severity. */
public enum Status {
    GREEN, AMBER, RED;

    public static Status worst(Status a, Status b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
