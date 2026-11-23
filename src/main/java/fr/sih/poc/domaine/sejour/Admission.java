package fr.sih.poc.domaine.sejour;

import java.time.LocalDateTime;

/** Commande d'admission d'un patient. dateEntree null = maintenant. */
public record Admission(Long patientId, TypeSejour type, String ufCode, String motif, LocalDateTime dateEntree) {
}
