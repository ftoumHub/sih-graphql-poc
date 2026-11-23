package fr.sih.poc.api.graphql;

import fr.sih.poc.api.graphql.entrees.CreerPatientInput;
import fr.sih.poc.api.graphql.entrees.ModifierPatientInput;
import fr.sih.poc.api.graphql.erreurs.ValidateurEntrees;
import fr.sih.poc.application.DocumentService;
import fr.sih.poc.application.PatientService;
import fr.sih.poc.application.SejourService;
import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;
import fr.sih.poc.domaine.document.Document;
import fr.sih.poc.domaine.document.TypeDocument;
import fr.sih.poc.domaine.patient.FiltrePatient;
import fr.sih.poc.domaine.patient.Patient;
import fr.sih.poc.domaine.sejour.Sejour;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Contrôleur GraphQL des patients.
 *  - @QueryMapping / @MutationMapping : champs des types racines Query / Mutation
 *    (le nom de la méthode = le nom du champ)
 *  - @SchemaMapping(typeName = "Patient") : champs calculés ou relations du type Patient ;
 *    le premier paramètre (Patient) est la "source", c'est-à-dire l'objet parent déjà résolu.
 */
@Controller
public class PatientController {

    private final PatientService patientService;
    private final SejourService sejourService;
    private final DocumentService documentService;
    private final ValidateurEntrees validateur;
    private final Clock horloge;

    public PatientController(PatientService patientService, SejourService sejourService,
                             DocumentService documentService, ValidateurEntrees validateur, Clock horloge) {
        this.validateur = validateur;
        this.patientService = patientService;
        this.sejourService = sejourService;
        this.documentService = documentService;
        this.horloge = horloge;
    }

    // ------------------------------------------------------------------ Query

    @QueryMapping
    public Patient patient(@Argument Long id) {
        return patientService.trouver(id).orElse(null);
    }

    @QueryMapping
    public Patient patientParIpp(@Argument String ipp) {
        return patientService.parIpp(ipp).orElse(null);
    }

    @QueryMapping
    public PageResultat<Patient> patients(@Argument FiltrePatient filtre, @Argument Integer page, @Argument Integer taille) {
        return Resultats.ouErreurGraphQl(Pagination.de(page, taille)
                .map(pagination -> patientService.rechercher(filtre, pagination)));
    }

    // ------------------------------------------------------------------ Mutation

    /** Renvoie un Patient ou une ErreurMetier (union ResultatPatient). */
    @MutationMapping
    public Object creerPatient(@Argument CreerPatientInput entree) {
        return Resultats.enUnion(validateur.valider(entree)
                .map(CreerPatientInput::versCommande)
                .flatMap(patientService::creer));
    }

    @MutationMapping
    public Object modifierPatient(@Argument ModifierPatientInput entree) {
        return Resultats.enUnion(validateur.valider(entree)
                .flatMap(e -> patientService.modifier(e.patientId(), e.versCommande())));
    }

    // ------------------------------------------------------------------ Champs du type Patient

    @SchemaMapping(typeName = "Patient")
    public int age(Patient patient) {
        return patient.age(LocalDate.now(horloge));
    }

    @SchemaMapping(typeName = "Patient")
    public boolean estMineur(Patient patient) {
        return patient.estMineur(LocalDate.now(horloge));
    }

    /**
     * Exemple VOLONTAIREMENT naïf : appelé une fois par patient => N requêtes SQL pour N patients.
     * Activer les logs SQL et comparer avec le champ "sejours" (BatchMapping) : voir tutoriel, chapitre N+1.
     */
    @SchemaMapping(typeName = "Patient")
    public Sejour sejourEnCours(Patient patient) {
        List<Sejour> enCours = sejourService.enCoursDuPatient(patient.id());
        return enCours.stream()
                .max(Comparator.comparing(Sejour::dateEntree))
                .orElse(null);
    }

    /** Champ avec argument : @Argument lit l'argument du CHAMP (et non de la requête racine). */
    @SchemaMapping(typeName = "Patient")
    public List<Document> documents(Patient patient, @Argument TypeDocument type) {
        return documentService.parPatient(patient.id(), type);
    }
}
