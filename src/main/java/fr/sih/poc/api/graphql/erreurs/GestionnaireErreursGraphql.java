package fr.sih.poc.api.graphql.erreurs;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.commun.ErreurMetier;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Traduit les exceptions levées pendant l'exécution en erreurs GraphQL.
 *
 * Depuis le passage à Either, les erreurs métier ne sont plus des exceptions. Il reste :
 *  1. ErreurMetierException : Left d'une QUERY (pagination, recherche) -> erreur avec extensions.code ;
 *  2. tout le reste = erreur INATTENDUE (bug, base indisponible...) -> MASQUÉE : message générique
 *     et référence de corrélation ; le détail ne part que dans les logs serveur.
 */
@Component
public class GestionnaireErreursGraphql extends DataFetcherExceptionResolverAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GestionnaireErreursGraphql.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ErreurMetierException e) {
            ErreurMetier erreur = e.erreur();
            List<Map<String, String>> details = erreur.details().stream()
                    .map(d -> Map.of("champ", d.champ(), "message", d.message()))
                    .toList();
            return GraphqlErrorBuilder.newError(env)
                    .errorType(erreur.code() == CodeErreur.RESSOURCE_INTROUVABLE ? ErrorType.NOT_FOUND : ErrorType.BAD_REQUEST)
                    .message(erreur.message())
                    .extensions(Map.of("code", erreur.code().name(), "details", details))
                    .build();
        }
        // Masquage (error masking) : on ne divulgue ni message technique ni stacktrace.
        String reference = UUID.randomUUID().toString();
        LOG.error("Erreur inattendue [{}] sur {}", reference, env.getExecutionStepInfo().getPath(), ex);
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("Erreur interne. Référence : " + reference)
                .extensions(Map.of("reference", reference))
                .build();
    }
}
