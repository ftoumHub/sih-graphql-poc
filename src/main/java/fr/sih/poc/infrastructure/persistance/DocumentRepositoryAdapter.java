package fr.sih.poc.infrastructure.persistance;

import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;
import fr.sih.poc.domaine.document.Document;
import fr.sih.poc.domaine.document.DocumentRepository;
import fr.sih.poc.domaine.document.FiltreDocument;
import fr.sih.poc.domaine.document.StatutDocument;
import fr.sih.poc.domaine.document.TypeDocument;
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

public class DocumentRepositoryAdapter implements DocumentRepository {

    private final DocumentJdbcRepository repository;
    private final JdbcAggregateTemplate template;

    public DocumentRepositoryAdapter(DocumentJdbcRepository repository, JdbcAggregateTemplate template) {
        this.repository = repository;
        this.template = template;
    }

    @Override
    public Document enregistrer(Document document) {
        return repository.save(DocumentEntity.depuis(document)).versDomaine();
    }

    @Override
    public Optional<Document> parId(Long id) {
        return repository.findById(id).map(DocumentEntity::versDomaine);
    }

    @Override
    public void supprimer(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<Document> parSejourIds(Collection<Long> sejourIds) {
        return repository.findBySejourIdInOrderByDateCreation(sejourIds).stream().map(DocumentEntity::versDomaine).toList();
    }

    @Override
    public List<Document> parPatient(Long patientId, TypeDocument type) {
        List<DocumentEntity> entites = type == null
                ? repository.findByPatientIdOrderByDateCreationDesc(patientId)
                : repository.findByPatientIdAndTypeDocumentOrderByDateCreationDesc(patientId, type.name());
        return entites.stream().map(DocumentEntity::versDomaine).toList();
    }

    @Override
    public boolean existeDocumentValide(Long sejourId, TypeDocument type) {
        return repository.existsBySejourIdAndTypeDocumentAndStatut(sejourId, type.name(), StatutDocument.VALIDE.name());
    }

    @Override
    public PageResultat<Document> rechercher(FiltreDocument filtre, Pagination pagination) {
        List<Criteria> criteres = new ArrayList<>();
        if (filtre.patientId() != null) {
            criteres.add(Criteria.where("patientId").is(filtre.patientId()));
        }
        if (filtre.sejourId() != null) {
            criteres.add(Criteria.where("sejourId").is(filtre.sejourId()));
        }
        if (filtre.types() != null && !filtre.types().isEmpty()) {
            criteres.add(Criteria.where("typeDocument").in(filtre.types().stream().map(Enum::name).toList()));
        }
        if (filtre.statut() != null) {
            criteres.add(Criteria.where("statut").is(filtre.statut().name()));
        }
        if (filtre.auteur() != null) {
            criteres.add(Criteria.where("auteur").like(contient(filtre.auteur())).ignoreCase(true));
        }
        if (filtre.titreContient() != null) {
            criteres.add(Criteria.where("titre").like(contient(filtre.titreContient())).ignoreCase(true));
        }
        PageRequest pageRequest = PageRequest.of(pagination.page(), pagination.taille(),
                Sort.by(Sort.Order.desc("dateCreation"), Sort.Order.asc("id")));
        Page<DocumentEntity> page = template.findAll(Query.query(Criteria.from(criteres)), DocumentEntity.class, pageRequest);
        return new PageResultat<>(page.getContent(), pagination.page(), pagination.taille(), page.getTotalElements())
                .map(DocumentEntity::versDomaine);
    }

    @Override
    public List<Document> rechercherTexte(String texte, int limite) {
        return repository.rechercherTexte(contient(texte), limite).stream().map(DocumentEntity::versDomaine).toList();
    }
}
