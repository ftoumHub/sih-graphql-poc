package fr.sih.poc.application;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.commun.ErreurMetier;
import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;
import fr.sih.poc.domaine.patient.FiltrePatient;
import fr.sih.poc.domaine.patient.ModificationPatient;
import fr.sih.poc.domaine.patient.NouveauPatient;
import fr.sih.poc.domaine.patient.Patient;
import fr.sih.poc.domaine.patient.PatientRepository;
import io.vavr.control.Either;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PatientService {

    private final PatientRepository patients;
    private final Transactions transactions;
    private final Clock horloge;

    public PatientService(PatientRepository patients, Transactions transactions, Clock horloge) {
        this.patients = patients;
        this.transactions = transactions;
        this.horloge = horloge;
    }

    public Either<ErreurMetier, Patient> creer(NouveauPatient commande) {
        return transactions.executer(() ->
                ErreurMetier.exiger(!patients.ippExiste(commande.ipp()), CodeErreur.IPP_DEJA_UTILISE,
                                "L'IPP " + commande.ipp() + " est déjà attribué", commande)
                        .flatMap(c -> Patient.nouveau(c, LocalDateTime.now(horloge)))
                        .map(patients::enregistrer));
    }

    public Either<ErreurMetier, Patient> modifier(Long id, ModificationPatient modification) {
        return transactions.executer(() -> obtenir(id)
                .map(patient -> patient.modifier(modification))
                .map(patients::enregistrer));
    }

    public Either<ErreurMetier, Patient> obtenir(Long id) {
        return ErreurMetier.trouve(patients.parId(id), "Patient", id);
    }

    public Optional<Patient> trouver(Long id) {
        return patients.parId(id);
    }

    public Optional<Patient> parIpp(String ipp) {
        return patients.parIpp(ipp);
    }

    public List<Patient> parIds(Collection<Long> ids) {
        return patients.parIds(ids);
    }

    public PageResultat<Patient> rechercher(FiltrePatient filtre, Pagination pagination) {
        return patients.rechercher(filtre == null ? FiltrePatient.vide() : filtre, pagination);
    }
}
