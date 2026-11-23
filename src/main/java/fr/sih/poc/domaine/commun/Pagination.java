package fr.sih.poc.domaine.commun;

import io.vavr.control.Either;

/**
 * Demande de page (numérotée à partir de 0). La taille est bornée pour éviter
 * qu'un client ne rapatrie toute la base en une requête.
 */
public record Pagination(int page, int taille) {

    public static final int TAILLE_PAR_DEFAUT = 20;
    public static final int TAILLE_MAX = 100;

    public static Either<ErreurMetier, Pagination> de(Integer page, Integer taille) {
        int p = page == null ? 0 : page;
        int t = taille == null ? TAILLE_PAR_DEFAUT : taille;
        return ErreurMetier.exiger(p >= 0, CodeErreur.PAGINATION_INVALIDE, "Le numéro de page doit être >= 0", p)
                .flatMap(ok -> ErreurMetier.exiger(t >= 1 && t <= TAILLE_MAX, CodeErreur.PAGINATION_INVALIDE,
                        "La taille de page doit être comprise entre 1 et " + TAILLE_MAX, new Pagination(p, t)));
    }
}
