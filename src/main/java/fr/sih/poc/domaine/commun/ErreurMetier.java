package fr.sih.poc.domaine.commun;

import io.vavr.control.Either;

import java.util.List;
import java.util.Optional;

/**
 * Erreur métier ATTENDUE, traitée comme une valeur et non comme une exception.
 *
 * Tout cas d'usage susceptible d'échouer renvoie un {@code Either<ErreurMetier, T>} :
 *  - Left(erreur) : la règle n'est pas respectée, rien n'est écrit (rollback) ;
 *  - Right(valeur) : succès.
 * Les étapes s'enchaînent avec flatMap : la première erreur court-circuite la suite.
 *
 * Côté GraphQL, ce record est exposé comme le type ErreurMetier, membre des unions
 * de résultat des mutations (ResultatSejour = Sejour | ErreurMetier, etc.).
 */
public record ErreurMetier(CodeErreur code, String message, List<DetailErreur> details) {

    /** Précision sur l'erreur, typiquement le champ d'entrée concerné. */
    public record DetailErreur(String champ, String message) {
    }

    public ErreurMetier {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public static ErreurMetier de(CodeErreur code, String message) {
        return new ErreurMetier(code, message, List.of());
    }

    public static ErreurMetier introuvable(String ressource, Object identifiant) {
        return new ErreurMetier(CodeErreur.RESSOURCE_INTROUVABLE, ressource + " introuvable : " + identifiant,
                List.of(new DetailErreur(ressource, String.valueOf(identifiant))));
    }

    /** Right(valeur) si la condition est vraie, Left(erreur) sinon. */
    public static <T> Either<ErreurMetier, T> exiger(boolean condition, CodeErreur code, String message, T valeur) {
        return condition ? Either.right(valeur) : Either.left(de(code, message));
    }

    /** Convertit l'absence d'une ressource en erreur RESSOURCE_INTROUVABLE. */
    public static <T> Either<ErreurMetier, T> trouve(Optional<T> resultat, String ressource, Object identifiant) {
        return resultat.<Either<ErreurMetier, T>>map(Either::right)
                .orElseGet(() -> Either.left(introuvable(ressource, identifiant)));
    }
}
