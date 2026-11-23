package fr.sih.poc.infrastructure.persistance;

import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;
import fr.sih.poc.domaine.patient.FiltrePatient;
import fr.sih.poc.domaine.patient.Patient;
import fr.sih.poc.domaine.patient.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static fr.sih.poc.infrastructure.persistance.Motifs.contient;

/**
 * Adaptateur : implémente le port du domaine avec Spring Data JDBC.
 * La recherche multicritère utilise l'API Criteria de JdbcAggregateTemplate
 * (équivalent "Criteria API" de JPA, en plus simple).
 */
public class PatientRepositoryAdapter implements PatientRepository {

    private final PatientJdbcRepository repository;
    private final JdbcAggregateTemplate template;

    public PatientRepositoryAdapter(PatientJdbcRepository repository, JdbcAggregateTemplate template) {
        this.repository = repository;
        this.template = template;
    }

    @Override
    public Patient enregistrer(Patient patient) {
        return repository.save(PatientEntity.depuis(patient)).versDomaine();
    }

    @Override
    public Optional<Patient> parId(Long id) {
        return repository.findById(id).map(PatientEntity::versDomaine);
    }

    @Override
    public Optional<Patient> parIpp(String ipp) {
        return repository.findByIpp(ipp).map(PatientEntity::versDomaine);
    }

    @Override
    public boolean ippExiste(String ipp) {
        return repository.existsByIpp(ipp);
    }

    @Override
    public List<Patient> parIds(Collection<Long> ids) {
        return repository.findAllById(ids).stream().map(PatientEntity::versDomaine).toList();
    }

    @Override
    public PageResultat<Patient> rechercher(FiltrePatient filtre, Pagination pagination) {
        List<Criteria> criteres = new ArrayList<>();
        if (filtre.nom() != null) {
            // (nom_naissance ILIKE ? OR nom_usage ILIKE ?) : un groupe OR dans un ET
            criteres.add(Criteria.where("nomNaissance").like(contient(filtre.nom())).ignoreCase(true)
                    .or("nomUsage").like(contient(filtre.nom())).ignoreCase(true));
        }
        if (filtre.prenom() != null) {
            criteres.add(Criteria.where("prenom").like(contient(filtre.prenom())).ignoreCase(true));
        }
        if (filtre.sexe() != null) {
            criteres.add(Criteria.where("sexe").is(filtre.sexe().name()));
        }
        if (filtre.neApres() != null) {
            criteres.add(Criteria.where("dateNaissance").greaterThanOrEquals(filtre.neApres()));
        }
        if (filtre.neAvant() != null) {
            criteres.add(Criteria.where("dateNaissance").lessThanOrEquals(filtre.neAvant()));
        }

        PageRequest pageRequest = PageRequest.of(pagination.page(), pagination.taille(),
                Sort.by("nomNaissance", "prenom", "id"));
        Page<PatientEntity> page = template.findAll(Query.query(Criteria.from(criteres)), PatientEntity.class, pageRequest);
        return new PageResultat<>(page.getContent(), pagination.page(), pagination.taille(), page.getTotalElements())
                .map(PatientEntity::versDomaine);
    }

    @Override
    public List<Patient> rechercherTexte(String texte, int limite) {
        return repository.rechercherTexte(contient(texte), limite).stream().map(PatientEntity::versDomaine).toList();
    }
}
