package fr.sih.poc.domaine.document;

/** Commande de création d'un document (en brouillon). */
public record NouveauDocument(Long sejourId, TypeDocument type, String titre, String contenu, String auteur) {
}
