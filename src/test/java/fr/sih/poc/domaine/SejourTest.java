package fr.sih.poc.domaine;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.sejour.Admission;
import fr.sih.poc.domaine.sejour.ModeSortie;
import fr.sih.poc.domaine.sejour.Sejour;
import fr.sih.poc.domaine.sejour.StatutSejour;
import fr.sih.poc.domaine.sejour.TypeMouvement;
import fr.sih.poc.domaine.sejour.TypeSejour;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le domaine se teste sans Spring, sans base, sans GraphQL.
 * Les transitions renvoient un Either : on vérifie Right(valeur) ou Left(erreur).getCode().
 */
class SejourTest {

    private static final LocalDateTime ENTREE = LocalDateTime.of(2026, 11, 10, 8, 0);

    private final Sejour sejour = Sejour.admettre("2026001000",
            new Admission(1L, TypeSejour.HOSPITALISATION_COMPLETE, "3001", "Test", null), ENTREE);

    @Test
    void une_admission_cree_un_sejour_en_cours_avec_un_mouvement_d_entree() {
        assertThat(sejour.statut()).isEqualTo(StatutSejour.EN_COURS);
        assertThat(sejour.ufCourante()).isEqualTo("3001");
        assertThat(sejour.mouvements()).extracting("type").containsExactly(TypeMouvement.ENTREE);
    }

    @Test
    void une_mutation_change_l_uf_courante_et_ajoute_un_mouvement() {
        Sejour mute = sejour.muter("1202", ENTREE.plusHours(4)).get();
        assertThat(mute.ufCourante()).isEqualTo("1202");
        assertThat(mute.mouvements()).extracting("type").containsExactly(TypeMouvement.ENTREE, TypeMouvement.MUTATION);
        assertThat(sejour.mouvements()).hasSize(1); // immuable : l'original n'a pas changé
    }

    @Test
    void une_mutation_vers_l_uf_courante_est_refusee() {
        assertThat(sejour.muter("3001", ENTREE.plusHours(1)).getLeft().code()).isEqualTo(CodeErreur.MUTATION_MEME_UF);
    }

    @Test
    void une_mutation_ne_peut_pas_preceder_le_dernier_mouvement() {
        var resultat = sejour.muter("1202", ENTREE.minusMinutes(1));
        assertThat(resultat.isLeft()).isTrue();
        assertThat(resultat.getLeft().code()).isEqualTo(CodeErreur.CHRONOLOGIE_INVALIDE);
    }

    @Test
    void la_sortie_clot_le_sejour_et_fige_la_duree() {
        Sejour clos = sejour.sortir(ENTREE.plusDays(3), ModeSortie.DOMICILE).get();
        assertThat(clos.statut()).isEqualTo(StatutSejour.CLOS);
        assertThat(clos.dureeJours(ENTREE.plusDays(30))).isEqualTo(3);
        assertThat(clos.mouvements()).last().extracting("type").isEqualTo(TypeMouvement.SORTIE);
    }

    @Test
    void un_sejour_clos_ne_peut_plus_etre_mute_ni_annule() {
        Sejour clos = sejour.sortir(ENTREE.plusDays(1), ModeSortie.DOMICILE).get();
        assertThat(clos.muter("1202", ENTREE.plusDays(2)).getLeft().code()).isEqualTo(CodeErreur.SEJOUR_NON_EN_COURS);
        assertThat(clos.annuler().getLeft().code()).isEqualTo(CodeErreur.SEJOUR_NON_EN_COURS);
    }

    @Test
    void la_premiere_regle_violee_court_circuite_les_suivantes() {
        // Séjour clos ET même UF ET date antérieure : seule la première erreur (statut) est renvoyée.
        Sejour clos = sejour.sortir(ENTREE.plusDays(1), ModeSortie.DOMICILE).get();
        assertThat(clos.muter("3001", ENTREE.minusDays(1)).getLeft().code()).isEqualTo(CodeErreur.SEJOUR_NON_EN_COURS);
    }

    @Test
    void une_consultation_n_occupe_pas_de_lit() {
        assertThat(TypeSejour.CONSULTATION_EXTERNE.occupeUnLit()).isFalse();
        assertThat(TypeSejour.AMBULATOIRE.occupeUnLit()).isTrue();
    }
}
