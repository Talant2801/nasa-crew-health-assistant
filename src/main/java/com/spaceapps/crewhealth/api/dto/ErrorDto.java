package com.spaceapps.crewhealth.api.dto;

public record ErrorDto(int status, String error, String message) {
}
