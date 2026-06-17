package com.interviewcoach.common.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationSecretConfigTest {

    @Test
    void productionJwtAndEncryptionSecretsDoNotHaveFallbackDefaults() throws Exception {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(applicationYaml).contains("secret: ${IC_JWT_SECRET}");
        assertThat(applicationYaml).contains("encryption-key: ${IC_AI_ENCRYPTION_KEY}");
        assertThat(applicationYaml).doesNotContain("IC_JWT_SECRET:");
        assertThat(applicationYaml).doesNotContain("IC_AI_ENCRYPTION_KEY:");
        assertThat(applicationYaml).doesNotContain("dev-ai-encryption-key");
    }
}
