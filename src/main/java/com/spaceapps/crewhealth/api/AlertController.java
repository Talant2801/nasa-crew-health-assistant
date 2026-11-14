package com.spaceapps.crewhealth.api;

import com.spaceapps.crewhealth.model.Alert;
import com.spaceapps.crewhealth.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alerts;

    public AlertController(AlertService alerts) {
        this.alerts = alerts;
    }

    @GetMapping
    public List<Alert> list(@RequestParam(required = false) String crewId) {
        return alerts.alerts(crewId);
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<Void> ack(@PathVariable String id) {
        alerts.acknowledge(id);
        return ResponseEntity.noContent().build();
    }
}
