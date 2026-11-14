package com.spaceapps.crewhealth.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spaceapps.crewhealth.model.ActionCard;
import com.spaceapps.crewhealth.model.Environment;
import com.spaceapps.crewhealth.model.IndicatorDef;
import com.spaceapps.crewhealth.model.MissionContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

/** Loads the static JSON data files from src/main/resources/data/ once at startup. */
@Component
public class DataLoader {

    private final ObjectMapper mapper;
    private final Map<Environment, MissionContext> missions = new EnumMap<>(Environment.class);
    private final Map<String, IndicatorDef> indicators = new LinkedHashMap<>();
    private final Map<String, ActionCard> actionCards = new LinkedHashMap<>();

    public DataLoader(ObjectMapper mapper) {
        this.mapper = mapper;
        read("data/missions.json", new TypeReference<List<MissionContext>>() {})
                .forEach(m -> missions.put(m.env(), m));
        read("data/indicators.json", new TypeReference<List<IndicatorDef>>() {})
                .forEach(i -> indicators.put(i.code(), i));
        read("data/action-cards.json", new TypeReference<List<ActionCard>>() {})
                .forEach(c -> actionCards.put(c.id(), c));
    }

    public <T> T read(String path, TypeReference<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + path, e);
        }
    }

    public MissionContext mission(Environment env) {
        return missions.get(env);
    }

    /** Indicators in file order. */
    public Collection<IndicatorDef> indicators() {
        return Collections.unmodifiableCollection(indicators.values());
    }

    public Optional<IndicatorDef> indicator(String code) {
        return Optional.ofNullable(indicators.get(code));
    }

    public ActionCard card(String id) {
        return actionCards.get(id);
    }
}
