package fr.sih.poc.application;

import fr.sih.poc.domaine.statistiques.CompteurDocuments;
import fr.sih.poc.domaine.statistiques.OccupationUf;
import fr.sih.poc.domaine.statistiques.StatistiquesRepository;

import java.util.List;

public class StatistiquesService {

    private final StatistiquesRepository statistiques;

    public StatistiquesService(StatistiquesRepository statistiques) {
        this.statistiques = statistiques;
    }

    public long nombrePatients() {
        return statistiques.nombrePatients();
    }

    public long nombreSejoursEnCours() {
        return statistiques.nombreSejoursEnCours();
    }

    public List<OccupationUf> occupationParUf() {
        return statistiques.occupationParUf();
    }

    public List<CompteurDocuments> documentsParTypeEtStatut() {
        return statistiques.documentsParTypeEtStatut();
    }

    public Double dureeMoyenneSejoursClosJours() {
        return statistiques.dureeMoyenneSejoursClosJours();
    }
}
