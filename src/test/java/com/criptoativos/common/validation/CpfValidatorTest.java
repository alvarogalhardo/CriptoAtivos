package com.criptoativos.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "16899535009", "168.995.350-09"})
    void acceptsValidCpf(String cpf) {
        assertThat(validator.isValid(cpf, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "52998224724", // wrong second check digit
                "52998224715", // wrong first check digit
                "11111111111", // repeated digits pass the checksum but are not real
                "00000000000",
                "123",
                "abcdefghijk",
                "529982247259", // too long
                ""
            })
    void rejectsInvalidCpf(String cpf) {
        assertThat(validator.isValid(cpf, null)).isFalse();
    }

    @Test
    void nullIsDelegatedToNotNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
