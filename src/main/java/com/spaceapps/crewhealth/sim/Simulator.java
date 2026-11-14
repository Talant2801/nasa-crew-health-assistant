package com.spaceapps.crewhealth.sim;

import com.spaceapps.crewhealth.config.DataLoader;
import com.spaceapps.crewhealth.engine.AssessmentEngine;
import com.spaceapps.crewhealth.model.Astronaut;
import com.spaceapps.crewhealth.model.IndicatorDef;
import com.spaceapps.crewhealth.model.Reading;
import com.spaceapps.crewhealth.model.Status;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/** Generates deterministic seed data (fixed Random seed 42). Fictional crew only. */
@Component
public class Simulator {

    public static final long SEED = 42L;
    public static final int DAYS = 30;
    public static final String SOURCE = "SIMULATED_WEARABLE";

    /** Indicators reported as whole numbers. */
    private static final Set<String> INTEGER_CODES = Set.of("HR_REST", "SBP", "EXERCISE_MIN", "MOOD", "VISION");
    /** Self-reported scores: kept at baseline in seed data. */
    private static final Set<String> CONSTANT_CODES = Set.of("MOOD", "VISION");

    private final DataLoader data;
    private final AssessmentEngine engine;

    public Simulator(DataLoader data, AssessmentEngine engine) {
        this.data = data;
        this.engine = engine;
    }

    public record SeedData(List<Astronaut> crew, Map<String, Map<String, List<Reading>>> readings) {
    }

    public SeedData generate(LocalDate today) {
        Random rnd = new Random(SEED);
        List<Astronaut> crew = List.of(
                new Astronaut("ortiz", "Cmdr. Lena Ortiz", "Commander", baseline(64, 58, 118, 7.2, 4, 135, 10)),
                new Astronaut("navarro", "Dr. Kai Navarro", "Medical Officer", baseline(58, 52, 112, 6.9, 4, 150, 9)),
                new Astronaut("sato", "Eng. Mira Sato", "Flight Engineer", baseline(67, 66, 124, 7.5, 4, 120, 10)));

        Map<String, Map<String, List<Reading>>> all = new LinkedHashMap<>();
        for (Astronaut a : crew) {
            Map<String, List<Reading>> byCode = new LinkedHashMap<>();
            for (IndicatorDef def : data.indicators()) {
                double base = a.baseline().get(def.code());
                List<Double> values = greenSeries(def, base, rnd);
                if (a.id().equals("navarro") && def.code().equals("HRV")) {
                    values = decliningTail(values, base);
                }
                List<Reading> readings = new ArrayList<>();
                for (int i = 0; i < DAYS; i++) {
                    LocalDate day = today.minusDays(DAYS - 1 - i);
                    readings.add(new Reading(a.id(), def.code(), values.get(i), def.unit(), day, SOURCE));
                }
                byCode.put(def.code(), readings);
            }
            all.put(a.id(), byCode);
        }
        return new SeedData(crew, all);
    }

    private static Map<String, Double> baseline(double hr, double hrv, double sbp, double sleep,
                                                double mood, double exercise, double vision) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("HR_REST", hr);
        m.put("HRV", hrv);
        m.put("SBP", sbp);
        m.put("SLEEP_H", sleep);
        m.put("MOOD", mood);
        m.put("EXERCISE_MIN", exercise);
        m.put("VISION", vision);
        return m;
    }

    /** Baseline + small gaussian noise, redrawn until the series assesses GREEN. */
    private List<Double> greenSeries(IndicatorDef def, double base, Random rnd) {
        while (true) {
            List<Double> values = new ArrayList<>(DAYS);
            for (int i = 0; i < DAYS; i++) {
                double v = CONSTANT_CODES.contains(def.code()) ? base : base + rnd.nextGaussian() * base * 0.03;
                values.add(round(def.code(), v));
            }
            if (engine.status(def, base, values) == Status.GREEN) return values;
        }
    }

    /** Astronaut #2: HRV declines ~2% per day over the last 5 days (stays inside band → AMBER via trend). */
    private List<Double> decliningTail(List<Double> values, double base) {
        List<Double> out = new ArrayList<>(values);
        for (int k = 1; k <= 5; k++) {
            out.set(DAYS - 6 + k, round("HRV", base * (1 - 0.02 * k)));
        }
        return out;
    }

    private static double round(String code, double v) {
        return INTEGER_CODES.contains(code) ? Math.round(v) : Math.round(v * 10) / 10.0;
    }
}
