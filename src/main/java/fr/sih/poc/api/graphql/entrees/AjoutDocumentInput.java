package fr.sih.poc.api.graphql.entrees;

import fr.sih.poc.domaine.document.NouveauDocument;
import fr.sih.poc.domaine.document.TypeDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AjoutDocumentInput(
        Long sejourId,
        TypeDocument type,
        @NotBlank(message = "Le titre est obligatoire") @Size(max = 255) String titre,
        @Size(max = 100_000) String contenu,
        @NotBlank(message = "L'auteur est obligatoire") @Size(max = 100) String auteur) {

    public NouveauDocument versCommande() {
        return new NouveauDocument(sejourId, type, titre, contenu, auteur);
    }
}
