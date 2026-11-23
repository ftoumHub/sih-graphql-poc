package fr.sih.poc.domaine.patient;

/** Commande de modification partielle : un champ null signifie "inchangé". */
public record ModificationPatient(String nomNaissance, String nomUsage, String prenom, String telephone) {
}
