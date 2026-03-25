package com.example.interview.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public CommandLineRunner noOpReportRunner() {
        // Return a no-op CommandLineRunner for tests
        return args -> {
            // Do nothing during tests
        };
    }
}
