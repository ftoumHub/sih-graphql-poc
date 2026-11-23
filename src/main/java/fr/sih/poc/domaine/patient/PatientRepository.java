package fr.sih.poc.domaine.patient;

import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port secondaire de persistance des patients (implémenté par l'adaptateur Spring Data JDBC). */
public interface PatientRepository {

    Patient enregistrer(Patient patient);

    Optional<Patient> parId(Long id);

    Optional<Patient> parIpp(String ipp);

    boolean ippExiste(String ipp);

    /** Chargement groupé : utilisé par les @BatchMapping pour éviter le N+1. */
    List<Patient> parIds(Collection<Long> ids);

    PageResultat<Patient> rechercher(FiltrePatient filtre, Pagination pagination);

    /** Recherche plein texte simplifiée (nom, prénom, IPP). */
    List<Patient> rechercherTexte(String texte, int limite);
}
