package fr.sih.poc.api;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requêtes de lecture sur le jeu de démonstration (Liquibase, contexte "demo").
 * Chaque test exécute un fichier de src/test/resources/graphql-test/.
 */
class RequetesLectureTest extends GraphQlTestBase {

    @Nested
    class BasesDuLangage {

        @Test
        void q01_un_patient_par_id_avec_une_variable() {
            GraphQlTester.Response reponse = tester.documentName("q01-patient-par-id").variable("id", 1).execute();
            reponse.path("patient.prenom").entity(String.class).isEqualTo("Jeanne");
            reponse.path("patient.ipp").entity(String.class).isEqualTo("00010001");
            reponse.path("patient.sexe").entity(String.class).isEqualTo("F");
            reponse.path("patient.dateNaissance").entity(String.class).isEqualTo("1948-03-12");
            reponse.path("patient.age").entity(Integer.class).satisfies(age -> assertThat(age).isGreaterThanOrEqualTo(78));
        }

        @Test
        void q02_un_patient_inexistant_vaut_null_sans_erreur() {
            tester.documentName("q02-patient-inexistant").execute()
                    .path("patient").valueIsNull();
        }

        @Test
        void q03_le_nom_affiche_est_le_nom_d_usage_s_il_existe() {
            GraphQlTester.Response reponse = tester.documentName("q03-patient-par-ipp").execute();
            reponse.path("patientParIpp.nomNaissance").entity(String.class).isEqualTo("DUBOIS");
            reponse.path("patientParIpp.nomAffiche").entity(String.class).isEqualTo("LEFEVRE");
        }

        @Test
        void q04_la_syntaxe_raccourcie_fonctionne() {
            tester.documentName("q04-syntaxe-raccourcie").execute()
                    .path("patient.prenom").entity(String.class).isEqualTo("Jeanne");
        }

        @Test
        void q05_les_alias_permettent_de_demander_deux_fois_le_meme_champ() {
            GraphQlTester.Response reponse = tester.documentName("q05-alias").execute();
            reponse.path("jeanne.prenom").entity(String.class).isEqualTo("Jeanne");
            reponse.path("louis.prenom").entity(String.class).isEqualTo("Louis");
        }

        @Test
        void q06_les_fragments_factorisent_la_selection() {
            GraphQlTester.Response reponse = tester.documentName("q06-fragments").execute();
            reponse.path("premier.ipp").entity(String.class).isEqualTo("00010001");
            reponse.path("second.prenom").entity(String.class).isEqualTo("Hugo");
            reponse.path("second.estMineur").entity(Boolean.class).isEqualTo(true);
        }

        @Test
        void q07_la_valeur_par_defaut_de_la_variable_s_applique() {
            tester.documentName("q07-valeur-par-defaut-variable").execute()
                    .path("patients.contenu").entityList(Map.class).hasSize(3);
        }

        @Test
        void q08_la_pagination_renvoie_les_metadonnees() {
            GraphQlTester.Response reponse = tester.documentName("q08-pagination")
                    .variable("page", 1).variable("taille", 5).execute();
            reponse.path("patients.infoPage.page").entity(Integer.class).isEqualTo(1);
            reponse.path("patients.contenu").entityList(Map.class).hasSize(5);
            reponse.path("patients.infoPage.totalElements").entity(Integer.class).satisfies(n -> assertThat(n).isGreaterThanOrEqualTo(12));
            reponse.path("patients.infoPage.totalPages").entity(Integer.class).satisfies(n -> assertThat(n).isGreaterThanOrEqualTo(3));
            reponse.path("patients.infoPage.estDernierePage").entity(Boolean.class).isEqualTo(false);
        }

        @Test
        void q09_le_filtre_nom_cherche_dans_le_nom_de_naissance_et_le_nom_d_usage() {
            tester.documentName("q09-filtre-nom").execute()
                    .path("patients.contenu[*].nomNaissance").entityList(String.class)
                    .containsExactly("BONNET", "MARTIN");
        }

        @Test
        void q10_les_filtres_se_combinent_en_ET() {
            tester.documentName("q10-filtre-combine")
                    .variable("filtre", Map.of("sexe", "F", "neAvant", "1970-01-01"))
                    .execute()
                    .path("patients.contenu[*].nomAffiche").entityList(String.class)
                    .containsExactly("GARCIA", "MARTIN");
        }

        @Test
        void q11_le_scalaire_Date_est_accepte_en_variable() {
            GraphQlTester.Response reponse = tester.documentName("q11-patients-mineurs")
                    .variable("neApres", "2008-11-19").execute();
            reponse.path("patients.contenu[*].prenom").entityList(String.class).containsExactly("Hugo");
            reponse.path("patients.contenu[0].estMineur").entity(Boolean.class).isEqualTo(true);
        }

