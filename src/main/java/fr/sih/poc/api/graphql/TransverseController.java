package fr.sih.poc.api.graphql;

import fr.sih.poc.application.RechercheService;
import fr.sih.poc.application.ReferentielService;
import fr.sih.poc.application.StatistiquesService;
import fr.sih.poc.domaine.referentiel.UniteFonctionnelle;
import fr.sih.poc.domaine.statistiques.CompteurDocuments;
import fr.sih.poc.domaine.statistiques.OccupationUf;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

/** Recherche (union), référentiel et statistiques (résolution paresseuse champ par champ). */
@Controller
public class TransverseController {

    /** Objet "racine" vide : il ne sert qu'à porter le type GraphQL Statistiques. */
    public record StatistiquesRacine() {
    }

    private final RechercheService rechercheService;
    private final ReferentielService referentielService;
    private final StatistiquesService statistiquesService;

    public TransverseController(RechercheService rechercheService, ReferentielService referentielService,
                                StatistiquesService statistiquesService) {
        this.rechercheService = rechercheService;
        this.referentielService = referentielService;
        this.statistiquesService = statistiquesService;
    }

    // ------------------------------------------------------------- recherche : union

    /**
     * Renvoie une liste hétérogène (Patient, Sejour, Document) typée par l'interface Noeud.
     * Spring for GraphQL détermine le type GraphQL concret de chaque élément à partir du nom
     * simple de sa classe Java : Patient -> Patient, etc. (ClassNameTypeResolver).
     * Un texte trop court donne une erreur GraphQL (Left converti en ErreurMetierException).
     */
    @QueryMapping
    public List<Object> recherche(@Argument String texte, @Argument Integer limiteParType) {
        RechercheService.ResultatsRecherche resultats =
                Resultats.ouErreurGraphQl(rechercheService.rechercher(texte, limiteParType));
        List<Object> tout = new ArrayList<>();
        tout.addAll(resultats.patients());
        tout.addAll(resultats.sejours());
        tout.addAll(resultats.documents());
        return tout;
    }

    // ------------------------------------------------------------- référentiel

    @QueryMapping
    public List<UniteFonctionnelle> unitesFonctionnelles() {
        return referentielService.unitesFonctionnelles();
    }

    @QueryMapping
    public UniteFonctionnelle uniteFonctionnelle(@Argument String code) {
        return referentielService.uniteFonctionnelle(code).orElse(null);
    }

    // ------------------------------------------------------------- statistiques

    @QueryMapping
    public StatistiquesRacine statistiques() {
        return new StatistiquesRacine();
    }

    // Chacune de ces méthodes n'est appelée QUE si le client demande le champ correspondant.

    @SchemaMapping(typeName = "Statistiques")
    public long nombrePatients() {
        return statistiquesService.nombrePatients();
    }

    @SchemaMapping(typeName = "Statistiques")
    public long nombreSejoursEnCours() {
        return statistiquesService.nombreSejoursEnCours();
    }

    @SchemaMapping(typeName = "Statistiques")
    public List<OccupationUf> occupationParUf() {
        return statistiquesService.occupationParUf();
    }

    @SchemaMapping(typeName = "Statistiques")
    public List<CompteurDocuments> documentsParTypeEtStatut() {
        return statistiquesService.documentsParTypeEtStatut();
    }

    @SchemaMapping(typeName = "Statistiques")
    public Double dureeMoyenneSejoursClosJours() {
        return statistiquesService.dureeMoyenneSejoursClosJours();
    }
}
