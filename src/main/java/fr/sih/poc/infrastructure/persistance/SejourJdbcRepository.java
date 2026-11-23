package fr.sih.poc.infrastructure.persistance;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SejourJdbcRepository extends ListCrudRepository<SejourEntity, Long> {

    Optional<SejourEntity> findByNumeroSejour(String numeroSejour);

    List<SejourEntity> findByPatientIdInOrderByDateEntreeDesc(Collection<Long> patientIds);

    List<SejourEntity> findByPatientIdAndStatut(Long patientId, String statut);

    List<SejourEntity> findByStatutAndUfCouranteInOrderByDateEntree(String statut, Collection<String> ufCodes);

    @Query("""
            SELECT * FROM sejour
            WHERE numero_sejour LIKE :motif
               OR motif ILIKE :motif
            ORDER BY date_entree DESC
            LIMIT :limite
            """)
    List<SejourEntity> rechercherTexte(@Param("motif") String motif, @Param("limite") int limite);
}
