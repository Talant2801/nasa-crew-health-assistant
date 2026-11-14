package com.spaceapps.crewhealth.radiation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.spaceapps.crewhealth.config.DataLoader;
import com.spaceapps.crewhealth.model.SpaceWeather;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Replays a cached NASA DONKI SEP response from data/donki-sep.json.
 * Never calls the live API at runtime.
 */
@Component
public class DonkiAdapter {

    public static final String SOURCE = "NASA DONKI (replayed historical event)";
    private final DataLoader data;

    public DonkiAdapter(DataLoader data) {
        this.data = data;
    }

    public SpaceWeather.SpeEvent firstEvent() {
        List<JsonNode> events = data.read("data/donki-sep.json", new TypeReference<List<JsonNode>>() {});
        if (events.isEmpty()) throw new IllegalStateException("data/donki-sep.json contains no events");
        JsonNode e = events.get(0);
        JsonNode instruments = e.path("instruments");
        String instrument = instruments.isArray() && !instruments.isEmpty()
                ? instruments.get(0).path("displayName").asText(null) : null;
        return new SpaceWeather.SpeEvent(e.path("sepID").asText(null), e.path("eventTime").asText(null), instrument, SOURCE);
    }
}
