package fr.sih.poc.domaine.document;

import java.util.List;

/** Critères de recherche de documents (tous optionnels, combinés en ET). */
public record FiltreDocument(
        Long patientId,
        Long sejourId,
        List<TypeDocument> types,
        StatutDocument statut,
        String auteur,
        String titreContient) {

    public static FiltreDocument vide() {
        return new FiltreDocument(null, null, null, null, null, null);
    }
}
