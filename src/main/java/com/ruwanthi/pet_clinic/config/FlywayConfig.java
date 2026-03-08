package com.ruwanthi.pet_clinic.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway configuration that repairs failed migrations before running migrate().
 * This clears any stuck "failed" migration records in flyway_schema_history
 * so the application can start cleanly after a previously broken migration.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Repair first: removes failed migration records from flyway_schema_history
            flyway.repair();
            // Then run pending migrations
            flyway.migrate();
        };
    }
}

