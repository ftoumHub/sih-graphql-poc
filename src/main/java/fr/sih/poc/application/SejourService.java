package fr.sih.poc.application;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.commun.ErreurMetier;
import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;
import fr.sih.poc.domaine.document.DocumentRepository;
import fr.sih.poc.domaine.document.TypeDocument;
import fr.sih.poc.domaine.evenement.EvenementSejour;
import fr.sih.poc.domaine.evenement.PublicateurEvenements;
import fr.sih.poc.domaine.evenement.TypeEvenement;
import fr.sih.poc.domaine.patient.Patient;
import fr.sih.poc.domaine.patient.PatientRepository;
import fr.sih.poc.domaine.referentiel.UniteFonctionnelle;
import fr.sih.poc.domaine.referentiel.UniteFonctionnelleRepository;
import fr.sih.poc.domaine.sejour.Admission;
import fr.sih.poc.domaine.sejour.FiltreSejour;
import fr.sih.poc.domaine.sejour.ModeSortie;
import fr.sih.poc.domaine.sejour.Sejour;
import fr.sih.poc.domaine.sejour.SejourRepository;
import fr.sih.poc.domaine.sejour.TypeSejour;
import io.vavr.control.Either;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Cas d'usage du mouvement patient (admission, mutation, sortie, annulation).
 *
 * Chaque cas d'usage est un pipeline d'Either : la première règle violée court-circuite
 * la suite et devient le résultat. Les événements ne sont publiés (peek) que sur un succès,
 * APRÈS la transaction, pour ne jamais notifier un changement annulé.
 */
public class SejourService {

    /** Séjours dont la sortie exige une lettre de liaison validée (art. R1112-1-2 CSP). */
    private static final Set<TypeSejour> LETTRE_LIAISON_OBLIGATOIRE =
            Set.of(TypeSejour.HOSPITALISATION_COMPLETE, TypeSejour.AMBULATOIRE);

    private final SejourRepository sejours;
    private final PatientRepository patients;
    private final DocumentRepository documents;
    private final UniteFonctionnelleRepository unitesFonctionnelles;
    private final PublicateurEvenements evenements;
    private final Transactions transactions;
    private final Clock horloge;

    public SejourService(SejourRepository sejours, PatientRepository patients, DocumentRepository documents,
                         UniteFonctionnelleRepository unitesFonctionnelles, PublicateurEvenements evenements,
                         Transactions transactions, Clock horloge) {
        this.sejours = sejours;
        this.patients = patients;
        this.documents = documents;
        this.unitesFonctionnelles = unitesFonctionnelles;
        this.evenements = evenements;
        this.transactions = transactions;
        this.horloge = horloge;
    }

    public Either<ErreurMetier, Sejour> admettre(Admission admission) {
        return transactions.executer(() ->
                        patientExistant(admission.patientId())
                                .flatMap(patient -> ufExistante(admission.ufCode()))
                                .flatMap(uf -> pasDejaHospitalise(admission))
                                .flatMap(a -> dateNonFuture(dateOuMaintenant(a.dateEntree())))
                                .map(dateEntree -> Sejour.admettre(
                                        sejours.prochainNumeroSejour(dateEntree.getYear()), admission, dateEntree))
                                .map(sejours::enregistrer))
                .peek(sejour -> publier(TypeEvenement.ADMISSION, sejour));
    }

    public Either<ErreurMetier, Sejour> muter(Long sejourId, String ufDestination, LocalDateTime dateMutation) {
        return transactions.executer(() ->
                        ufExistante(ufDestination)
                                .flatMap(uf -> obtenir(sejourId))
                                .flatMap(sejour -> sejour.muter(ufDestination, dateOuMaintenant(dateMutation)))
                                .map(sejours::enregistrer))
                .peek(sejour -> publier(TypeEvenement.MUTATION, sejour));
    }

