package fr.sih.poc.domaine.patient;

import java.time.LocalDate;

/**
 * Critères de recherche de patients. Tous optionnels, combinés en ET.
 * Ce record est utilisé tel quel comme argument GraphQL (input FiltrePatient) :
 * Spring for GraphQL instancie le record via son constructeur à partir de la Map d'arguments.
 */
public record FiltrePatient(String nom, String prenom, Sexe sexe, LocalDate neApres, LocalDate neAvant) {

    public static FiltrePatient vide() {
        return new FiltrePatient(null, null, null, null, null);
    }
}
