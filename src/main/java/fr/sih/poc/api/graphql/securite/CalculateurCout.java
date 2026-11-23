package fr.sih.poc.api.graphql.securite;

import graphql.analysis.FieldComplexityCalculator;
import graphql.analysis.FieldComplexityEnvironment;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLAppliedDirectiveArgument;

/**
 * Calcul du coût d'une requête piloté par la directive de schéma @cout.
 *
 *   directive @cout(valeur: Int = 1, multiplicateur: String, cardinalite: Int) on FIELD_DEFINITION
 *
 * coût(champ) = (valeur + coût des sous-champs) x facteur
 *   - facteur = valeur de l'argument nommé par "multiplicateur" (ex. taille de page),
 *   - sinon "cardinalite" (estimation du nombre d'éléments d'une liste non paginée),
 *   - sinon 1.
 * Un champ sans directive coûte 1. L'introspection (__schema, __type...) est gratuite pour
 * ne pas bloquer GraphiQL.
 *
 * Couvre d'un seul mécanisme les listes géantes, les imbrications coûteuses ET les attaques
 * par alias (chaque alias ajoute son coût au total).
 */
public class CalculateurCout implements FieldComplexityCalculator {

    @Override
    public int calculate(FieldComplexityEnvironment env, int coutSousChamps) {
        if (env.getField().getName().startsWith("__") || env.getParentType().getName().startsWith("__")) {
            return 0;
        }
        GraphQLAppliedDirective cout = env.getFieldDefinition().getAppliedDirective("cout");
        if (cout == null) {
            return (int) Math.min(Integer.MAX_VALUE / 2, 1L + coutSousChamps);
        }
        int valeur = entier(cout.getArgument("valeur"), 1);
        int facteur = entier(cout.getArgument("cardinalite"), 1);
        String nomArgument = texte(cout.getArgument("multiplicateur"));
        if (nomArgument != null && env.getArguments().get(nomArgument) instanceof Number n) {
            facteur = n.intValue();
        }
        // calcul en long puis plafonnement : une requête très imbriquée ne doit pas déborder un int
        long total = (long) (valeur + coutSousChamps) * Math.max(1, facteur);
        return (int) Math.min(Integer.MAX_VALUE / 2, total);
    }

    private static int entier(GraphQLAppliedDirectiveArgument argument, int defaut) {
        if (argument == null) {
            return defaut;
        }
        Object valeur = argument.getValue();
        return valeur instanceof Number n ? n.intValue() : defaut;
    }

    private static String texte(GraphQLAppliedDirectiveArgument argument) {
        if (argument == null) {
            return null;
        }
        Object valeur = argument.getValue();
        return valeur instanceof String s ? s : null;
    }
}
