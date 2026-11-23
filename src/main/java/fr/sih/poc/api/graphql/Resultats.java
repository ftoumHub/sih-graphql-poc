package fr.sih.poc.api.graphql;

import fr.sih.poc.api.graphql.erreurs.ErreurMetierException;
import fr.sih.poc.domaine.commun.ErreurMetier;
import io.vavr.control.Either;

import java.util.function.Function;

/** Conversion des Either du domaine vers les deux styles de réponse GraphQL. */
final class Resultats {

    private Resultats() {
    }

    /**
     * Mutation : renvoie l'erreur OU la valeur. Le type GraphQL concret de l'union
     * (ErreurMetier, Sejour, Patient...) est déduit du nom de la classe Java renvoyée.
     */
    static Object enUnion(Either<ErreurMetier, ?> resultat) {
        return resultat.fold(Function.identity(), Function.identity());
    }

    /** Query : la valeur, ou une erreur GraphQL dans le tableau "errors". */
    static <T> T ouErreurGraphQl(Either<ErreurMetier, T> resultat) {
        return resultat.getOrElseThrow(ErreurMetierException::new);
    }
}
