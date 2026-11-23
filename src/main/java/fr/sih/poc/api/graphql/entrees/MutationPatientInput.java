package fr.sih.poc.api.graphql.entrees;

import java.time.LocalDateTime;

public record MutationPatientInput(Long sejourId, String ufDestination, LocalDateTime dateMutation) {
}
