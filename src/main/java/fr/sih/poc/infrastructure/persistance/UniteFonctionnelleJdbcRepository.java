package fr.sih.poc.infrastructure.persistance;

import org.springframework.data.repository.ListCrudRepository;

public interface UniteFonctionnelleJdbcRepository extends ListCrudRepository<UniteFonctionnelleEntity, String> {
}
