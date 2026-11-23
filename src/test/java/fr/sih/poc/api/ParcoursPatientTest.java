package fr.sih.poc.api;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutations : on rejoue le parcours complet d'un patient
 * création -> admission -> mutation -> document -> validation -> sortie.
 */
class ParcoursPatientTest extends GraphQlTestBase {

    @Test
    void m01_creer_un_patient_met_le_nom_en_majuscules() {
        String ipp = nouvelIpp();
        GraphQlTester.Response reponse = tester.documentName("m01-creer-patient")
                .variable("entree", Map.of("ipp", ipp, "nomNaissance", "Ztest-creation", "prenom", "Alice",
                        "dateNaissance", "1980-04-15", "sexe", "F", "telephone", "0611223344"))
                .execute();
        reponse.path("creerPatient.id").hasValue();
        reponse.path("creerPatient.ipp").entity(String.class).isEqualTo(ipp);
        reponse.path("creerPatient.nomNaissance").entity(String.class).isEqualTo("ZTEST-CREATION");
        reponse.path("creerPatient.dateCreation").entity(String.class)
                .satisfies(d -> assertThat(d).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));

        // Relecture via une query : la mutation a bien été persistée
        tester.documentName("q01-patient-par-id")
                .variable("id", reponse.path("creerPatient.id").entity(String.class).get())
                .execute()
                .path("patient.ipp").entity(String.class).isEqualTo(ipp);
    }

    @Test
    void m04_la_modification_est_partielle() {
        String id = creerPatient("Ztest-modification", "Bruno");
        GraphQlTester.Response reponse = tester.documentName("m04-modifier-patient")
                .variable("entree", Map.of("patientId", id, "nomUsage", "Ztest-usage", "telephone", "0699887766"))
                .execute();
        reponse.path("modifierPatient.nomNaissance").entity(String.class).isEqualTo("ZTEST-MODIFICATION");
        reponse.path("modifierPatient.nomUsage").entity(String.class).isEqualTo("ZTEST-USAGE");
        reponse.path("modifierPatient.nomAffiche").entity(String.class).isEqualTo("ZTEST-USAGE");
        reponse.path("modifierPatient.telephone").entity(String.class).isEqualTo("0699887766");
    }

    @Test
    void parcours_complet_admission_mutation_lettre_de_liaison_sortie() {
        String patientId = creerPatient("Ztest-parcours", "Chloe");

        // 1. Admission aux urgences adultes
        GraphQlTester.Response admission = tester.documentName("m05-admettre-patient")
                .variable("entree", Map.of("patientId", patientId, "type", "HOSPITALISATION_COMPLETE",
                        "ufCode", "3001", "motif", "Douleur abdominale"))
                .execute();
        String sejourId = admission.path("admettrePatient.id").entity(String.class).get();
        admission.path("admettrePatient.statut").entity(String.class).isEqualTo("EN_COURS");
        admission.path("admettrePatient.numeroSejour").entity(String.class)
                .satisfies(n -> assertThat(n).startsWith(String.valueOf(LocalDate.now().getYear())).hasSize(10));
        admission.path("admettrePatient.__typename").entity(String.class).isEqualTo("Sejour");
        admission.path("admettrePatient.mouvements[*].type").entityList(String.class).containsExactly("ENTREE");
        admission.path("admettrePatient.patient.nomAffiche").entity(String.class).isEqualTo("ZTEST-PARCOURS");

        // 2. Mutation vers l'USIC
        GraphQlTester.Response mutation = tester.documentName("m08-muter-patient")
                .variable("entree", Map.of("sejourId", sejourId, "ufDestination", "1202")).execute();
        mutation.path("muterPatient.ufCourante.code").entity(String.class).isEqualTo("1202");
        mutation.path("muterPatient.mouvements[*].type").entityList(String.class).containsExactly("ENTREE", "MUTATION");

        // 3. Sortie refusée : pas de lettre de liaison
        tester.documentName("m13-sortir-patient").variable("entree", Map.of("sejourId", sejourId, "modeSortie", "DOMICILE"))
                .execute()
                .path("sortirPatient.code").entity(String.class).isEqualTo("LETTRE_LIAISON_MANQUANTE");

        // 4. Ajout de la lettre de liaison (brouillon)
        GraphQlTester.Response ajout = tester.documentName("m11-ajouter-document")
                .variable("entree", Map.of("sejourId", sejourId, "type", "LETTRE_DE_LIAISON",
                        "titre", "Lettre de liaison", "contenu", "Retour à domicile.", "auteur", "Dr Test"))
                .execute();
        String documentId = ajout.path("ajouterDocument.id").entity(String.class).get();
        ajout.path("ajouterDocument.statut").entity(String.class).isEqualTo("BROUILLON");
        ajout.path("ajouterDocument.taille").entity(Integer.class).isEqualTo("Retour à domicile.".length());
        ajout.path("ajouterDocument.patient.nomAffiche").entity(String.class).isEqualTo("ZTEST-PARCOURS");

        // 5. Toujours refusée : la lettre n'est pas validée
        tester.documentName("m13-sortir-patient").variable("entree", Map.of("sejourId", sejourId, "modeSortie", "DOMICILE"))
                .execute()
                .path("sortirPatient.code").entity(String.class).isEqualTo("LETTRE_LIAISON_MANQUANTE");

        // 6. Validation
        GraphQlTester.Response validation = tester.documentName("m12-valider-document")
                .variable("entree", Map.of("documentId", documentId, "validePar", "Dr Valideur")).execute();
        validation.path("validerDocument.statut").entity(String.class).isEqualTo("VALIDE");
        validation.path("validerDocument.validePar").entity(String.class).isEqualTo("Dr Valideur");
        validation.path("validerDocument.dateValidation").hasValue();

        // 7. Sortie acceptée
        GraphQlTester.Response sortie = tester.documentName("m13-sortir-patient")
                .variable("entree", Map.of("sejourId", sejourId, "modeSortie", "DOMICILE")).execute();
        sortie.path("sortirPatient.statut").entity(String.class).isEqualTo("CLOS");
        sortie.path("sortirPatient.modeSortie").entity(String.class).isEqualTo("DOMICILE");
        sortie.path("sortirPatient.mouvements[*].type").entityList(String.class)
                .containsExactly("ENTREE", "MUTATION", "SORTIE");

        // 8. Le dossier patient reflète le tout
        GraphQlTester.Response dossier = tester.documentName("q20-dossier-patient").variable("id", patientId).execute();
        dossier.path("patient.sejours[0].statut").entity(String.class).isEqualTo("CLOS");
        dossier.path("patient.sejours[0].documents[*].statut").entityList(String.class).containsExactly("VALIDE");

        // 9. Une seconde sortie est refusée
        tester.documentName("m13-sortir-patient").variable("entree", Map.of("sejourId", sejourId, "modeSortie", "DOMICILE"))
                .execute()
                .path("sortirPatient.code").entity(String.class).isEqualTo("SEJOUR_NON_EN_COURS");
    }

    @Test
    void une_consultation_externe_est_possible_pendant_une_hospitalisation() {
        String patientId = creerPatient("Ztest-consultation", "Denis");
        admettre(patientId, "HOSPITALISATION_COMPLETE", "1202");
        String consultation = admettre(patientId, "CONSULTATION_EXTERNE", "6101");
        assertThat(consultation).isNotBlank();

        // ... mais pas une seconde hospitalisation
        tester.documentName("m05-admettre-patient")
                .variable("entree", Map.of("patientId", patientId, "type", "AMBULATOIRE", "ufCode", "5101"))
                .execute()
                .path("admettrePatient.code").entity(String.class).isEqualTo("SEJOUR_DEJA_EN_COURS");
    }

    @Test
    void la_sortie_pour_deces_ne_requiert_pas_de_lettre_de_liaison() {
        String sejourId = admettre(creerPatient("Ztest-deces", "Emile"), "HOSPITALISATION_COMPLETE", "1202");
        tester.documentName("m13-sortir-patient").variable("entree", Map.of("sejourId", sejourId, "modeSortie", "DECES"))
                .execute()
                .path("sortirPatient.statut").entity(String.class).isEqualTo("CLOS");
    }

    @Test
    void m15_un_brouillon_peut_etre_supprime() {
        String sejourId = admettre(creerPatient("Ztest-suppression", "Fanny"), "URGENCES", "3001");
        String documentId = tester.documentName("m11-ajouter-document")
                .variable("entree", Map.of("sejourId", sejourId, "type", "ORDONNANCE", "titre", "Brouillon", "auteur", "Dr Test"))
                .execute()
                .path("ajouterDocument.id").entity(String.class).get();

        tester.documentName("m15-supprimer-document").variable("entree", Map.of("documentId", documentId)).execute()
                .path("supprimerDocument.id").entity(String.class).isEqualTo(documentId);

        // document(...) : requête écrite directement dans le test, sans fichier
        tester.document("query($id: ID!) { document(id: $id) { id } }").variable("id", documentId).execute()
                .path("document").valueIsNull();
    }

    @Test
    void m18_annuler_un_sejour() {
        String sejourId = admettre(creerPatient("Ztest-annulation", "Gilles"), "AMBULATOIRE", "5101");
        tester.documentName("m18-annuler-sejour").variable("entree", Map.of("sejourId", sejourId)).execute()
                .path("annulerSejour.statut").entity(String.class).isEqualTo("ANNULE");
    }

    @Test
    void m19_plusieurs_mutations_s_executent_en_serie() {
        String ippA = nouvelIpp();
        String ippB = nouvelIpp();
        GraphQlTester.Response reponse = tester.documentName("m19-mutations-multiples")
                .variable("ippA", ippA).variable("ippB", ippB).execute();
        reponse.path("premier.ipp").entity(String.class).isEqualTo(ippA);
        reponse.path("second.ipp").entity(String.class).isEqualTo(ippB);
        Long idA = Long.valueOf(reponse.path("premier.id").entity(String.class).get());
        Long idB = Long.valueOf(reponse.path("second.id").entity(String.class).get());
        assertThat(idB).isGreaterThan(idA); // exécutées dans l'ordre
    }
}
