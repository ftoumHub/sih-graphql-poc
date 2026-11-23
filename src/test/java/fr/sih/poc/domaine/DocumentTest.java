package fr.sih.poc.domaine;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.commun.Pagination;
import fr.sih.poc.domaine.document.Document;
import fr.sih.poc.domaine.document.NouveauDocument;
import fr.sih.poc.domaine.document.StatutDocument;
import fr.sih.poc.domaine.document.TypeDocument;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 18, 10, 0);

    private final Document brouillon = Document.brouillon(
            new NouveauDocument(1L, TypeDocument.LETTRE_DE_LIAISON, "Lettre", "Contenu", "Dr A"), 7L, MAINTENANT);

    @Test
    void un_nouveau_document_est_un_brouillon_supprimable() {
        assertThat(brouillon.statut()).isEqualTo(StatutDocument.BROUILLON);
        assertThat(brouillon.patientId()).isEqualTo(7L);
        assertThat(brouillon.taille()).isEqualTo(7);
        assertThat(brouillon.supprimable().isRight()).isTrue();
    }

    @Test
    void un_document_valide_n_est_ni_revalidable_ni_supprimable() {
        Document valide = brouillon.valider("Dr B", MAINTENANT.plusHours(1)).get();
        assertThat(valide.statut()).isEqualTo(StatutDocument.VALIDE);
        assertThat(valide.validePar()).isEqualTo("Dr B");
        assertThat(valide.valider("Dr C", MAINTENANT).getLeft().code()).isEqualTo(CodeErreur.DOCUMENT_DEJA_VALIDE);
        assertThat(valide.supprimable().getLeft().code()).isEqualTo(CodeErreur.DOCUMENT_VALIDE_NON_SUPPRIMABLE);
    }

    @Test
    void la_pagination_est_bornee() {
        assertThat(Pagination.de(null, null).get()).isEqualTo(new Pagination(0, 20));
        assertThat(Pagination.de(0, 500).getLeft().code()).isEqualTo(CodeErreur.PAGINATION_INVALIDE);
        assertThat(Pagination.de(-1, 10).getLeft().code()).isEqualTo(CodeErreur.PAGINATION_INVALIDE);
    }
}
