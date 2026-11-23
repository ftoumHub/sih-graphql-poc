package fr.sih.poc.api.graphql.entrees;

import fr.sih.poc.domaine.sejour.ModeSortie;

import java.time.LocalDateTime;

public record SortieInput(Long sejourId, ModeSortie modeSortie, LocalDateTime dateSortie) {
}
