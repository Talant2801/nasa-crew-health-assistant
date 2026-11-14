package com.spaceapps.crewhealth.service;

import com.spaceapps.crewhealth.config.DataLoader;
import com.spaceapps.crewhealth.engine.IndicatorAssessment;
import com.spaceapps.crewhealth.model.ActionCard;
import com.spaceapps.crewhealth.model.Alert;
import com.spaceapps.crewhealth.model.Astronaut;
import com.spaceapps.crewhealth.model.Status;
import com.spaceapps.crewhealth.radiation.RadiationAssessment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Alerts are derived from the current assessment on every call; only acknowledgements are stored. */
@Service
public class AlertService {

    public static final String ROUTINE_TEXT = "Monitor and re-measure tomorrow.";

    private final CrewService crew;
    private final MissionService mission;
    private final DataLoader data;
    private final Set<String> acked = ConcurrentHashMap.newKeySet();

    public AlertService(CrewService crew, MissionService mission, DataLoader data) {
        this.crew = crew;
        this.mission = mission;
        this.data = data;
    }

    /** @param crewId null → all; otherwise that astronaut's alerts plus crew-wide alerts. */
    public List<Alert> alerts(String crewId) {
        List<Astronaut> members = crewId == null ? crew.crew() : List.of(crew.astronaut(crewId));
        List<Alert> out = new ArrayList<>();

        RadiationAssessment rad = crew.radiation();
        if (rad.status() != Status.GREEN) {
            out.add(alert("crew:RADIATION", null, "RADIATION", rad.status(), rad.summary(), rad.actionCardId()));
        }
        for (Astronaut a : members) {
            for (IndicatorAssessment ia : crew.assess(a.id())) {
                if (ia.status() != Status.GREEN) {
                    out.add(alert(a.id() + ":" + ia.code(), a.id(), ia.code(), ia.status(), ia.summary(), ia.actionCardId()));
                }
            }
        }
        out.sort(Comparator.comparing((Alert al) -> al.severity().ordinal()).reversed());
        return out;
    }

    public void acknowledge(String id) {
        acked.add(id);
    }

    public void clearAcks() {
        acked.clear();
    }

    private Alert alert(String id, String crewId, String indicator, Status severity, String summary, String cardId) {
        ActionCard card = data.card(cardId);
        boolean escalate = severity == Status.RED || (card != null && card.isUrgent());
        String text = escalate ? mission.context().escalation() : ROUTINE_TEXT;
        return new Alert(id, crewId, indicator, severity, summary, card, text, acked.contains(id));
    }
}
