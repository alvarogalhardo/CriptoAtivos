package com.criptoativos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class CriptoAtivosApplicationTest {

    @Test
    void applicationClassIsAnnotatedAsSpringBootApplication() {
        assertThat(CriptoAtivosApplication.class).hasAnnotation(SpringBootApplication.class);
    }
}
