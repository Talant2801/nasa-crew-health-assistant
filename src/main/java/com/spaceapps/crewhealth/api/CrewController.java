package com.spaceapps.crewhealth.api;

import com.spaceapps.crewhealth.api.dto.CrewSummaryDto;
import com.spaceapps.crewhealth.api.dto.ModuleDto;
import com.spaceapps.crewhealth.api.dto.ReadingsDto;
import com.spaceapps.crewhealth.engine.IndicatorAssessment;
import com.spaceapps.crewhealth.model.Astronaut;
import com.spaceapps.crewhealth.model.IndicatorDef;
import com.spaceapps.crewhealth.model.Reading;
import com.spaceapps.crewhealth.model.Status;
import com.spaceapps.crewhealth.radiation.RadiationAssessment;
import com.spaceapps.crewhealth.service.CrewService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/crew")
public class CrewController {

    private final CrewService crew;

    public CrewController(CrewService crew) {
        this.crew = crew;
    }

    @GetMapping
    public List<CrewSummaryDto> list() {
        RadiationAssessment rad = crew.radiation();
        return crew.crew().stream().map(a -> summary(a, rad)).toList();
    }

    /** Body-system modules (worst indicator wins) followed by the crew-wide RADIATION module. */
    @GetMapping("/{id}/modules")
    public List<Object> modules(@PathVariable String id) {
        Map<String, List<IndicatorAssessment>> bySystem = new LinkedHashMap<>();
        for (IndicatorAssessment ia : crew.assess(id)) {
            bySystem.computeIfAbsent(ia.bodySystem(), k -> new ArrayList<>()).add(ia);
        }
        List<Object> modules = new ArrayList<>();
        bySystem.forEach((system, list) -> modules.add(new ModuleDto(system,
                list.stream().map(IndicatorAssessment::status).reduce(Status.GREEN, Status::worst), list)));
        modules.add(crew.radiation());
        return modules;
    }

    @GetMapping("/{id}/readings")
    public ReadingsDto readings(@PathVariable String id,
                                @RequestParam String indicator,
                                @RequestParam(defaultValue = "30") int days) {
        Astronaut a = crew.astronaut(id);
        IndicatorDef def = crew.indicator(indicator);
        List<Reading> all = crew.readings(id, indicator);
        List<Reading> window = all.subList(Math.max(0, all.size() - Math.max(days, 0)), all.size());
        double baseline = a.baseline().get(def.code());
        double band = baseline * def.bandPercent() / 100.0;
        return new ReadingsDto(def.code(), def.unit(), baseline, round(baseline - band), round(baseline + band),
                def.absoluteLimit(),
                window.stream().map(r -> new IndicatorAssessment.Point(r.timestamp(), r.value())).toList());
    }

    private CrewSummaryDto summary(Astronaut a, RadiationAssessment rad) {
        IndicatorAssessment worst = crew.assess(a.id()).stream()
                .max(Comparator.comparing(ia -> ia.status().ordinal())).orElse(null);
        Status indicatorStatus = worst == null ? Status.GREEN : worst.status();
        Status overall = Status.worst(indicatorStatus, rad.status());
        String topIssue = null;
        if (overall != Status.GREEN) {
            // Personal indicator wins ties; crew-wide radiation only if strictly worse.
            topIssue = rad.status().ordinal() > indicatorStatus.ordinal() ? rad.summary() : worst.summary();
        }
        return new CrewSummaryDto(a.id(), a.name(), a.role(), overall, topIssue);
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
