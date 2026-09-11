package com.criptoativos.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Base class for every integration test: full Spring context against a real Postgres. */
@SpringBootTest
@ActiveProfiles("test")
@Import(ContainersConfig.class)
public abstract class AbstractIT {}
