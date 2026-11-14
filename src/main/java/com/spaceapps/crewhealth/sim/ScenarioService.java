package com.spaceapps.crewhealth.sim;

import com.spaceapps.crewhealth.model.SpaceWeather;
import com.spaceapps.crewhealth.radiation.DonkiAdapter;
import com.spaceapps.crewhealth.service.AlertService;
import com.spaceapps.crewhealth.service.CrewService;
import com.spaceapps.crewhealth.service.MissionService;
import com.spaceapps.crewhealth.service.NotFoundException;
import org.springframework.stereotype.Service;

/** Demo scenarios: solar-storm, sleep-deficit, reset. */
@Service
public class ScenarioService {

    private final CrewService crew;
    private final MissionService mission;
    private final AlertService alerts;
    private final DonkiAdapter donki;

    public ScenarioService(CrewService crew, MissionService mission, AlertService alerts, DonkiAdapter donki) {
        this.crew = crew;
        this.mission = mission;
        this.alerts = alerts;
        this.donki = donki;
    }

    public void run(String name) {
        switch (name) {
            case "solar-storm" -> mission.setSpaceWeather(new SpaceWeather(true, donki.firstEvent()));
            case "sleep-deficit" -> {
                String first = crew.crew().get(0).id();
                crew.overwriteLast(first, "SLEEP_H", 4.6, 4.8, 4.5);
                crew.overwriteLast(first, "MOOD", 3, 3, 2);
            }
            case "reset" -> {
                crew.reset();
                mission.reset();
                alerts.clearAcks();
            }
            default -> throw new NotFoundException("Unknown scenario: " + name);
        }
    }
}
