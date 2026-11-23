package fr.sih.poc.api.graphql.entrees;

import fr.sih.poc.domaine.patient.ModificationPatient;
import jakarta.validation.constraints.Size;

/** Modification partielle : un champ absent (null) n'est pas modifié. */
public record ModifierPatientInput(
        Long patientId,
        @Size(min = 1, max = 100) String nomNaissance,
        @Size(min = 1, max = 100) String nomUsage,
        @Size(min = 1, max = 100) String prenom,
        String telephone) {

    public ModificationPatient versCommande() {
        return new ModificationPatient(nomNaissance, nomUsage, prenom, telephone);
    }
}
