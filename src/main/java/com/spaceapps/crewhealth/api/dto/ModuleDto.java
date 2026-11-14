package com.spaceapps.crewhealth.api.dto;

import com.spaceapps.crewhealth.engine.IndicatorAssessment;
import com.spaceapps.crewhealth.model.Status;

import java.util.List;

public record ModuleDto(String bodySystem, Status status, List<IndicatorAssessment> indicators) {
}
