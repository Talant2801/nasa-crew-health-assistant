package com.spaceapps.crewhealth.service;

import com.spaceapps.crewhealth.config.DataLoader;
import com.spaceapps.crewhealth.engine.AssessmentEngine;
import com.spaceapps.crewhealth.engine.IndicatorAssessment;
import com.spaceapps.crewhealth.model.Astronaut;
import com.spaceapps.crewhealth.model.IndicatorDef;
import com.spaceapps.crewhealth.model.Reading;
import com.spaceapps.crewhealth.radiation.RadiationAssessment;
import com.spaceapps.crewhealth.radiation.RadiationService;
import com.spaceapps.crewhealth.sim.Simulator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory crew + readings store and per-astronaut assessment. */
@Service
public class CrewService {

    private final DataLoader data;
    private final AssessmentEngine engine;
    private final Simulator simulator;
    private final RadiationService radiation;
    private final MissionService mission;

    private volatile List<Astronaut> crew = List.of();
    /** astronautId → indicatorCode → readings (oldest → newest). */
    private final Map<String, Map<String, List<Reading>>> readings = new ConcurrentHashMap<>();

    public CrewService(DataLoader data, AssessmentEngine engine, Simulator simulator,
                       RadiationService radiation, MissionService mission) {
        this.data = data;
        this.engine = engine;
        this.simulator = simulator;
        this.radiation = radiation;
        this.mission = mission;
        reset();
    }

    public synchronized void reset() {
        Simulator.SeedData seed = simulator.generate(LocalDate.now());
        readings.clear();
        seed.readings().forEach((id, byCode) -> {
            Map<String, List<Reading>> copy = new ConcurrentHashMap<>();
            byCode.forEach((code, list) -> copy.put(code, new ArrayList<>(list)));
            readings.put(id, copy);
        });
        crew = seed.crew();
    }

    public List<Astronaut> crew() {
        return crew;
    }

    public Astronaut astronaut(String id) {
        return crew.stream().filter(a -> a.id().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("Unknown astronaut: " + id));
    }

    public IndicatorDef indicator(String code) {
        return data.indicator(code).orElseThrow(() -> new NotFoundException("Unknown indicator: " + code));
    }

    public List<Reading> readings(String astronautId, String code) {
        astronaut(astronautId);
        indicator(code);
        return List.copyOf(readings.get(astronautId).getOrDefault(code, List.of()));
    }

    /** Replace the values of the last N readings (used by scenarios). */
    public synchronized void overwriteLast(String astronautId, String code, double... values) {
        List<Reading> list = readings.get(astronautId).get(code);
        int start = list.size() - values.length;
        for (int i = 0; i < values.length; i++) {
            Reading r = list.get(start + i);
            list.set(start + i, new Reading(r.astronautId(), r.indicatorCode(), values[i], r.unit(), r.timestamp(), "MANUAL"));
        }
    }

    public List<IndicatorAssessment> assess(String astronautId) {
        Astronaut a = astronaut(astronautId);
        List<IndicatorAssessment> out = new ArrayList<>();
        for (IndicatorDef def : data.indicators()) {
            out.add(engine.assess(def, a.baseline().get(def.code()), readings(astronautId, def.code())));
        }
        return out;
    }

    public RadiationAssessment radiation() {
        return radiation.assess(mission.context(), mission.missionDay(), mission.spaceWeather());
    }
}
