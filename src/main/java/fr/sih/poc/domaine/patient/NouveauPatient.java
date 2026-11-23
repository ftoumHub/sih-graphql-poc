package fr.sih.poc.domaine.patient;

import java.time.LocalDate;

/** Commande de création d'un patient. */
public record NouveauPatient(
        String ipp,
        String nomNaissance,
        String nomUsage,
        String prenom,
        LocalDate dateNaissance,
        Sexe sexe,
        String telephone) {
}
