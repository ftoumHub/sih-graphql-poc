package fr.sih.poc.api.graphql;

import fr.sih.poc.api.graphql.entrees.AdmissionInput;
import fr.sih.poc.api.graphql.entrees.AnnulationSejourInput;
import fr.sih.poc.api.graphql.entrees.MutationPatientInput;
import fr.sih.poc.api.graphql.entrees.SortieInput;
import fr.sih.poc.api.graphql.erreurs.ValidateurEntrees;
import fr.sih.poc.application.SejourService;
import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;
import fr.sih.poc.domaine.sejour.FiltreSejour;
import fr.sih.poc.domaine.sejour.Sejour;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Les mutations renvoient Object : soit le Sejour, soit l'ErreurMetier (union ResultatSejour).
 * Chaque mutation prend un unique argument "entree" (input dédié à l'action).
 */
@Controller
public class SejourController {

    private final SejourService sejourService;
    private final ValidateurEntrees validateur;
    private final Clock horloge;

    public SejourController(SejourService sejourService, ValidateurEntrees validateur, Clock horloge) {
        this.sejourService = sejourService;
        this.validateur = validateur;
        this.horloge = horloge;
    }

    @QueryMapping
    public Sejour sejour(@Argument Long id) {
        return sejourService.trouver(id).orElse(null);
    }

    @QueryMapping
    public Sejour sejourParNumero(@Argument String numero) {
        return sejourService.parNumero(numero).orElse(null);
    }

    @QueryMapping
    public PageResultat<Sejour> sejours(@Argument FiltreSejour filtre, @Argument Integer page, @Argument Integer taille) {
        return Resultats.ouErreurGraphQl(Pagination.de(page, taille)
                .map(pagination -> sejourService.rechercher(filtre, pagination)));
    }

    @MutationMapping
    public Object admettrePatient(@Argument AdmissionInput entree) {
        return Resultats.enUnion(validateur.valider(entree)
                .map(AdmissionInput::versCommande)
                .flatMap(sejourService::admettre));
    }

    @MutationMapping
    public Object muterPatient(@Argument MutationPatientInput entree) {
        return Resultats.enUnion(sejourService.muter(entree.sejourId(), entree.ufDestination(), entree.dateMutation()));
    }

    @MutationMapping
    public Object sortirPatient(@Argument SortieInput entree) {
        return Resultats.enUnion(sejourService.sortir(entree.sejourId(), entree.modeSortie(), entree.dateSortie()));
    }

    @MutationMapping
    public Object annulerSejour(@Argument AnnulationSejourInput entree) {
        return Resultats.enUnion(sejourService.annuler(entree.sejourId()));
    }

    @SchemaMapping(typeName = "Sejour")
    public long dureeJours(Sejour sejour) {
        return sejour.dureeJours(LocalDateTime.now(horloge));
    }
}
