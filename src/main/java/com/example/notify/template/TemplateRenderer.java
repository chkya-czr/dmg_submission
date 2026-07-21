package com.example.notify.template;

import com.example.notify.common.errors.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders {@code {{variableName}}} placeholders against a variable map. Kept dependency-free
 * (no Spring context needed) so it's trivially unit-testable.
 */
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    /** All distinct {@code {{token}}} names referenced in the given template text, in order of
     * first appearance. */
    public Set<String> extractTokens(String template) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template == null ? "" : template);
        while (matcher.find()) {
            tokens.add(matcher.group(1));
        }
        return tokens;
    }

    /**
     * Replaces every placeholder with its value from {@code variables}. Collects *all* missing
     * variable names before throwing, rather than failing on the first one, so the caller gets a
     * complete picture in one error.
     */
    public String render(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        Map<String, String> vars = variables == null ? Map.of() : variables;
        List<String> missing = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = vars.get(name);
            if (value == null) {
                missing.add(name);
                value = "";
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        if (!missing.isEmpty()) {
            throw new MissingTemplateVariableException(missing);
        }
        return result.toString();
    }

    /**
     * At template create/update time: every token referenced in {@code subjectTemplate}/{@code
     * bodyTemplate} must be declared in {@code variablesSchema}, and every declared variable must
     * be referenced at least once — catches typos in either direction early instead of at send time.
     */
    public void validateSchema(List<String> variablesSchema, String subjectTemplate, String bodyTemplate) {
        Set<String> declared = new LinkedHashSet<>(variablesSchema == null ? List.of() : variablesSchema);
        Set<String> referenced = new LinkedHashSet<>();
        referenced.addAll(extractTokens(subjectTemplate));
        referenced.addAll(extractTokens(bodyTemplate));

        Set<String> undeclared = new LinkedHashSet<>(referenced);
        undeclared.removeAll(declared);
        Set<String> unused = new LinkedHashSet<>(declared);
        unused.removeAll(referenced);

        if (!undeclared.isEmpty() || !unused.isEmpty()) {
            StringBuilder message = new StringBuilder("Template variables_schema does not match the template text.");
            if (!undeclared.isEmpty()) {
                message.append(" Referenced but not declared: ").append(undeclared).append(".");
            }
            if (!unused.isEmpty()) {
                message.append(" Declared but never referenced: ").append(unused).append(".");
            }
            throw new BadRequestException(message.toString());
        }
    }
}
