package fr.sih.poc.domaine.document;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.commun.ErreurMetier;
import io.vavr.control.Either;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agrégat Document médical, rattaché à un séjour.
 * Cycle de vie : BROUILLON -> VALIDE. Un document validé n'est plus modifiable ni supprimable.
 */
public record Document(
        Long id,
        Long sejourId,
        Long patientId,
        TypeDocument type,
        String titre,
        String contenu,
        String auteur,
        StatutDocument statut,
        LocalDateTime dateCreation,
        LocalDateTime dateValidation,
        String validePar) {

    public Document {
        Objects.requireNonNull(sejourId, "sejourId");
        Objects.requireNonNull(patientId, "patientId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(titre, "titre");
        Objects.requireNonNull(statut, "statut");
    }

    public static Document brouillon(NouveauDocument commande, Long patientId, LocalDateTime maintenant) {
        return new Document(null, commande.sejourId(), patientId, commande.type(), commande.titre(),
                commande.contenu(), commande.auteur(), StatutDocument.BROUILLON, maintenant, null, null);
    }

    public Either<ErreurMetier, Document> valider(String validateur, LocalDateTime maintenant) {
        return ErreurMetier.exiger(statut == StatutDocument.BROUILLON, CodeErreur.DOCUMENT_DEJA_VALIDE,
                        "Le document " + id + " est déjà validé", this)
                .map(d -> new Document(id, sejourId, patientId, type, titre, contenu, auteur,
                        StatutDocument.VALIDE, dateCreation, maintenant, validateur));
    }

    /** Right(this) si le document peut être supprimé (brouillon uniquement). */
    public Either<ErreurMetier, Document> supprimable() {
        return ErreurMetier.exiger(statut == StatutDocument.BROUILLON, CodeErreur.DOCUMENT_VALIDE_NON_SUPPRIMABLE,
                "Un document validé ne peut pas être supprimé (document " + id + ")", this);
    }

    /** Champ calculé exposé en GraphQL (lu directement sur le record). */
    public int taille() {
        return contenu == null ? 0 : contenu.length();
    }
}
