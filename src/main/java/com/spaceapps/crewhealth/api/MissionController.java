package com.spaceapps.crewhealth.api;

import com.spaceapps.crewhealth.api.dto.MissionContextRequest;
import com.spaceapps.crewhealth.api.dto.MissionDto;
import com.spaceapps.crewhealth.model.MissionContext;
import com.spaceapps.crewhealth.service.MissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class MissionController {

    private final MissionService mission;

    public MissionController(MissionService mission) {
        this.mission = mission;
    }

    @GetMapping("/mission")
    public MissionDto get() {
        MissionContext c = mission.context();
        return new MissionDto(c.env(), c.label(), mission.missionDay(), c.missionLengthDays(), c.commsDelaySeconds(),
                c.escalation(), c.doseRateMSvPerDay(), c.doseSource(), mission.spaceWeather());
    }

    @PostMapping("/mission/context")
    public MissionDto setContext(@Valid @RequestBody MissionContextRequest req) {
        mission.setEnvironment(req.env());
        return get();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
