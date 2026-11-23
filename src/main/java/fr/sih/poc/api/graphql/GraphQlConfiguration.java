package fr.sih.poc.api.graphql;

import fr.sih.poc.api.graphql.scalaires.Scalaires;
import fr.sih.poc.api.graphql.securite.CalculateurCout;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration(proxyBeanMethods = false)
public class GraphQlConfiguration {

    /** Enregistre les scalaires personnalisés déclarés dans commun.graphqls. */
    @Bean
    RuntimeWiringConfigurer scalairesSih() {
        return wiring -> wiring
                .scalar(Scalaires.DATE)
                .scalar(Scalaires.DATE_HEURE)
                .scalar(Scalaires.IPP)
                .scalar(Scalaires.TELEPHONE);
    }

    /*
     * Protections contre le déni de service. Spring Boot enregistre automatiquement
     * tout bean Instrumentation dans le moteur graphql-java. Les deux limites sont
     * vérifiées AVANT l'exécution : une requête refusée ne touche pas la base.
     */

    /** Profondeur maximale (patient -> sejours -> patient -> sejours...). Supérieure à celle de l'introspection GraphiQL. */
    @Bean
    MaxQueryDepthInstrumentation profondeurMaximale(@Value("${sih.graphql.profondeur-max:20}") int profondeurMax) {
        return new MaxQueryDepthInstrumentation(profondeurMax);
    }

    /** Coût maximal, calculé à partir de la directive @cout du schéma. */
    @Bean
    MaxQueryComplexityInstrumentation coutMaximal(@Value("${sih.graphql.cout-max:5000}") int coutMax) {
        return new MaxQueryComplexityInstrumentation(coutMax, new CalculateurCout());
    }
}
