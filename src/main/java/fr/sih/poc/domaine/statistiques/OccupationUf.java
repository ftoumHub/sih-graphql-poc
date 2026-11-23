package fr.sih.poc.domaine.statistiques;

/** Occupation d'une UF : nombre de séjours en cours rapporté à la capacité. */
public record OccupationUf(String ufCode, String libelle, int capacite, int patientsPresents) {

    /** Taux d'occupation en % (null si l'UF n'a pas de lits, ex. consultations). */
    public Double tauxOccupation() {
        return capacite == 0 ? null : Math.round(1000.0 * patientsPresents / capacite) / 10.0;
    }
}
