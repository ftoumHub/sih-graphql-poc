package fr.sih.poc.domaine.referentiel;

/**
 * Unité fonctionnelle (UF) : maille élémentaire d'activité de l'hôpital
 * (un service peut contenir plusieurs UF). Données de référentiel en lecture seule.
 */
public record UniteFonctionnelle(String code, String libelle, String service, int capacite) {
}
