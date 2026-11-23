package fr.sih.poc.domaine.commun;

import java.util.List;
import java.util.function.Function;

/**
 * Une page de résultats, exposée en GraphQL par les types PatientPage / SejourPage / DocumentPage :
 * { contenu, infoPage }. graphql-java lit directement les accesseurs du record.
 */
public record PageResultat<T>(List<T> contenu, int page, int taille, long totalElements) {

    /** Métadonnées de pagination, partagées par tous les types XxxPage (type GraphQL InfoPage). */
    public record InfoPage(int page, int taille, long totalElements, int totalPages, boolean estDernierePage) {
    }

    public InfoPage infoPage() {
        int totalPages = (int) Math.ceil((double) totalElements / taille);
        return new InfoPage(page, taille, totalElements, totalPages, page >= totalPages - 1);
    }

    public <R> PageResultat<R> map(Function<T, R> conversion) {
        return new PageResultat<>(contenu.stream().map(conversion).toList(), page, taille, totalElements);
    }
}
