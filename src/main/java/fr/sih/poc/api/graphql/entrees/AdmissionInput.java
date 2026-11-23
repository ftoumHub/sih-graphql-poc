package fr.sih.poc.api.graphql.entrees;

import fr.sih.poc.domaine.sejour.Admission;
import fr.sih.poc.domaine.sejour.TypeSejour;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** L'ID GraphQL (une chaîne) est converti automatiquement en Long par Spring for GraphQL. */
public record AdmissionInput(
        Long patientId,
        TypeSejour type,
        String ufCode,
        @Size(max = 255) String motif,
        LocalDateTime dateEntree) {

    public Admission versCommande() {
        return new Admission(patientId, type, ufCode, motif, dateEntree);
    }
}
