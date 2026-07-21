package com.example.notify.template;

import com.example.notify.common.errors.BadRequestException;

import java.util.List;

/** Thrown when rendering a template and one or more required variables were not supplied.
 * Collects every missing name (not just the first) so the caller gets one useful error. */
public class MissingTemplateVariableException extends BadRequestException {

    private final List<String> missingVariables;

    public MissingTemplateVariableException(List<String> missingVariables) {
        super("Missing required template variable(s): " + String.join(", ", missingVariables));
        this.missingVariables = List.copyOf(missingVariables);
    }

    public List<String> getMissingVariables() {
        return missingVariables;
    }
}
