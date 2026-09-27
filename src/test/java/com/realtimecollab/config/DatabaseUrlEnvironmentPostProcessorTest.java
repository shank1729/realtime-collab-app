package com.realtimecollab.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsPostgresUrlToSpringDatasourceProperties() {
        Map<String, Object> properties = DatabaseUrlEnvironmentPostProcessor
                .toSpringDatasourceProperties(
                        "postgresql://neondb_owner:p%40ssword@example.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require")
                .orElseThrow();

        assertThat(properties)
                .containsEntry("spring.datasource.url",
                        "jdbc:postgresql://example.ap-southeast-1.aws.neon.tech/neondb?sslmode=require")
                .containsEntry("spring.datasource.username", "neondb_owner")
                .containsEntry("spring.datasource.password", "p@ssword");
    }

    @Test
    void leavesJdbcUrlForApplicationProperties() {
        assertThat(DatabaseUrlEnvironmentPostProcessor
                .toSpringDatasourceProperties("jdbc:postgresql://localhost:5432/collabdb"))
                .isEmpty();
    }
}
