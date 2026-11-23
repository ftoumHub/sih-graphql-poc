package fr.sih.poc.infrastructure.persistance;

import fr.sih.poc.domaine.patient.Patient;
import fr.sih.poc.domaine.patient.Sexe;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Modèle de persistance de l'agrégat Patient (table "patient").
 * Les colonnes sont déduites des noms de propriétés : nomNaissance -> nom_naissance.
 * Les enums sont stockés en String et convertis dans le mapper : le domaine ne dépend pas du stockage.
 */
@Table("patient")
public record PatientEntity(
        @Id Long id,
        String ipp,
        String nomNaissance,
        String nomUsage,
        String prenom,
        LocalDate dateNaissance,
        String sexe,
        String telephone,
        LocalDateTime dateCreation) {

    /** "Wither" utilisé par Spring Data JDBC pour renseigner l'id généré par la base sur un record immuable. */
    public PatientEntity withId(Long nouvelId) {
        return new PatientEntity(nouvelId, ipp, nomNaissance, nomUsage, prenom, dateNaissance, sexe, telephone, dateCreation);
    }

    static PatientEntity depuis(Patient p) {
        return new PatientEntity(p.id(), p.ipp(), p.nomNaissance(), p.nomUsage(), p.prenom(), p.dateNaissance(),
                p.sexe().name(), p.telephone(), p.dateCreation());
    }

    Patient versDomaine() {
        return new Patient(id, ipp, nomNaissance, nomUsage, prenom, dateNaissance, Sexe.valueOf(sexe), telephone, dateCreation);
    }
}
