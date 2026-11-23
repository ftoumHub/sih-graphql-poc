package fr.sih.poc.infrastructure.persistance;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DocumentJdbcRepository extends ListCrudRepository<DocumentEntity, Long> {

    List<DocumentEntity> findBySejourIdInOrderByDateCreation(Collection<Long> sejourIds);

    List<DocumentEntity> findByPatientIdOrderByDateCreationDesc(Long patientId);

    List<DocumentEntity> findByPatientIdAndTypeDocumentOrderByDateCreationDesc(Long patientId, String typeDocument);

    boolean existsBySejourIdAndTypeDocumentAndStatut(Long sejourId, String typeDocument, String statut);

    @Query("""
            SELECT * FROM document
            WHERE titre ILIKE :motif
               OR contenu ILIKE :motif
            ORDER BY date_creation DESC
            LIMIT :limite
            """)
    List<DocumentEntity> rechercherTexte(@Param("motif") String motif, @Param("limite") int limite);
}
