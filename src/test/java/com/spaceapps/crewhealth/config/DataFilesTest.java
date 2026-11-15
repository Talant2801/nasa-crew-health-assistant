package com.spaceapps.crewhealth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spaceapps.crewhealth.model.ActionCard;
import com.spaceapps.crewhealth.model.Environment;
import com.spaceapps.crewhealth.model.IndicatorDef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Guards the JSON data files: every reference resolves and every card cites a source. */
class DataFilesTest {

    private final DataLoader data = new DataLoader(new ObjectMapper());

    @Test
    void everyMissionEnvironmentIsDefined() {
        for (Environment env : Environment.values()) {
            assertNotNull(data.mission(env), "missing mission profile for " + env);
        }
    }

    @Test
    void everyIndicatorPointsToAnExistingCard() {
        for (IndicatorDef def : data.indicators()) {
            assertNotNull(data.card(def.actionCardId()), def.code() + " → unknown card " + def.actionCardId());
        }
        for (String id : List.of("spe-shelter", "dose-projection")) {
            assertNotNull(data.card(id), "radiation card missing: " + id);
        }
    }

    @Test
    void everyCardIsCompleteAndSourced() {
        for (IndicatorDef def : data.indicators()) {
            ActionCard c = data.card(def.actionCardId());
            assertFalse(c.source().isBlank(), c.id() + " has no source");
            assertFalse(c.source().contains("TODO"), c.id() + " source is still a TODO");
            assertTrue(c.sourceUrl().startsWith("https://"), c.id() + " has no https source link");
            assertTrue(c.doNow().size() >= 2 && c.doNow().size() <= 3, c.id() + " needs 2–3 doNow steps");
            assertTrue(List.of("ROUTINE", "URGENT").contains(c.escalateLevel()), c.id() + " bad escalateLevel");
        }
    }
}
