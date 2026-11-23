package fr.sih.poc.infrastructure.persistance;

import fr.sih.poc.domaine.sejour.ModeSortie;
import fr.sih.poc.domaine.sejour.Mouvement;
import fr.sih.poc.domaine.sejour.Sejour;
import fr.sih.poc.domaine.sejour.StatutSejour;
import fr.sih.poc.domaine.sejour.TypeMouvement;
import fr.sih.poc.domaine.sejour.TypeSejour;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Racine de l'agrégat Séjour en persistance.
 *
 * La List<MouvementEntity> est une relation 1-N INTERNE à l'agrégat :
 *  - chargée systématiquement avec le séjour (pas de lazy loading en Spring Data JDBC) ;
 *  - à chaque save(), Spring Data JDBC supprime les lignes "mouvement" du séjour puis les ré-insère ;
 *  - "ordre" stocke l'index dans la liste, "sejour_id" la référence vers la racine.
 *
 * Le patient, lui, est un AUTRE agrégat : on ne le référence que par son identifiant (patientId).
 */
@Table("sejour")
public record SejourEntity(
        @Id Long id,
        String numeroSejour,
        Long patientId,
        String typeSejour,
        String statut,
        String ufCourante,
        String motif,
        LocalDateTime dateEntree,
        LocalDateTime dateSortie,
        String modeSortie,
        @MappedCollection(idColumn = "sejour_id", keyColumn = "ordre") List<MouvementEntity> mouvements) {

    public SejourEntity withId(Long nouvelId) {
        return new SejourEntity(nouvelId, numeroSejour, patientId, typeSejour, statut, ufCourante, motif,
                dateEntree, dateSortie, modeSortie, mouvements);
    }

    @Table("mouvement")
    public record MouvementEntity(String typeMouvement, String uf, LocalDateTime dateMouvement) {
    }

    static SejourEntity depuis(Sejour s) {
        return new SejourEntity(s.id(), s.numeroSejour(), s.patientId(), s.type().name(), s.statut().name(),
                s.ufCourante(), s.motif(), s.dateEntree(), s.dateSortie(),
                s.modeSortie() == null ? null : s.modeSortie().name(),
                s.mouvements().stream()
                        .map(m -> new MouvementEntity(m.type().name(), m.ufCode(), m.dateMouvement()))
                        .toList());
    }

    Sejour versDomaine() {
        return new Sejour(id, numeroSejour, patientId, TypeSejour.valueOf(typeSejour), StatutSejour.valueOf(statut),
                ufCourante, motif, dateEntree, dateSortie,
                modeSortie == null ? null : ModeSortie.valueOf(modeSortie),
                mouvements == null ? List.of() : mouvements.stream()
                        .map(m -> new Mouvement(TypeMouvement.valueOf(m.typeMouvement()), m.uf(), m.dateMouvement()))
                        .toList());
    }
}
