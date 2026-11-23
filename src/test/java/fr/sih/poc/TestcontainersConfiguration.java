package fr.sih.poc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL éphémère pour les tests. @ServiceConnection remplace automatiquement
 * spring.datasource.* par les coordonnées du conteneur. Le conteneur vit aussi longtemps
 * que le contexte Spring (partagé entre classes de test grâce au cache de contexte).
 * Liquibase joue le schéma ET le jeu de démonstration (contexte "demo").
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }
}
