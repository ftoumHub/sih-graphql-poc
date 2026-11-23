package fr.sih.poc.api;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les familles d'erreurs :
 *  1. erreurs de VALIDATION du document (champ inconnu, scalaire mal formé, variable manquante)
 *     et de PROTECTION (profondeur, coût) : rejet avant exécution, dans "errors" ;
 *  2. erreurs de requête côté QUERY (pagination, recherche) : "errors" + extensions.code ;
 *  3. erreurs MÉTIER des MUTATIONS : renvoyées comme DONNÉES (union ... | ErreurMetier), sans "errors".
 */
class ErreursTest extends GraphQlTestBase {

    // =================================================================== 1. validation et protection

    @Test
    void e02_un_champ_inconnu_est_rejete_a_la_validation() {
        tester.documentName("e02-champ-inconnu").execute()
                .errors().satisfy(erreurs -> {
                    assertThat(erreurs).isNotEmpty();
                    assertThat(erreurs.get(0).getMessage()).contains("numeroSecu");
                    assertThat(erreurs.get(0).getErrorType().toString()).isEqualTo("ValidationError");
                });
    }

    @Test
    void e04_un_scalaire_Date_mal_forme_est_refuse() {
        tester.documentName("e04-scalaire-invalide").execute()
                .errors().satisfy(erreurs -> assertThat(erreurs).isNotEmpty()
                        .anySatisfy(e -> assertThat(e.getMessage()).contains("12/03/1948")));
    }

    @Test
    void e08_un_scalaire_IPP_mal_forme_est_refuse() {
        tester.documentName("e08-scalaire-ipp-invalide").execute()
                .errors().satisfy(erreurs -> assertThat(erreurs).isNotEmpty()
                        .anySatisfy(e -> assertThat(e.getMessage()).contains("12AB")));
    }

    @Test
    void e05_une_requete_trop_profonde_est_bloquee() {
        // Elle est à la fois trop profonde ET trop coûteuse : l'une ou l'autre protection l'arrête.
        tester.documentName("e05-profondeur-excessive").execute()
                .errors().satisfy(erreurs -> assertThat(erreurs).isNotEmpty()
                        .anySatisfy(e -> assertThat(e.getMessage()).containsAnyOf("depth", "complexity")));
    }

    @Test
    void e07_une_requete_trop_couteuse_est_bloquee() {
        tester.documentName("e07-cout-excessif").execute()
                .errors().satisfy(erreurs -> assertThat(erreurs).isNotEmpty()
                        .anySatisfy(e -> assertThat(e.getMessage()).containsIgnoringCase("complexity")));
    }

    @Test
    void e06_une_variable_obligatoire_manquante_est_refusee() {
        tester.documentName("e06-variable-manquante").execute()
                .errors().satisfy(erreurs -> assertThat(erreurs).isNotEmpty());
    }

    // =================================================================== 2. erreurs de query

    @Test
    void e01_une_erreur_sur_un_champ_donne_une_reponse_partielle() {
        GraphQlTester.Response reponse = tester.documentName("e01-reponse-partielle").execute();
        reponse.errors().satisfy(erreurs -> {
            assertThat(erreurs).hasSize(1);
            assertThat(erreurs.get(0).getPath()).isEqualTo("recherche");
            assertThat(erreurs.get(0).getExtensions()).containsEntry("code", "RECHERCHE_TROP_COURTE");
        });
        reponse.path("patient.prenom").entity(String.class).isEqualTo("Jeanne");
        reponse.path("recherche").valueIsNull();
    }

    @Test
    void e03_une_taille_de_page_trop_grande_est_refusee() {
        tester.documentName("e03-pagination-invalide").execute()
                .errors().satisfy(erreurs -> {
                    assertThat(erreurs).hasSize(1);
                    assertThat(erreurs.get(0).getErrorType().toString()).isEqualTo("BAD_REQUEST");
                    assertThat(erreurs.get(0).getExtensions()).containsEntry("code", "PAGINATION_INVALIDE");
                });
    }

    // =================================================================== 3. erreurs métier = données

