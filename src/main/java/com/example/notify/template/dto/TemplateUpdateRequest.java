package com.example.notify.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TemplateUpdateRequest(
        String subjectTemplate,
        @NotBlank String bodyTemplate,
        @NotNull List<String> variablesSchema
) {
}
