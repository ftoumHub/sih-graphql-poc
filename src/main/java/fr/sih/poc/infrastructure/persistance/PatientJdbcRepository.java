package fr.sih.poc.infrastructure.persistance;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data JDBC. Trois façons de requêter sont montrées :
 *  - méthodes héritées (findById, findAllById, save...)
 *  - requêtes dérivées du nom de méthode (findByIpp, existsByIpp)
 *  - SQL explicite avec @Query
 */
public interface PatientJdbcRepository extends ListCrudRepository<PatientEntity, Long> {

    Optional<PatientEntity> findByIpp(String ipp);

    boolean existsByIpp(String ipp);

    @Query("""
            SELECT * FROM patient
            WHERE nom_naissance ILIKE :motif
               OR nom_usage ILIKE :motif
               OR prenom ILIKE :motif
               OR ipp LIKE :motif
            ORDER BY nom_naissance, prenom
            LIMIT :limite
            """)
    List<PatientEntity> rechercherTexte(@Param("motif") String motif, @Param("limite") int limite);
}
