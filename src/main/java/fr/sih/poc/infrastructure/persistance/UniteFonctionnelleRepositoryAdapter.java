package fr.sih.poc.infrastructure.persistance;

import fr.sih.poc.domaine.referentiel.UniteFonctionnelle;
import fr.sih.poc.domaine.referentiel.UniteFonctionnelleRepository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class UniteFonctionnelleRepositoryAdapter implements UniteFonctionnelleRepository {

    private final UniteFonctionnelleJdbcRepository repository;

    public UniteFonctionnelleRepositoryAdapter(UniteFonctionnelleJdbcRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UniteFonctionnelle> toutes() {
        return repository.findAll().stream()
                .map(UniteFonctionnelleEntity::versDomaine)
                .sorted(Comparator.comparing(UniteFonctionnelle::code))
                .toList();
    }

    @Override
    public Optional<UniteFonctionnelle> parCode(String code) {
        return repository.findById(code).map(UniteFonctionnelleEntity::versDomaine);
    }

    @Override
    public List<UniteFonctionnelle> parCodes(Collection<String> codes) {
        return repository.findAllById(codes).stream().map(UniteFonctionnelleEntity::versDomaine).toList();
    }
}
