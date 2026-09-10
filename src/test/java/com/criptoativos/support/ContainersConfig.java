package com.criptoativos.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Declares the Postgres container as a bean so Spring Boot manages its lifecycle and wires the
 * datasource via {@link ServiceConnection}.
 *
 * <p>Because Spring caches the test context across classes, every {@code *IT} that extends {@link
 * AbstractIT} shares a single container instead of starting its own.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
