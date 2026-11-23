package fr.sih.poc.domaine.statistiques;

import fr.sih.poc.domaine.document.StatutDocument;
import fr.sih.poc.domaine.document.TypeDocument;

public record CompteurDocuments(TypeDocument type, StatutDocument statut, long nombre) {
}
