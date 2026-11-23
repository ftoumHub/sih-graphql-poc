package fr.sih.poc.domaine.statistiques;

import java.util.List;

/**
 * Port de lecture dédié aux indicateurs. Chaque méthode correspond à un champ
 * du type GraphQL Statistiques : elle n'est exécutée que si le client demande ce champ.
 */
public interface StatistiquesRepository {

    long nombrePatients();

    long nombreSejoursEnCours();

    List<OccupationUf> occupationParUf();

    List<CompteurDocuments> documentsParTypeEtStatut();

    Double dureeMoyenneSejoursClosJours();
}
