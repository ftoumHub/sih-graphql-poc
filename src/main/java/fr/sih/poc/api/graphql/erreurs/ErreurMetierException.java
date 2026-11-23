package fr.sih.poc.api.graphql.erreurs;

import fr.sih.poc.domaine.commun.ErreurMetier;

/**
 * Pont entre Either et le mécanisme d'erreurs GraphQL, utilisé UNIQUEMENT pour les queries.
 *
 * Choix de conception :
 *  - mutations : l'échec d'une règle métier est un résultat attendu => renvoyé comme DONNÉE
 *    (union ResultatXxx = Xxx | ErreurMetier), sélectionnable et typé dans le schéma ;
 *  - queries : une page de 500 éléments ou une recherche d'un caractère sont des erreurs du
 *    développeur client => erreur GraphQL classique (tableau "errors", champ à null).
 */
public class ErreurMetierException extends RuntimeException {

    private final transient ErreurMetier erreur;

    public ErreurMetierException(ErreurMetier erreur) {
        super(erreur.message());
        this.erreur = erreur;
    }

    public ErreurMetier erreur() {
        return erreur;
    }
}
