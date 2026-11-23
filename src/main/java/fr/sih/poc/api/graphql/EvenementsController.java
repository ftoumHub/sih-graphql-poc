package fr.sih.poc.api.graphql;

import fr.sih.poc.domaine.evenement.EvenementSih;
import fr.sih.poc.domaine.evenement.TypeEvenement;
import fr.sih.poc.infrastructure.evenement.FluxEvenementsSih;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Subscriptions : la méthode renvoie un Flux ; chaque élément émis est envoyé au client
 * sous forme d'un message "next" sur la WebSocket (/graphql-ws), avec la sélection de champs demandée.
 * Le type concret (EvenementSejour / EvenementDocument) de l'interface EvenementSih est résolu
 * d'après le nom de la classe Java.
 */
@Controller
public class EvenementsController {

    private final FluxEvenementsSih fluxEvenements;

    public EvenementsController(FluxEvenementsSih fluxEvenements) {
        this.fluxEvenements = fluxEvenements;
    }

    @SubscriptionMapping
    public Flux<EvenementSih> evenements(@Argument Long patientId, @Argument List<TypeEvenement> types) {
        return fluxEvenements.flux()
                .filter(e -> patientId == null || patientId.equals(e.patientId()))
                .filter(e -> types == null || types.isEmpty() || types.contains(e.type()));
    }
}
