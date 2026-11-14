package com.spaceapps.crewhealth.model;

public record SpaceWeather(boolean activeSpe, SpeEvent event) {

    public static SpaceWeather quiet() {
        return new SpaceWeather(false, null);
    }

    public record SpeEvent(String sepID, String eventTime, String instrument, String source) {
    }
}
