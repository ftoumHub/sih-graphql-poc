package fr.sih.poc.domaine.sejour;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.commun.ErreurMetier;
import io.vavr.control.Either;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Agrégat Séjour : une venue du patient dans l'établissement.
 * Il porte la liste ordonnée de ses mouvements et garantit leurs invariants
 * (chronologie, statut). Immuable : chaque transition renvoie un nouveau Sejour,
 * ou une ErreurMetier si la transition est interdite (Either de Vavr).
 */
public record Sejour(
        Long id,
        String numeroSejour,
        Long patientId,
        TypeSejour type,
        StatutSejour statut,
        String ufCourante,
        String motif,
        LocalDateTime dateEntree,
        LocalDateTime dateSortie,
        ModeSortie modeSortie,
        List<Mouvement> mouvements) {

    public Sejour {
        Objects.requireNonNull(numeroSejour, "numeroSejour");
        Objects.requireNonNull(patientId, "patientId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(statut, "statut");
        Objects.requireNonNull(dateEntree, "dateEntree");
        mouvements = List.copyOf(mouvements);
    }

    public static Sejour admettre(String numeroSejour, Admission admission, LocalDateTime dateEntree) {
        return new Sejour(null, numeroSejour, admission.patientId(), admission.type(), StatutSejour.EN_COURS,
                admission.ufCode(), admission.motif(), dateEntree, null, null,
                List.of(new Mouvement(TypeMouvement.ENTREE, admission.ufCode(), dateEntree)));
    }

    public Either<ErreurMetier, Sejour> muter(String ufDestination, LocalDateTime dateMutation) {
        return verifierEnCours("muter")
                .flatMap(s -> ErreurMetier.exiger(!ufDestination.equals(ufCourante), CodeErreur.MUTATION_MEME_UF,
                        "Le patient est déjà dans l'UF " + ufDestination, s))
                .flatMap(s -> s.verifierChronologie(dateMutation))
                .map(s -> new Sejour(id, numeroSejour, patientId, type, statut, ufDestination, motif, dateEntree,
                        null, null, avec(new Mouvement(TypeMouvement.MUTATION, ufDestination, dateMutation))));
    }

    public Either<ErreurMetier, Sejour> sortir(LocalDateTime dateSortie, ModeSortie mode) {
        return verifierEnCours("clore")
                .flatMap(s -> s.verifierChronologie(dateSortie))
                .map(s -> new Sejour(id, numeroSejour, patientId, type, StatutSejour.CLOS, ufCourante, motif,
                        dateEntree, dateSortie, mode, avec(new Mouvement(TypeMouvement.SORTIE, ufCourante, dateSortie))));
    }

    public Either<ErreurMetier, Sejour> annuler() {
        return verifierEnCours("annuler")
                .map(s -> new Sejour(id, numeroSejour, patientId, type, StatutSejour.ANNULE, ufCourante, motif,
                        dateEntree, null, null, mouvements));
    }

    public boolean estEnCours() {
        return statut == StatutSejour.EN_COURS;
    }

    /** Durée en jours (calendaires) ; pour un séjour en cours, calculée jusqu'à "maintenant". */
    public long dureeJours(LocalDateTime maintenant) {
        LocalDateTime fin = dateSortie != null ? dateSortie : maintenant;
        return Duration.between(dateEntree.toLocalDate().atStartOfDay(), fin.toLocalDate().atStartOfDay()).toDays();
    }

    private Either<ErreurMetier, Sejour> verifierEnCours(String action) {
        return ErreurMetier.exiger(estEnCours(), CodeErreur.SEJOUR_NON_EN_COURS,
                "Impossible de " + action + " le séjour " + numeroSejour + " : statut " + statut, this);
    }

    private Either<ErreurMetier, Sejour> verifierChronologie(LocalDateTime date) {
        LocalDateTime dernier = mouvements.isEmpty() ? dateEntree : mouvements.get(mouvements.size() - 1).dateMouvement();
        return ErreurMetier.exiger(!date.isBefore(dernier), CodeErreur.CHRONOLOGIE_INVALIDE,
                "La date " + date + " est antérieure au dernier mouvement (" + dernier + ")", this);
    }

    private List<Mouvement> avec(Mouvement mouvement) {
        List<Mouvement> copie = new ArrayList<>(mouvements);
        copie.add(mouvement);
        return copie;
    }
}
