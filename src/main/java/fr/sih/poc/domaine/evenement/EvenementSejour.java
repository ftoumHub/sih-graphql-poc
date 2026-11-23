package fr.sih.poc.domaine.evenement;

import fr.sih.poc.domaine.sejour.Sejour;

import java.time.LocalDateTime;

public record EvenementSejour(TypeEvenement type, LocalDateTime horodatage, Sejour sejour) implements EvenementSih {

    @Override
    public Long patientId() {
        return sejour.patientId();
    }
}
