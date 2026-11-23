package fr.sih.poc.application;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.commun.ErreurMetier;
import fr.sih.poc.domaine.document.Document;
import fr.sih.poc.domaine.document.DocumentRepository;
import fr.sih.poc.domaine.patient.Patient;
import fr.sih.poc.domaine.patient.PatientRepository;
import fr.sih.poc.domaine.sejour.Sejour;
import fr.sih.poc.domaine.sejour.SejourRepository;
import io.vavr.control.Either;

import java.util.List;

/** Recherche transverse : alimente l'union GraphQL ResultatRecherche = Patient | Sejour | Document. */
public class RechercheService {

    public record ResultatsRecherche(List<Patient> patients, List<Sejour> sejours, List<Document> documents) {
    }

    private final PatientRepository patients;
    private final SejourRepository sejours;
    private final DocumentRepository documents;

    public RechercheService(PatientRepository patients, SejourRepository sejours, DocumentRepository documents) {
        this.patients = patients;
        this.sejours = sejours;
        this.documents = documents;
    }

    public Either<ErreurMetier, ResultatsRecherche> rechercher(String texte, int limiteParType) {
        String critere = texte == null ? "" : texte.trim();
        return ErreurMetier.exiger(critere.length() >= 2, CodeErreur.RECHERCHE_TROP_COURTE,
                        "La recherche doit contenir au moins 2 caractères", critere)
                .map(c -> new ResultatsRecherche(
                        patients.rechercherTexte(c, limiteParType),
                        sejours.rechercherTexte(c, limiteParType),
                        documents.rechercherTexte(c, limiteParType)));
    }
}
