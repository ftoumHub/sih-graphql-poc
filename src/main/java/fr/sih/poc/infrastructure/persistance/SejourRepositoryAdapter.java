package fr.sih.poc.infrastructure.persistance;

import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;
import fr.sih.poc.domaine.sejour.FiltreSejour;
import fr.sih.poc.domaine.sejour.Sejour;
import fr.sih.poc.domaine.sejour.SejourRepository;
import fr.sih.poc.domaine.sejour.StatutSejour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static fr.sih.poc.infrastructure.persistance.Motifs.contient;

public class SejourRepositoryAdapter implements SejourRepository {

    private final SejourJdbcRepository repository;
    private final JdbcAggregateTemplate template;
    private final JdbcClient jdbcClient;

    public SejourRepositoryAdapter(SejourJdbcRepository repository, JdbcAggregateTemplate template, JdbcClient jdbcClient) {
        this.repository = repository;
        this.template = template;
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Sejour enregistrer(Sejour sejour) {
        return repository.save(SejourEntity.depuis(sejour)).versDomaine();
    }

    @Override
    public Optional<Sejour> parId(Long id) {
        return repository.findById(id).map(SejourEntity::versDomaine);
    }

    @Override
    public Optional<Sejour> parNumero(String numeroSejour) {
        return repository.findByNumeroSejour(numeroSejour).map(SejourEntity::versDomaine);
    }

    @Override
    public List<Sejour> parIds(Collection<Long> ids) {
        return repository.findAllById(ids).stream().map(SejourEntity::versDomaine).toList();
    }

    @Override
    public List<Sejour> parPatientIds(Collection<Long> patientIds) {
        return repository.findByPatientIdInOrderByDateEntreeDesc(patientIds).stream().map(SejourEntity::versDomaine).toList();
    }

    @Override
    public List<Sejour> enCoursParUf(Collection<String> ufCodes) {
        return repository.findByStatutAndUfCouranteInOrderByDateEntree(StatutSejour.EN_COURS.name(), ufCodes)
                .stream().map(SejourEntity::versDomaine).toList();
    }

    @Override
    public List<Sejour> enCoursDuPatient(Long patientId) {
        return repository.findByPatientIdAndStatut(patientId, StatutSejour.EN_COURS.name())
                .stream().map(SejourEntity::versDomaine).toList();
    }

    @Override
    public PageResultat<Sejour> rechercher(FiltreSejour filtre, Pagination pagination) {
        List<Criteria> criteres = new ArrayList<>();
        if (filtre.patientId() != null) {
            criteres.add(Criteria.where("patientId").is(filtre.patientId()));
        }
        if (filtre.statuts() != null && !filtre.statuts().isEmpty()) {
            criteres.add(Criteria.where("statut").in(filtre.statuts().stream().map(Enum::name).toList()));
        }
        if (filtre.type() != null) {
            criteres.add(Criteria.where("typeSejour").is(filtre.type().name()));
        }
        if (filtre.ufCode() != null) {
            criteres.add(Criteria.where("ufCourante").is(filtre.ufCode()));
        }
        if (filtre.entreApres() != null) {
            criteres.add(Criteria.where("dateEntree").greaterThanOrEquals(filtre.entreApres()));
        }
        if (filtre.entreAvant() != null) {
            criteres.add(Criteria.where("dateEntree").lessThan(filtre.entreAvant()));
        }
        PageRequest pageRequest = PageRequest.of(pagination.page(), pagination.taille(),
                Sort.by(Sort.Order.desc("dateEntree"), Sort.Order.asc("id")));
        Page<SejourEntity> page = template.findAll(Query.query(Criteria.from(criteres)), SejourEntity.class, pageRequest);
        return new PageResultat<>(page.getContent(), pagination.page(), pagination.taille(), page.getTotalElements())
                .map(SejourEntity::versDomaine);
    }

    @Override
    public List<Sejour> rechercherTexte(String texte, int limite) {
        return repository.rechercherTexte(contient(texte), limite).stream().map(SejourEntity::versDomaine).toList();
    }

    @Override
    public String prochainNumeroSejour(int annee) {
        // Spring Data JDBC n'empêche pas le SQL direct : JdbcClient (Spring 6.1) pour une séquence.
        Long valeur = jdbcClient.sql("SELECT nextval('seq_numero_sejour')").query(Long.class).single();
        return String.format("%d%06d", annee, valeur);
    }
}
