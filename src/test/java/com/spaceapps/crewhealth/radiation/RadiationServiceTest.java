package com.spaceapps.crewhealth.radiation;

import com.spaceapps.crewhealth.model.Environment;
import com.spaceapps.crewhealth.model.MissionContext;
import com.spaceapps.crewhealth.model.SpaceWeather;
import com.spaceapps.crewhealth.model.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RadiationServiceTest {

    private final RadiationService service = new RadiationService();
    private final MissionContext iss = new MissionContext(Environment.ISS, "ISS", 0.5, 180, 0, "esc", "src");
    private final MissionContext mars = new MissionContext(Environment.MARS, "Mars", 1.8, 900, 720, "esc", "src");

    @Test
    void issDay30Gives15mSv() {
        RadiationAssessment r = service.assess(iss, 30, SpaceWeather.quiet());
        assertEquals(15.0, r.currentDoseMSv(), 1e-9);
        assertEquals(Status.GREEN, r.status());
    }

    @Test
    void marsProjectionIsAmber() {
        RadiationAssessment r = service.assess(mars, 30, SpaceWeather.quiet());
        assertEquals(1620.0, r.projectedDoseMSv(), 1e-9);
        assertEquals(Status.AMBER, r.status());
        assertEquals("dose-projection", r.actionCardId());
    }

    @Test
    void activeSpeIsRed() {
        RadiationAssessment r = service.assess(iss, 30, new SpaceWeather(true, null));
        assertEquals(Status.RED, r.status());
        assertEquals("spe-shelter", r.actionCardId());
    }
}