        @Test
        void q12_les_directives_include_et_skip_pilotent_la_selection() {
            GraphQlTester.Response reponse = tester.documentName("q12-directives")
                    .variable("avecSejours", true).variable("sansTelephone", true).execute();
            reponse.path("patient.telephone").pathDoesNotExist();
            reponse.path("patient.sejours").entityList(Map.class).hasSize(2);

            GraphQlTester.Response inverse = tester.documentName("q12-directives")
                    .variable("avecSejours", false).variable("sansTelephone", false).execute();
            inverse.path("patient.sejours").pathDoesNotExist();
            inverse.path("patient.telephone").entity(String.class).isEqualTo("0612345678");
        }

        @Test
        void q13_typename_donne_le_type_GraphQL() {
            GraphQlTester.Response reponse = tester.documentName("q13-typename").execute();
            reponse.path("patient.__typename").entity(String.class).isEqualTo("Patient");
            reponse.path("patient.sejours[0].__typename").entity(String.class).isEqualTo("Sejour");
        }

        @Test
        void q14_a_q16_l_introspection_decrit_le_schema() {
            tester.documentName("q14-introspection-type").execute()
                    .path("__type.fields[*].name").entityList(String.class)
                    .contains("numeroSejour", "patient", "mouvements", "documents", "dureeJours");

            tester.documentName("q15-introspection-enum").execute()
                    .path("__type.enumValues").entityList(Map.class).hasSize(7);

            GraphQlTester.Response racines = tester.documentName("q16-introspection-racines").execute();
            racines.path("__schema.mutationType.fields[*].name").entityList(String.class)
                    .contains("creerPatient", "admettrePatient", "sortirPatient", "validerDocument");
            racines.path("__schema.subscriptionType.fields[*].name").entityList(String.class)
                    .containsExactly("evenements");
        }

        @Test
        void q17_un_champ_deprecie_reste_disponible_et_documente() {
            GraphQlTester.Response reponse = tester.documentName("q17-champs-deprecies").execute();
            reponse.path("__type.fields[?(@.name == 'ufCode')].isDeprecated").entityList(Boolean.class).containsExactly(true);
            reponse.path("__type.fields[?(@.name == 'uniteFonctionnelle')].isDeprecated").entityList(Boolean.class).containsExactly(false);
        }
    }

    @Nested
    class NavigationDansLeGraphe {

        @Test
        void q20_le_dossier_patient_traverse_sejours_mouvements_et_documents() {
            GraphQlTester.Response reponse = tester.documentName("q20-dossier-patient").variable("id", 1).execute();
            reponse.path("patient.sejours[*].numeroSejour").entityList(String.class)
                    .containsExactly("2026000002", "2026000001");
            reponse.path("patient.sejours[1].mouvements").entityList(Map.class).hasSize(4);
            reponse.path("patient.sejours[1].documents").entityList(Map.class).hasSize(4);
            reponse.path("patient.sejours[1].mouvements[1].uniteFonctionnelle.libelle").entity(String.class)
                    .isEqualTo("USIC (soins intensifs cardiologie)");
            reponse.path("patient.sejours[1].dureeJours").entity(Integer.class).isEqualTo(7);
        }

        @Test
        void q21_on_navigue_du_sejour_vers_le_patient() {
            GraphQlTester.Response reponse = tester.documentName("q21-sejour-vers-patient").execute();
            reponse.path("sejour.patient.prenom").entity(String.class).isEqualTo("Louis");
            reponse.path("sejour.ufCourante.code").entity(String.class).isEqualTo("2101");
            reponse.path("sejour.estEnCours").entity(Boolean.class).isEqualTo(true);
        }

        @Test
        void q22_un_sejour_par_son_numero() {
            GraphQlTester.Response reponse = tester.documentName("q22-sejour-par-numero")
                    .variable("numero", "2026000001").execute();
            reponse.path("sejourParNumero.statut").entity(String.class).isEqualTo("CLOS");
            reponse.path("sejourParNumero.modeSortie").entity(String.class).isEqualTo("DOMICILE");
            reponse.path("sejourParNumero.dureeJours").entity(Integer.class).isEqualTo(7);
        }

        @Test
        void q23_les_mouvements_retracent_le_parcours() {
            GraphQlTester.Response reponse = tester.documentName("q23-parcours-mouvements").execute();
            reponse.path("sejour.mouvements[*].uniteFonctionnelle.code").entityList(String.class)
                    .containsExactly("3001", "1202", "1201", "1201");
            reponse.path("sejour.mouvements[*].type").entityList(String.class)
                    .containsExactly("ENTREE", "MUTATION", "MUTATION", "SORTIE");
        }

        @Test
        @SuppressWarnings("unchecked")
        void q24_sejourEnCours_et_sejours_donnent_des_resultats_coherents() {
            List<Map> patients = tester.documentName("q24-n-plus-1-vs-batch").execute()
                    .path("patients.contenu").entityList(Map.class).get();
            Map<String, Object> martin = patients.stream()
                    .filter(p -> "MARTIN".equals(p.get("nomAffiche"))).findFirst().orElseThrow();
            assertThat((Map<String, Object>) martin.get("sejourEnCours")).containsEntry("numeroSejour", "2026000002");
            Map<String, Object> girard = patients.stream()
                    .filter(p -> "GIRARD".equals(p.get("nomAffiche"))).findFirst().orElseThrow();
            assertThat(girard.get("sejourEnCours")).isNull();
            assertThat((List<?>) girard.get("sejours")).isEmpty();
        }

