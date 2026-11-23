package fr.sih.poc.api.graphql;

import fr.sih.poc.api.graphql.entrees.AjoutDocumentInput;
import fr.sih.poc.api.graphql.entrees.SuppressionDocumentInput;
import fr.sih.poc.api.graphql.entrees.ValidationDocumentInput;
import fr.sih.poc.api.graphql.erreurs.ValidateurEntrees;
import fr.sih.poc.application.DocumentService;
import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;
import fr.sih.poc.domaine.document.Document;
import fr.sih.poc.domaine.document.FiltreDocument;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
public class DocumentController {

    /** Succès de la suppression (membre de l'union ResultatSuppression). */
    public record DocumentSupprime(Long id) {
    }

    private final DocumentService documentService;
    private final ValidateurEntrees validateur;

    public DocumentController(DocumentService documentService, ValidateurEntrees validateur) {
        this.documentService = documentService;
        this.validateur = validateur;
    }

    @QueryMapping
    public Document document(@Argument Long id) {
        return documentService.trouver(id).orElse(null);
    }

    @QueryMapping
    public PageResultat<Document> documents(@Argument FiltreDocument filtre, @Argument Integer page, @Argument Integer taille) {
        return Resultats.ouErreurGraphQl(Pagination.de(page, taille)
                .map(pagination -> documentService.rechercher(filtre, pagination)));
    }

    @MutationMapping
    public Object ajouterDocument(@Argument AjoutDocumentInput entree) {
        return Resultats.enUnion(validateur.valider(entree)
                .map(AjoutDocumentInput::versCommande)
                .flatMap(documentService::ajouter));
    }

    @MutationMapping
    public Object validerDocument(@Argument ValidationDocumentInput entree) {
        return Resultats.enUnion(validateur.valider(entree)
                .flatMap(e -> documentService.valider(e.documentId(), e.validePar())));
    }

    @MutationMapping
    public Object supprimerDocument(@Argument SuppressionDocumentInput entree) {
        return Resultats.enUnion(documentService.supprimer(entree.documentId()).map(DocumentSupprime::new));
    }

    /** Remplace la lecture directe de Document.contenu() pour gérer l'argument longueurMax. */
    @SchemaMapping(typeName = "Document", field = "contenu")
    public String contenu(Document document, @Argument Integer longueurMax) {
        String contenu = document.contenu();
        if (contenu == null || longueurMax == null || contenu.length() <= longueurMax) {
            return contenu;
        }
        return contenu.substring(0, Math.max(0, longueurMax)) + "…";
    }
}
