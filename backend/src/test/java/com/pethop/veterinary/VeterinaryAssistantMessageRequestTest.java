package com.pethop.veterinary;

import static org.assertj.core.api.Assertions.assertThat;

import com.pethop.veterinary.dto.VeterinaryAssistantMessageRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VeterinaryAssistantMessageRequestTest {
    private static Validator validator;

    @BeforeAll static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test void rejectsBlankMessage() {
        assertThat(validator.validate(new VeterinaryAssistantMessageRequest("  "))).isNotEmpty();
    }

    @Test void rejectsOversizedMessage() {
        assertThat(validator.validate(new VeterinaryAssistantMessageRequest("x".repeat(2001)))).isNotEmpty();
    }
}