    public Either<ErreurMetier, Sejour> sortir(Long sejourId, ModeSortie modeSortie, LocalDateTime dateSortie) {
        return transactions.executer(() ->
                        obtenir(sejourId)
                                .flatMap(sejour -> lettreDeLiaisonSiNecessaire(sejour, modeSortie))
                                .flatMap(sejour -> sejour.sortir(dateOuMaintenant(dateSortie), modeSortie))
                                .map(sejours::enregistrer))
                .peek(sejour -> publier(TypeEvenement.SORTIE, sejour));
    }

    public Either<ErreurMetier, Sejour> annuler(Long sejourId) {
        return transactions.executer(() ->
                        obtenir(sejourId)
                                .flatMap(Sejour::annuler)
                                .map(sejours::enregistrer))
                .peek(sejour -> publier(TypeEvenement.ANNULATION, sejour));
    }

    // ------------------------------------------------------------------ lectures

    public Either<ErreurMetier, Sejour> obtenir(Long id) {
        return ErreurMetier.trouve(sejours.parId(id), "Sejour", id);
    }

    public Optional<Sejour> trouver(Long id) {
        return sejours.parId(id);
    }

    public Optional<Sejour> parNumero(String numero) {
        return sejours.parNumero(numero);
    }

    public List<Sejour> parIds(Collection<Long> ids) {
        return sejours.parIds(ids);
    }

    public List<Sejour> parPatientIds(Collection<Long> patientIds) {
        return sejours.parPatientIds(patientIds);
    }

    public List<Sejour> enCoursDuPatient(Long patientId) {
        return sejours.enCoursDuPatient(patientId);
    }

    public List<Sejour> enCoursParUf(Collection<String> ufCodes) {
        return sejours.enCoursParUf(ufCodes);
    }

    public PageResultat<Sejour> rechercher(FiltreSejour filtre, Pagination pagination) {
        return sejours.rechercher(filtre == null ? FiltreSejour.vide() : filtre, pagination);
    }

    // ------------------------------------------------------------------ règles

    private Either<ErreurMetier, Patient> patientExistant(Long patientId) {
        return ErreurMetier.trouve(patients.parId(patientId), "Patient", patientId);
    }

    private Either<ErreurMetier, UniteFonctionnelle> ufExistante(String ufCode) {
        return ErreurMetier.trouve(unitesFonctionnelles.parCode(ufCode), "UniteFonctionnelle", ufCode);
    }

    private Either<ErreurMetier, Admission> pasDejaHospitalise(Admission admission) {
        boolean dejaHospitalise = admission.type().occupeUnLit()
                && sejours.enCoursDuPatient(admission.patientId()).stream().anyMatch(s -> s.type().occupeUnLit());
        return ErreurMetier.exiger(!dejaHospitalise, CodeErreur.SEJOUR_DEJA_EN_COURS,
                "Le patient " + admission.patientId() + " a déjà un séjour en cours", admission);
    }

    private Either<ErreurMetier, LocalDateTime> dateNonFuture(LocalDateTime date) {
        return ErreurMetier.exiger(!date.isAfter(maintenant()), CodeErreur.DATE_FUTURE,
                "La date d'entrée ne peut pas être dans le futur", date);
    }

    private Either<ErreurMetier, Sejour> lettreDeLiaisonSiNecessaire(Sejour sejour, ModeSortie modeSortie) {
        boolean exigee = LETTRE_LIAISON_OBLIGATOIRE.contains(sejour.type()) && modeSortie != ModeSortie.DECES;
        return ErreurMetier.exiger(
                !exigee || documents.existeDocumentValide(sejour.id(), TypeDocument.LETTRE_DE_LIAISON),
                CodeErreur.LETTRE_LIAISON_MANQUANTE,
                "Une lettre de liaison validée est obligatoire pour la sortie du séjour " + sejour.numeroSejour(),
                sejour);
    }

    private void publier(TypeEvenement type, Sejour sejour) {
        evenements.publier(new EvenementSejour(type, maintenant(), sejour));
    }

    private LocalDateTime dateOuMaintenant(LocalDateTime date) {
        return date != null ? date : maintenant();
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }
}
