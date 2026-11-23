package fr.sih.poc.api;

import fr.sih.poc.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base des tests d'API GraphQL.
 *
 * ExecutionGraphQlServiceTester exécute les requêtes directement sur le moteur GraphQL
 * (sans HTTP) : rapide, et supporte queries, mutations ET subscriptions.
 * documentName("x") charge src/test/resources/graphql-test/x.graphql : les mêmes fichiers
 * que ceux que l'on copie-colle dans GraphiQL.
 *
 * Les tests de mutation créent leurs propres patients (IPP uniques) pour ne pas
 * dépendre de l'ordre d'exécution ni modifier le jeu de démonstration.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
@Import(TestcontainersConfiguration.class)
public abstract class GraphQlTestBase {

    private static final AtomicInteger COMPTEUR_IPP = new AtomicInteger((int) (System.nanoTime() % 1_000_000));

    @Autowired
    protected ExecutionGraphQlServiceTester tester;

    protected static String nouvelIpp() {
        return String.format("9%07d", COMPTEUR_IPP.incrementAndGet() % 10_000_000);
    }

    /** Crée un patient de test et renvoie son id. */
    protected String creerPatient(String nom, String prenom) {
        return tester.documentName("m01-creer-patient")
                .variable("entree", Map.of(
                        "ipp", nouvelIpp(),
                        "nomNaissance", nom,
                        "prenom", prenom,
                        "dateNaissance", LocalDate.of(1975, 6, 1).toString(),
                        "sexe", "M"))
                .execute()
                .path("creerPatient.id").entity(String.class).get();
    }

    /** Admet un patient et renvoie l'id du séjour créé. */
    protected String admettre(String patientId, String type, String ufCode) {
        return tester.documentName("m05-admettre-patient")
                .variable("entree", Map.of("patientId", patientId, "type", type, "ufCode", ufCode, "motif", "Test"))
                .execute()
                .path("admettrePatient.id").entity(String.class).get();
    }
}
