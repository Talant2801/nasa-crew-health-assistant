package com.spaceapps.crewhealth.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowTest {

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post("/api/scenarios/reset")).andExpect(status().isNoContent());
    }

    @Test
    void solarStormRaisesCrewRadiationRedAlert() throws Exception {
        mvc.perform(get("/api/crew")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));

        mvc.perform(post("/api/scenarios/solar-storm")).andExpect(status().isNoContent());

        mvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("crew:RADIATION"))
                .andExpect(jsonPath("$[0].severity").value("RED"))
                .andExpect(jsonPath("$[0].actionCard.id").value("spe-shelter"));

        mvc.perform(post("/api/mission/context").contentType(MediaType.APPLICATION_JSON).content("{\"env\":\"MARS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.env").value("MARS"))
                .andExpect(jsonPath("$.missionDay").value(30));

        mvc.perform(get("/api/alerts"))
                .andExpect(jsonPath("$[0].escalationText").value(
                        "Start the protocol yourself now. Message ground in parallel — reply will take ~24 min round trip."));
    }

    @Test
    void unknownAstronautAndScenarioReturn404() throws Exception {
        mvc.perform(get("/api/crew/nobody/modules")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Unknown astronaut: nobody"));
        mvc.perform(post("/api/scenarios/nope")).andExpect(status().isNotFound());
        mvc.perform(get("/api/crew/ortiz/readings?indicator=NOPE")).andExpect(status().isNotFound());
    }
}
