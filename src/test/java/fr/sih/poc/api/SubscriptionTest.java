package fr.sih.poc.api;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Subscriptions testées sans WebSocket : executeSubscription() renvoie un Flux
 * des réponses successives. StepVerifier s'abonne, PUIS déclenche les mutations
 * (.then), puis vérifie les événements reçus.
 */
class SubscriptionTest extends GraphQlTestBase {

    @Test
    void s02_un_abonne_recoit_les_evenements_de_son_patient_dans_l_ordre() {
        String patientId = creerPatient("Ztest-abonnement", "Hector");
        String autrePatientId = creerPatient("Ztest-autre", "Irene");

        Flux<String> types = tester.documentName("s02-evenements-patient")
                .variable("patientId", patientId)
                .variable("types", List.of("ADMISSION", "MUTATION", "SORTIE"))
                .executeSubscription()
                .toFlux("evenements.type", String.class);

        StepVerifier.create(types.take(2))
                .thenAwait(Duration.ofMillis(200))
                .then(() -> {
                    admettre(autrePatientId, "URGENCES", "3001");              // autre patient : filtré
                    String sejourId = admettre(patientId, "URGENCES", "3001");  // ADMISSION
                    tester.documentName("m08-muter-patient")                   // MUTATION
                            .variable("entree", Map.of("sejourId", sejourId, "ufDestination", "1202"))
                            .execute().path("muterPatient.numeroSejour").hasValue();
                })
                .expectNext("ADMISSION", "MUTATION")
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void s01_l_interface_EvenementSih_est_resolue_vers_le_bon_type_concret() {
        String sejourId = admettre(creerPatient("Ztest-evenements", "Jules"), "URGENCES", "3001");

        Flux<Map> evenements = tester.documentName("s01-tous-evenements")
                .executeSubscription()
                .toFlux("evenements", Map.class);

        StepVerifier.create(evenements.filter(e -> "DOCUMENT_AJOUTE".equals(e.get("type"))).take(1))
                .thenAwait(Duration.ofMillis(200))
                .then(() -> tester.documentName("m11-ajouter-document")
                        .variable("entree", Map.of("sejourId", sejourId, "type", "ORDONNANCE",
                                "titre", "Ordonnance temps réel", "auteur", "Dr Test"))
                        .execute().path("ajouterDocument.id").hasValue())
                .expectNextMatches(e -> "EvenementDocument".equals(e.get("__typename"))
                        && "Ordonnance temps réel".equals(((Map<?, ?>) e.get("document")).get("titre")))
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }
}
