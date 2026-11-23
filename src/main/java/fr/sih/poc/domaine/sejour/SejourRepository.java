package fr.sih.poc.domaine.sejour;

import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port secondaire de persistance des séjours. */
public interface SejourRepository {

    Sejour enregistrer(Sejour sejour);

    Optional<Sejour> parId(Long id);

    Optional<Sejour> parNumero(String numeroSejour);

    List<Sejour> parIds(Collection<Long> ids);

    List<Sejour> parPatientIds(Collection<Long> patientIds);

    List<Sejour> enCoursParUf(Collection<String> ufCodes);

    List<Sejour> enCoursDuPatient(Long patientId);

    PageResultat<Sejour> rechercher(FiltreSejour filtre, Pagination pagination);

    List<Sejour> rechercherTexte(String texte, int limite);

    /** Numéro de séjour (NDA) unique, tiré d'une séquence PostgreSQL. */
    String prochainNumeroSejour(int annee);
}
