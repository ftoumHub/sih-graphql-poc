package fr.sih.poc.domaine.document;

import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port secondaire de persistance des documents. */
public interface DocumentRepository {

    Document enregistrer(Document document);

    Optional<Document> parId(Long id);

    void supprimer(Long id);

    List<Document> parSejourIds(Collection<Long> sejourIds);

    List<Document> parPatient(Long patientId, TypeDocument type);

    boolean existeDocumentValide(Long sejourId, TypeDocument type);

    PageResultat<Document> rechercher(FiltreDocument filtre, Pagination pagination);

    List<Document> rechercherTexte(String texte, int limite);
}
