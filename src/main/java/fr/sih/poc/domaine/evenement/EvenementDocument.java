package fr.sih.poc.domaine.evenement;

import fr.sih.poc.domaine.document.Document;

import java.time.LocalDateTime;

public record EvenementDocument(TypeEvenement type, LocalDateTime horodatage, Document document) implements EvenementSih {

    @Override
    public Long patientId() {
        return document.patientId();
    }
}