    @Test
    void m02_bean_validation_renvoie_un_detail_par_champ() {
        GraphQlTester.Response reponse = tester.documentName("m02-creer-patient-invalide").execute();
        reponse.path("creerPatient.__typename").entity(String.class).isEqualTo("ErreurMetier");
        reponse.path("creerPatient.code").entity(String.class).isEqualTo("VALIDATION");
        reponse.path("creerPatient.details[*].champ").entityList(String.class)
                .containsExactly("dateNaissance", "nomNaissance", "prenom");
    }

    @Test
    void m03_un_ipp_deja_attribue_est_refuse() {
        erreurMetier("m03-creer-patient-ipp-existant", Map.of(), "creerPatient", "IPP_DEJA_UTILISE");
    }

    @Test
    void m06_un_patient_deja_hospitalise_ne_peut_pas_etre_admis() {
        erreurMetier("m06-admettre-deja-hospitalise", Map.of(), "admettrePatient", "SEJOUR_DEJA_EN_COURS");
    }

    @Test
    void m07_une_UF_inconnue_donne_RESSOURCE_INTROUVABLE_avec_le_detail() {
        GraphQlTester.Response reponse = erreurMetier("m07-admettre-uf-inconnue", Map.of("patientId", 10),
                "admettrePatient", "RESSOURCE_INTROUVABLE");
        reponse.path("admettrePatient.details[0].champ").entity(String.class).isEqualTo("UniteFonctionnelle");
        reponse.path("admettrePatient.details[0].message").entity(String.class).isEqualTo("9999");
    }

    @Test
    void m09_muter_vers_l_UF_courante_est_refuse() {
        erreurMetier("m09-muter-meme-uf", Map.of(), "muterPatient", "MUTATION_MEME_UF");
    }

    @Test
    void m10_la_sortie_exige_une_lettre_de_liaison_validee() {
        erreurMetier("m10-sortir-sans-lettre", Map.of("sejourId", 12), "sortirPatient", "LETTRE_LIAISON_MANQUANTE");
        // ... et rien n'a été modifié : le séjour est toujours en cours
        tester.documentName("q22-sejour-par-numero").variable("numero", "2026000012").execute()
                .path("sejourParNumero.statut").entity(String.class).isEqualTo("EN_COURS");
    }

    @Test
    void m14_on_ne_sort_pas_d_un_sejour_deja_clos() {
        erreurMetier("m14-sortir-sejour-clos", Map.of(), "sortirPatient", "SEJOUR_NON_EN_COURS");
    }

    @Test
    void m16_un_document_valide_n_est_pas_supprimable() {
        erreurMetier("m16-supprimer-document-valide", Map.of(), "supprimerDocument", "DOCUMENT_VALIDE_NON_SUPPRIMABLE");
    }

    @Test
    void m17_pas_de_document_sur_un_sejour_annule() {
        erreurMetier("m17-document-sejour-annule", Map.of(), "ajouterDocument", "SEJOUR_ANNULE");
    }

    @Test
    void m20_un_mouvement_ne_peut_pas_preceder_le_precedent() {
        erreurMetier("m20-muter-date-anterieure", Map.of("sejourId", 2), "muterPatient", "CHRONOLOGIE_INVALIDE");
    }

    @Test
    void m21_sans_fragment_sur_ErreurMetier_l_erreur_passe_inapercue() {
        // Piège classique de l'approche "erreurs comme données" : le client DOIT sélectionner ErreurMetier.
        GraphQlTester.Response reponse = tester.documentName("m21-erreur-sans-fragment").execute();
        reponse.path("sortirPatient").entity(Map.class).satisfies(m -> assertThat(m).isEmpty());
    }

    /** Vérifie qu'une mutation renvoie une ErreurMetier du code attendu, SANS erreur GraphQL. */
    private GraphQlTester.Response erreurMetier(String document, Map<String, Object> variables, String champ, String code) {
        GraphQlTester.Request<?> requete = tester.documentName(document);
        variables.forEach(requete::variable);
        GraphQlTester.Response reponse = requete.execute();
        reponse.errors().satisfy(aucuneErreur());
        reponse.path(champ + ".__typename").entity(String.class).isEqualTo("ErreurMetier");
        reponse.path(champ + ".code").entity(String.class).isEqualTo(code);
        reponse.path(champ + ".message").entity(String.class).satisfies(m -> assertThat(m).isNotBlank());
        return reponse;
    }

    private static Consumer<List<ResponseError>> aucuneErreur() {
        return erreurs -> assertThat(erreurs).isEmpty();
    }
}
