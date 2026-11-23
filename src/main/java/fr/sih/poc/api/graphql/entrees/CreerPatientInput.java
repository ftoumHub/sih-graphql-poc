package fr.sih.poc.api.graphql.entrees;

import fr.sih.poc.domaine.patient.NouveauPatient;
import fr.sih.poc.domaine.patient.Sexe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Input GraphQL "CreerPatientInput".
 * Le FORMAT de l'IPP et du téléphone est garanti par les scalaires IPP et Telephone
 * (rejet dès la validation de la requête). Les autres contraintes sont vérifiées par
 * Bean Validation via ValidateurEntrees (résultat : ErreurMetier VALIDATION).
 */
public record CreerPatientInput(
        String ipp,
        @NotBlank(message = "Le nom de naissance est obligatoire") @Size(max = 100) String nomNaissance,
        @Size(min = 1, max = 100) String nomUsage,
        @NotBlank(message = "Le prénom est obligatoire") @Size(max = 100) String prenom,
        @Past(message = "La date de naissance doit être dans le passé") LocalDate dateNaissance,
        Sexe sexe,
        String telephone) {

    public NouveauPatient versCommande() {
        return new NouveauPatient(ipp, nomNaissance.trim(), nomUsage, prenom.trim(), dateNaissance, sexe, telephone);
    }
}
