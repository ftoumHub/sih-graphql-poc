package fr.sih.poc;

import org.springframework.boot.SpringApplication;

/**
 * Lance l'application avec un PostgreSQL Testcontainers (sans docker-compose) :
 *   mvn spring-boot:test-run
 * ou exécuter ce main depuis l'IDE.
 */
public class TestSihGraphqlApplication {

    public static void main(String[] args) {
        SpringApplication.from(SihGraphqlApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
