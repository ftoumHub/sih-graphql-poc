package fr.sih.poc.domaine.patient;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.commun.ErreurMetier;
import io.vavr.control.Either;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Objects;

/**
 * Agrégat Patient. Objet du domaine pur : aucune annotation Spring / Spring Data.
 * Le mapping vers la table "patient" est fait dans l'adaptateur de persistance.
 */
public record Patient(
        Long id,
        String ipp,
        String nomNaissance,
        String nomUsage,
        String prenom,
        LocalDate dateNaissance,
        Sexe sexe,
        String telephone,
        LocalDateTime dateCreation) {

    public Patient {
        Objects.requireNonNull(ipp, "ipp");
        Objects.requireNonNull(nomNaissance, "nomNaissance");
        Objects.requireNonNull(prenom, "prenom");
        Objects.requireNonNull(dateNaissance, "dateNaissance");
        Objects.requireNonNull(sexe, "sexe");
    }

    public static Either<ErreurMetier, Patient> nouveau(NouveauPatient commande, LocalDateTime maintenant) {
        return ErreurMetier.exiger(!commande.dateNaissance().isAfter(maintenant.toLocalDate()),
                        CodeErreur.DATE_NAISSANCE_FUTURE, "La date de naissance ne peut pas être dans le futur", commande)
                .map(c -> new Patient(null, c.ipp(), c.nomNaissance().toUpperCase(),
                        c.nomUsage() == null ? null : c.nomUsage().toUpperCase(),
                        c.prenom(), c.dateNaissance(), c.sexe(), c.telephone(), maintenant));
    }

    /** Modification partielle : seuls les champs renseignés sont changés. */
    public Patient modifier(ModificationPatient modification) {
        return new Patient(id, ipp,
                modification.nomNaissance() != null ? modification.nomNaissance().toUpperCase() : nomNaissance,
                modification.nomUsage() != null ? modification.nomUsage().toUpperCase() : nomUsage,
                modification.prenom() != null ? modification.prenom() : prenom,
                dateNaissance,
                sexe,
                modification.telephone() != null ? modification.telephone() : telephone,
                dateCreation);
    }

    /** Nom affiché : nom d'usage s'il existe, sinon nom de naissance. */
    public String nomAffiche() {
        return nomUsage != null ? nomUsage : nomNaissance;
    }

    public int age(LocalDate aujourdhui) {
        return Period.between(dateNaissance, aujourdhui).getYears();
    }

    public boolean estMineur(LocalDate aujourdhui) {
        return age(aujourdhui) < 18;
    }
}
