package com.example.notify.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** A single injectable {@link Clock} bean so time-dependent logic (rate limiting, backoff,
 * scheduling) can be tested deterministically instead of calling {@code Instant.now()} directly. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
