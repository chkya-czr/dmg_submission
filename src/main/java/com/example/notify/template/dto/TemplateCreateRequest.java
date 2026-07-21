package com.example.notify.template.dto;

import com.example.notify.common.model.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record TemplateCreateRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9_]{1,100}$", message = "must be lowercase alphanumeric/underscore") String code,
        @NotNull Channel channel,
        String subjectTemplate,
        @NotBlank String bodyTemplate,
        @NotNull List<String> variablesSchema
) {
}
