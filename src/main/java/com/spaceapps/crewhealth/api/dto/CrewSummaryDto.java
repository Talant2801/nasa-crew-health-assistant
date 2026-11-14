package com.spaceapps.crewhealth.api.dto;

import com.spaceapps.crewhealth.model.Status;

public record CrewSummaryDto(String id, String name, String role, Status overallStatus, String topIssue) {
}