        @Test
        void q25_un_champ_imbrique_peut_avoir_des_arguments() {
            GraphQlTester.Response reponse = tester.documentName("q25-argument-de-champ").execute();
            reponse.path("patient.lettres").entityList(Map.class).hasSize(1);
            reponse.path("patient.tous").entityList(Map.class).hasSize(6);
        }

        @Test
        void q26_le_contenu_peut_etre_tronque() {
            GraphQlTester.Response reponse = tester.documentName("q26-contenu-tronque").execute();
            String complet = reponse.path("document.complet").entity(String.class).get();
            reponse.path("document.apercu").entity(String.class).isEqualTo(complet.substring(0, 30) + "…");
            reponse.path("document.taille").entity(Integer.class).isEqualTo(complet.length());
        }

        @Test
        void q27_sejours_en_cours_avec_relations_chargees_en_lot() {
            GraphQlTester.Response reponse = tester.documentName("q27-sejours-en-cours").execute();
            reponse.path("sejours.infoPage.totalElements").entity(Integer.class).satisfies(n -> assertThat(n).isGreaterThanOrEqualTo(6));
            reponse.path("sejours.contenu[*].numeroSejour").entityList(String.class)
                    .contains("2026000002", "2026000003", "2026000007", "2026000009", "2026000011", "2026000012");
        }

        @Test
        void q28_filtre_sur_une_periode() {
            tester.documentName("q28-sejours-periode")
                    .variable("filtre", Map.of("entreApres", "2026-11-01T00:00:00", "entreAvant", "2026-12-01T00:00:00",
                            "type", "HOSPITALISATION_COMPLETE"))
                    .execute()
                    .path("sejours.contenu[*].numeroSejour").entityList(String.class)
                    .contains("2026000002", "2026000003", "2026000008", "2026000009", "2026000011");
        }

        @Test
        void q29_documents_en_brouillon_avec_sejour_et_patient() {
            tester.documentName("q29-documents-brouillons").execute()
                    .path("documents.contenu[*].titre").entityList(String.class)
                    .contains("Scanner thoraco-abdomino-pelvien");
        }

        @Test
        void q30_filtre_sur_une_liste_d_enumerations() {
            tester.documentName("q30-documents-multi-criteres").execute()
                    .path("documents.infoPage.totalElements").entity(Integer.class).isEqualTo(3);
        }

        @Test
        void q31_q32_le_referentiel_des_UF() {
            GraphQlTester.Response reponse = tester.documentName("q31-unites-fonctionnelles").execute();
            reponse.path("unitesFonctionnelles").entityList(Map.class).hasSize(7);
            reponse.path("unitesFonctionnelles[6].code").entity(String.class).isEqualTo("6101");
            reponse.path("unitesFonctionnelles[6].tauxOccupation").valueIsNull();

            GraphQlTester.Response uf = tester.documentName("q32-uf-par-code").execute();
            uf.path("uniteFonctionnelle.capacite").entity(Integer.class).isEqualTo(28);
            uf.path("uniteFonctionnelle.sejoursEnCours[*].numeroSejour").entityList(String.class)
                    .contains("2026000009", "2026000011");
        }

        @Test
        void q33_la_recherche_renvoie_des_Noeud_de_types_differents() {
            tester.documentName("q33-recherche-interface").variable("texte", "cardi").execute()
                    .path("recherche[*].__typename").entityList(String.class)
                    .contains("Sejour", "Document")
                    .doesNotContain("Patient");
        }

        @Test
        void q34_recherche_par_ipp() {
            GraphQlTester.Response reponse = tester.documentName("q34-recherche-ipp").execute();
            reponse.path("recherche").entityList(Map.class).hasSize(1);
            reponse.path("recherche[0].nomAffiche").entity(String.class).isEqualTo("ROUX");
        }

        @Test
        void q35_a_q37_statistiques_et_multi_racines() {
            GraphQlTester.Response stats = tester.documentName("q35-statistiques").execute();
            stats.path("statistiques.nombrePatients").entity(Integer.class).satisfies(n -> assertThat(n).isGreaterThanOrEqualTo(12));
            stats.path("statistiques.occupationParUf").entityList(Map.class).hasSize(7);
            stats.path("statistiques.dureeMoyenneSejoursClosJours").hasValue();

            GraphQlTester.Response partielles = tester.documentName("q36-statistiques-partielles").execute();
            partielles.path("statistiques.nombrePatients").hasValue();
            partielles.path("statistiques.occupationParUf").pathDoesNotExist();

            GraphQlTester.Response accueil = tester.documentName("q37-multi-racines").execute();
            accueil.path("urgences.sejoursEnCours[*].numeroSejour").entityList(String.class).contains("2026000007");
            accueil.path("brouillons.contenu").entityList(Map.class).hasSizeLessThan(4);
        }
    }
}
