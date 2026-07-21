package com.example.notify.template;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void rendersAllPlaceholders() {
        String result = renderer.render("Hello {{userName}}, your order {{orderId}} shipped.",
                Map.of("userName", "Ada", "orderId", "A-100"));
        assertThat(result).isEqualTo("Hello Ada, your order A-100 shipped.");
    }

    @Test
    void toleratesExtraWhitespaceInsidePlaceholders() {
        String result = renderer.render("Hi {{  userName }}", Map.of("userName", "Grace"));
        assertThat(result).isEqualTo("Hi Grace");
    }

    @Test
    void collectsAllMissingVariablesNotJustTheFirst() {
        assertThatThrownBy(() -> renderer.render("{{a}} {{b}} {{c}}", Map.of("b", "x")))
                .isInstanceOf(MissingTemplateVariableException.class)
                .satisfies(ex -> assertThat(((MissingTemplateVariableException) ex).getMissingVariables())
                        .containsExactly("a", "c"));
    }

    @Test
    void extractTokensFindsDistinctNamesInOrder() {
        assertThat(renderer.extractTokens("{{a}} {{b}} {{a}}")).containsExactly("a", "b");
    }

    @Test
    void validateSchemaPassesWhenDeclaredMatchesReferenced() {
        renderer.validateSchema(List.of("userName", "orderId"), "Subject {{userName}}", "Body {{orderId}}");
        // no exception thrown
    }

    @Test
    void validateSchemaRejectsReferencedButUndeclared() {
        assertThatThrownBy(() -> renderer.validateSchema(List.of("userName"), null, "Body {{orderId}}"))
                .isInstanceOf(com.example.notify.common.errors.BadRequestException.class)
                .hasMessageContaining("orderId");
    }

    @Test
    void validateSchemaRejectsDeclaredButUnused() {
        assertThatThrownBy(() -> renderer.validateSchema(List.of("userName", "unused"), null, "Body {{userName}}"))
                .isInstanceOf(com.example.notify.common.errors.BadRequestException.class)
                .hasMessageContaining("unused");
    }
}
