package com.spaceapps.crewhealth.api.dto;

import com.spaceapps.crewhealth.model.Environment;
import jakarta.validation.constraints.NotNull;

public record MissionContextRequest(@NotNull Environment env) {
}
