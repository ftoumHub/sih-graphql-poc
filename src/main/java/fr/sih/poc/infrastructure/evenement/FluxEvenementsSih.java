package fr.sih.poc.infrastructure.evenement;

import fr.sih.poc.domaine.evenement.EvenementSih;
import fr.sih.poc.domaine.evenement.PublicateurEvenements;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Bus d'événements en mémoire basé sur un Sink Reactor "multicast" :
 * chaque abonné GraphQL (subscription) reçoit les événements émis APRÈS son abonnement.
 *
 * Limite assumée du POC : mono-instance. En production on brancherait ici
 * PostgreSQL LISTEN/NOTIFY, Kafka, RabbitMQ... sans toucher au domaine.
 */
public class FluxEvenementsSih implements PublicateurEvenements {

    private final Sinks.Many<EvenementSih> sink = Sinks.many().multicast().directBestEffort();

    @Override
    public synchronized void publier(EvenementSih evenement) {
        // synchronized : un Sink ne supporte pas les émissions concurrentes.
        // Sans abonné, l'émission est simplement ignorée (FAIL_ZERO_SUBSCRIBER).
        sink.tryEmitNext(evenement);
    }

    public Flux<EvenementSih> flux() {
        return sink.asFlux();
    }
}
