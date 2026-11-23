package fr.sih.poc.domaine.sejour;

import java.time.LocalDateTime;
import java.util.List;

/** Critères de recherche de séjours (tous optionnels, combinés en ET). */
public record FiltreSejour(
        Long patientId,
        List<StatutSejour> statuts,
        TypeSejour type,
        String ufCode,
        LocalDateTime entreApres,
        LocalDateTime entreAvant) {

    public static FiltreSejour vide() {
        return new FiltreSejour(null, null, null, null, null, null);
    }
}
