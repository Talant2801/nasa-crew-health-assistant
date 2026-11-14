package com.spaceapps.crewhealth.api;

import com.spaceapps.crewhealth.sim.ScenarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioService scenarios;

    public ScenarioController(ScenarioService scenarios) {
        this.scenarios = scenarios;
    }

    @PostMapping("/{name}")
    public ResponseEntity<Void> run(@PathVariable String name) {
        scenarios.run(name);
        return ResponseEntity.noContent().build();
    }
}
