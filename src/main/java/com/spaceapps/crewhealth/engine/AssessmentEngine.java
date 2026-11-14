package com.spaceapps.crewhealth.engine;

import com.spaceapps.crewhealth.model.IndicatorDef;
import com.spaceapps.crewhealth.model.Reading;
import com.spaceapps.crewhealth.model.Status;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Rule-based comparison of an astronaut's latest reading against their OWN baseline.
 * RED: absolute limit breached. AMBER: outside baseline ± band, or 3 consecutive bad day-over-day moves. Else GREEN.
 */
@Component
public class AssessmentEngine {

    /** Relative daily slope (as fraction of baseline) below which the trend is FLAT. */
    private static final double FLAT_SLOPE_FRACTION = 0.005;

    public Status status(IndicatorDef def, double baseline, List<Double> values) {
        if (values.isEmpty()) return Status.GREEN;
        double latest = values.get(values.size() - 1);
        if (breachesLimit(def, latest)) return Status.RED;
        if (outsideBand(def, baseline, latest) || badTrend(def, values)) return Status.AMBER;
        return Status.GREEN;
    }

    /** @param readings sorted oldest → newest */
    public IndicatorAssessment assess(IndicatorDef def, double baseline, List<Reading> readings) {
        List<Double> values = readings.stream().map(Reading::value).toList();
        List<IndicatorAssessment.Point> last7 = readings.subList(Math.max(0, readings.size() - 7), readings.size())
                .stream().map(r -> new IndicatorAssessment.Point(r.timestamp(), r.value())).toList();
        Status status = status(def, baseline, values);
        Double latest = values.isEmpty() ? null : values.get(values.size() - 1);
        String trend = trend(last7.stream().map(IndicatorAssessment.Point::value).toList(), baseline);
        String summary = latest == null ? def.name() + ": no data." : summary(def, baseline, values, status);
        return new IndicatorAssessment(def.code(), def.name(), def.unit(), latest, baseline, status, trend,
                summary, last7, def.bodySystem(), def.actionCardId());
    }

    boolean breachesLimit(IndicatorDef def, double v) {
        if (def.absoluteLimit() == null) return false;
        return def.limitIsAbove() ? v > def.absoluteLimit() : v < def.absoluteLimit();
    }

    boolean outsideBand(IndicatorDef def, double baseline, double v) {
        double band = Math.abs(baseline) * def.bandPercent() / 100.0;
        return v < baseline - band || v > baseline + band;
    }

    /** True if the last 3 day-over-day changes all move in the bad direction. */
    boolean badTrend(IndicatorDef def, List<Double> values) {
        int n = values.size();
        if (n < 4) return false;
        for (int i = n - 3; i < n; i++) {
            double delta = values.get(i) - values.get(i - 1);
            boolean bad = def.badIsUp() ? delta > 0 : delta < 0;
            if (!bad) return false;
        }
        return true;
    }

    /** UP / DOWN / FLAT from least-squares slope over the given (≤7) values. */
    String trend(List<Double> values, double baseline) {
        int n = values.size();
        if (n < 2) return "FLAT";
        double meanX = (n - 1) / 2.0;
        double meanY = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double num = 0, den = 0;
        for (int i = 0; i < n; i++) {
            num += (i - meanX) * (values.get(i) - meanY);
            den += (i - meanX) * (i - meanX);
        }
        double slope = num / den;
        double threshold = Math.abs(baseline) * FLAT_SLOPE_FRACTION;
        if (slope > threshold) return "UP";
        if (slope < -threshold) return "DOWN";
        return "FLAT";
    }

    String summary(IndicatorDef def, double baseline, List<Double> values, Status status) {
        double latest = values.get(values.size() - 1);
        String head = def.name() + " " + fmt(latest) + " " + def.unit();
        double pct = baseline == 0 ? 0 : (latest - baseline) / baseline * 100.0;
        String rel = Math.round(Math.abs(pct)) == 0
                ? "at your baseline of " + fmt(baseline)
                : Math.round(Math.abs(pct)) + "% " + (pct > 0 ? "above" : "below") + " your baseline of " + fmt(baseline);
        String s = head + " — " + rel + ".";
        if (status == Status.RED) {
            s += " " + (def.limitIsAbove() ? "Above" : "Below") + " the limit of " + fmt(def.absoluteLimit()) + ".";
        } else if (status == Status.AMBER && !outsideBand(def, baseline, latest) && badTrend(def, values)) {
            s += " " + (def.badIsUp() ? "Rising" : "Falling") + " 3 days in a row.";
        }
        return s;
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }
}
