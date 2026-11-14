package com.spaceapps.crewhealth.engine;

import com.spaceapps.crewhealth.model.IndicatorDef;
import com.spaceapps.crewhealth.model.Status;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessmentEngineTest {

    private final AssessmentEngine engine = new AssessmentEngine();
    private final IndicatorDef hr = new IndicatorDef("HR_REST", "Resting heart rate", "bpm", "CARDIOVASCULAR",
            10, "UP", 100.0, "ABOVE", "test", "elevated-hr");

    @Test
    void redWhenAbsoluteLimitBreached() {
        assertEquals(Status.RED, engine.status(hr, 64, List.of(64.0, 65.0, 64.0, 105.0)));
    }

    @Test
    void amberWhenOutsideBand() {
        assertEquals(Status.AMBER, engine.status(hr, 64, List.of(64.0, 65.0, 64.0, 74.0, 72.0)));
    }

    @Test
    void amberOnThreeDayBadTrend() {
        // all inside 57.6..70.4 band, but rising 3 days in a row
        assertEquals(Status.AMBER, engine.status(hr, 64, List.of(63.0, 64.0, 65.0, 66.0)));
    }

    @Test
    void greenOtherwise() {
        assertEquals(Status.GREEN, engine.status(hr, 64, List.of(64.0, 63.0, 65.0, 64.0, 66.0, 63.0)));
    }

    @Test
    void summaryMentionsBaseline() {
        var a = engine.assess(hr, 64, List.of(
                new com.spaceapps.crewhealth.model.Reading("x", "HR_REST", 74, "bpm", java.time.LocalDate.now(), "MANUAL")));
        assertEquals("Resting heart rate 74 bpm — 16% above your baseline of 64.", a.summary());
    }
}
