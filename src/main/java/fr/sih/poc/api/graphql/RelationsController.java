package fr.sih.poc.api.graphql;

import fr.sih.poc.application.DocumentService;
import fr.sih.poc.application.PatientService;
import fr.sih.poc.application.ReferentielService;
import fr.sih.poc.application.SejourService;
import fr.sih.poc.domaine.document.Document;
import fr.sih.poc.domaine.patient.Patient;
import fr.sih.poc.domaine.referentiel.UniteFonctionnelle;
import fr.sih.poc.domaine.sejour.Mouvement;
import fr.sih.poc.domaine.sejour.Sejour;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.stereotype.Controller;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Résolution des relations ENTRE agrégats, en lot (@BatchMapping).
 *
 * Principe : graphql-java collecte toutes les sources d'un même niveau de la requête
 * (ex. les 20 séjours d'une page), puis appelle UNE fois la méthode avec la liste complète.
 * On renvoie une Map source -> valeur. Résultat : 1 requête SQL "WHERE id IN (...)"
 * au lieu de 20 "WHERE id = ?". Sous le capot : un DataLoader enregistré automatiquement.
 *
 * Attention : chaque source DOIT avoir une entrée dans la Map, sinon le champ vaut null
 * (ce qui casse les champs non-nullables comme [Sejour!]!).
 */
@Controller
public class RelationsController {

    private final PatientService patientService;
    private final SejourService sejourService;
    private final DocumentService documentService;
    private final ReferentielService referentielService;

    public RelationsController(PatientService patientService, SejourService sejourService,
                               DocumentService documentService, ReferentielService referentielService) {
        this.patientService = patientService;
        this.sejourService = sejourService;
        this.documentService = documentService;
        this.referentielService = referentielService;
    }

    // ------------------------------------------------------------- relations "N -> 1"

    @BatchMapping(typeName = "Sejour", field = "patient")
    public Map<Sejour, Patient> patientDuSejour(List<Sejour> sejours) {
        Map<Long, Patient> patients = indexer(patientService.parIds(ids(sejours, Sejour::patientId)), Patient::id);
        return associer(sejours, s -> patients.get(s.patientId()));
    }

    @BatchMapping(typeName = "Document", field = "patient")
    public Map<Document, Patient> patientDuDocument(List<Document> documents) {
        Map<Long, Patient> patients = indexer(patientService.parIds(ids(documents, Document::patientId)), Patient::id);
        return associer(documents, d -> patients.get(d.patientId()));
    }

    @BatchMapping(typeName = "Document", field = "sejour")
    public Map<Document, Sejour> sejourDuDocument(List<Document> documents) {
        Map<Long, Sejour> sejours = indexer(sejourService.parIds(ids(documents, Document::sejourId)), Sejour::id);
        return associer(documents, d -> sejours.get(d.sejourId()));
    }

    @BatchMapping(typeName = "Sejour", field = "ufCourante")
    public Map<Sejour, UniteFonctionnelle> ufDuSejour(List<Sejour> sejours) {
        Map<String, UniteFonctionnelle> ufs = indexer(
                referentielService.parCodes(ids(sejours, Sejour::ufCourante)), UniteFonctionnelle::code);
        return associer(sejours, s -> ufs.get(s.ufCourante()));
    }

    @BatchMapping(typeName = "Mouvement", field = "uniteFonctionnelle")
    public Map<Mouvement, UniteFonctionnelle> ufDuMouvement(List<Mouvement> mouvements) {
        Map<String, UniteFonctionnelle> ufs = indexer(
                referentielService.parCodes(ids(mouvements, Mouvement::ufCode)), UniteFonctionnelle::code);
        return associer(mouvements, m -> ufs.get(m.ufCode()));
    }

    // ------------------------------------------------------------- relations "1 -> N"

    @BatchMapping(typeName = "Patient", field = "sejours")
    public Map<Patient, List<Sejour>> sejoursDuPatient(List<Patient> patients) {
        Map<Long, List<Sejour>> parPatient = sejourService.parPatientIds(ids(patients, Patient::id)).stream()
                .collect(Collectors.groupingBy(Sejour::patientId));
        return associer(patients, p -> parPatient.getOrDefault(p.id(), List.of()));
    }

    @BatchMapping(typeName = "Sejour", field = "documents")
    public Map<Sejour, List<Document>> documentsDuSejour(List<Sejour> sejours) {
        Map<Long, List<Document>> parSejour = documentService.parSejourIds(ids(sejours, Sejour::id)).stream()
                .collect(Collectors.groupingBy(Document::sejourId));
        return associer(sejours, s -> parSejour.getOrDefault(s.id(), List.of()));
    }

    @BatchMapping(typeName = "UniteFonctionnelle", field = "sejoursEnCours")
    public Map<UniteFonctionnelle, List<Sejour>> sejoursEnCoursDeLUf(List<UniteFonctionnelle> ufs) {
        Map<String, List<Sejour>> parUf = enCoursParUf(ufs);
        return associer(ufs, uf -> parUf.getOrDefault(uf.code(), List.of()));
    }

    @BatchMapping(typeName = "UniteFonctionnelle", field = "tauxOccupation")
    public Map<UniteFonctionnelle, Double> tauxOccupation(List<UniteFonctionnelle> ufs) {
        Map<String, List<Sejour>> parUf = enCoursParUf(ufs);
        Map<UniteFonctionnelle, Double> resultat = new LinkedHashMap<>();
        for (UniteFonctionnelle uf : ufs) {
            int presents = parUf.getOrDefault(uf.code(), List.of()).size();
            resultat.put(uf, uf.capacite() == 0 ? null : Math.round(1000.0 * presents / uf.capacite()) / 10.0);
        }
        return resultat;
    }

    // ------------------------------------------------------------- utilitaires

    private Map<String, List<Sejour>> enCoursParUf(List<UniteFonctionnelle> ufs) {
        return sejourService.enCoursParUf(ids(ufs, UniteFonctionnelle::code)).stream()
                .collect(Collectors.groupingBy(Sejour::ufCourante));
    }

    private static <S, K> List<K> ids(Collection<S> sources, Function<S, K> cle) {
        return sources.stream().map(cle).distinct().toList();
    }

    private static <K, V> Map<K, V> indexer(Collection<V> valeurs, Function<V, K> cle) {
        return valeurs.stream().collect(Collectors.toMap(cle, Function.identity()));
    }

    /** LinkedHashMap : accepte les valeurs null (contrairement à Collectors.toMap). */
    private static <S, V> Map<S, V> associer(List<S> sources, Function<S, V> valeur) {
        Map<S, V> resultat = new LinkedHashMap<>();
        sources.forEach(s -> resultat.put(s, valeur.apply(s)));
        return resultat;
    }
}
