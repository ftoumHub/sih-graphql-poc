package fr.sih.poc.domaine.sejour;

import java.time.LocalDateTime;

/**
 * Mouvement d'un patient au sein d'un séjour (entrée, mutation entre UF, sortie).
 * Entité interne à l'agrégat Séjour : elle n'a pas d'identité propre ni de repository.
 */
public record Mouvement(TypeMouvement type, String ufCode, LocalDateTime dateMouvement) {
}
