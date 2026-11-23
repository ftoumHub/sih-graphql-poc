package fr.sih.poc.domaine.evenement;

import java.time.LocalDateTime;

/**
 * Événement métier publié après chaque transition importante.
 * Exposé en GraphQL sous forme d'interface "EvenementSih" implémentée par
 * EvenementSejour et EvenementDocument : Spring for GraphQL résout le type
 * concret grâce au nom simple de la classe Java (ClassNameTypeResolver).
 */
public sealed interface EvenementSih permits EvenementSejour, EvenementDocument {

    TypeEvenement type();

    LocalDateTime horodatage();

    Long patientId();
}
