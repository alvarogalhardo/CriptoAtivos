package com.criptoativos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduling is off under the test profile so no job fires mid-assertion. */
@Configuration
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {}
