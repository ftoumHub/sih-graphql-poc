package fr.sih.poc.api.graphql.entrees;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidationDocumentInput(
        Long documentId,
        @NotBlank(message = "Le validateur est obligatoire") @Size(max = 100) String validePar) {
}
