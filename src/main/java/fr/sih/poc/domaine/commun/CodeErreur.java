package fr.sih.poc.domaine.commun;

/**
 * Codes stables des erreurs métier attendues. Exposés tels quels dans l'enum GraphQL CodeErreur :
 * le client s'appuie sur le code (traduisible, testable), le message n'est qu'une aide.
 */
public enum CodeErreur {
    RESSOURCE_INTROUVABLE,
    VALIDATION,
    IPP_DEJA_UTILISE,
    DATE_NAISSANCE_FUTURE,
    DATE_FUTURE,
    SEJOUR_DEJA_EN_COURS,
    SEJOUR_NON_EN_COURS,
    MUTATION_MEME_UF,
    CHRONOLOGIE_INVALIDE,
    LETTRE_LIAISON_MANQUANTE,
    SEJOUR_ANNULE,
    DOCUMENT_DEJA_VALIDE,
    DOCUMENT_VALIDE_NON_SUPPRIMABLE,
    PAGINATION_INVALIDE,
    RECHERCHE_TROP_COURTE
}
