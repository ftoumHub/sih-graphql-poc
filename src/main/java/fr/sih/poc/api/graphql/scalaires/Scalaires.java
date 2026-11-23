package fr.sih.poc.api.graphql.scalaires;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Scalaires personnalisés "Date" et "DateHeure".
 *
 * Un scalaire GraphQL, c'est 3 conversions (le "Coercing") :
 *  - serialize    : objet Java renvoyé par un contrôleur -> valeur JSON de la réponse
 *  - parseValue   : valeur JSON d'une VARIABLE ($date)    -> objet Java
 *  - parseLiteral : valeur écrite EN DUR dans la requête   -> objet Java
 * (+ valueToLiteral, utilisé pour l'introspection des valeurs par défaut).
 *
 * On pourrait utiliser la librairie graphql-java-extended-scalars ; on les écrit ici à la main
 * pour comprendre le mécanisme, et parce qu'on veut un LocalDateTime sans fuseau.
 *
 * IPP et Telephone sont des scalaires "métier" : côté Java ce sont de simples String, mais
 * le schéma documente leur format une fois pour toutes, et une valeur mal formée est rejetée
 * dès la validation de la requête, avant d'atteindre le moindre contrôleur.
 */
public final class Scalaires {

    private static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    public static final GraphQLScalarType DATE = scalaire("Date",
            "Date ISO-8601 sans heure, ex. 1948-03-12",
            LocalDate.class, LocalDate::parse, LocalDate::toString);

    public static final GraphQLScalarType DATE_HEURE = scalaire("DateHeure",
            "Date-heure locale ISO-8601, ex. 2026-11-18T09:30:00",
            LocalDateTime.class, LocalDateTime::parse, FORMAT_DATE_HEURE::format);

    public static final GraphQLScalarType IPP = texteFormate("IPP",
            "Identifiant permanent du patient : exactement 8 chiffres, ex. 00010001",
            Pattern.compile("\\d{8}"));

    public static final GraphQLScalarType TELEPHONE = texteFormate("Telephone",
            "Numéro de téléphone français sur 10 chiffres commençant par 0, sans espace, ex. 0612345678",
            Pattern.compile("0\\d{9}"));

    private Scalaires() {
    }

    private static GraphQLScalarType texteFormate(String nom, String description, Pattern format) {
        return scalaire(nom, description, String.class,
                texte -> {
                    if (!format.matcher(texte).matches()) {
                        throw new IllegalArgumentException("format attendu " + format.pattern());
                    }
                    return texte;
                },
                Function.identity());
    }

    private static <T> GraphQLScalarType scalaire(String nom, String description, Class<T> type,
                                                  Function<String, T> lecture, Function<T, String> ecriture) {
        Coercing<T, String> coercing = new Coercing<>() {

            @Override
            public String serialize(Object valeur, GraphQLContext contexte, Locale locale) {
                if (type.isInstance(valeur)) {
                    return ecriture.apply(type.cast(valeur));
                }
                throw new CoercingSerializeException(nom + " : " + type.getSimpleName() + " attendu, reçu " + valeur.getClass());
            }

            @Override
            public T parseValue(Object entree, GraphQLContext contexte, Locale locale) {
                if (entree instanceof String texte) {
                    return lire(texte, CoercingParseValueException::new);
                }
                throw new CoercingParseValueException(nom + " : chaîne attendue, reçu " + entree);
            }

            @Override
            public T parseLiteral(Value<?> entree, CoercedVariables variables, GraphQLContext contexte, Locale locale) {
                if (entree instanceof StringValue chaine) {
                    return lire(chaine.getValue(), CoercingParseLiteralException::new);
                }
                throw new CoercingParseLiteralException(nom + " : littéral chaîne attendu, reçu " + entree);
            }

            @Override
            public Value<?> valueToLiteral(Object entree, GraphQLContext contexte, Locale locale) {
                return StringValue.newStringValue(serialize(entree, contexte, locale)).build();
            }

            private T lire(String texte, Function<String, RuntimeException> erreur) {
                try {
                    return lecture.apply(texte);
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    throw erreur.apply(nom + " invalide : '" + texte + "' (" + description + ")");
                }
            }
        };
        return GraphQLScalarType.newScalar().name(nom).description(description).coercing(coercing).build();
    }
}
