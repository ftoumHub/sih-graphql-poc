package fr.sih.poc.application;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.commun.ErreurMetier;
import fr.sih.poc.domaine.commun.PageResultat;
import fr.sih.poc.domaine.commun.Pagination;
import fr.sih.poc.domaine.document.Document;
import fr.sih.poc.domaine.document.DocumentRepository;
import fr.sih.poc.domaine.document.FiltreDocument;
import fr.sih.poc.domaine.document.NouveauDocument;
import fr.sih.poc.domaine.document.TypeDocument;
import fr.sih.poc.domaine.evenement.EvenementDocument;
import fr.sih.poc.domaine.evenement.PublicateurEvenements;
import fr.sih.poc.domaine.evenement.TypeEvenement;
import fr.sih.poc.domaine.sejour.SejourRepository;
import fr.sih.poc.domaine.sejour.StatutSejour;
import io.vavr.control.Either;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DocumentService {

    private final DocumentRepository documents;
    private final SejourRepository sejours;
    private final PublicateurEvenements evenements;
    private final Transactions transactions;
    private final Clock horloge;

    public DocumentService(DocumentRepository documents, SejourRepository sejours, PublicateurEvenements evenements,
                           Transactions transactions, Clock horloge) {
        this.documents = documents;
        this.sejours = sejours;
        this.evenements = evenements;
        this.transactions = transactions;
        this.horloge = horloge;
    }

    public Either<ErreurMetier, Document> ajouter(NouveauDocument commande) {
        return transactions.executer(() ->
                        ErreurMetier.trouve(sejours.parId(commande.sejourId()), "Sejour", commande.sejourId())
                                .flatMap(sejour -> ErreurMetier.exiger(sejour.statut() != StatutSejour.ANNULE,
                                        CodeErreur.SEJOUR_ANNULE,
                                        "Impossible d'ajouter un document au séjour annulé " + sejour.numeroSejour(),
                                        sejour))
                                .map(sejour -> Document.brouillon(commande, sejour.patientId(), maintenant()))
                                .map(documents::enregistrer))
                .peek(document -> publier(TypeEvenement.DOCUMENT_AJOUTE, document));
    }

    public Either<ErreurMetier, Document> valider(Long id, String validePar) {
        return transactions.executer(() ->
                        obtenir(id)
                                .flatMap(document -> document.valider(validePar, maintenant()))
                                .map(documents::enregistrer))
                .peek(document -> publier(TypeEvenement.DOCUMENT_VALIDE, document));
    }

    public Either<ErreurMetier, Long> supprimer(Long id) {
        return transactions.executer(() ->
                obtenir(id)
                        .flatMap(Document::supprimable)
                        .map(document -> {
                            documents.supprimer(document.id());
                            return document.id();
                        }));
    }

    public Either<ErreurMetier, Document> obtenir(Long id) {
        return ErreurMetier.trouve(documents.parId(id), "Document", id);
    }

    public Optional<Document> trouver(Long id) {
        return documents.parId(id);
    }

    public List<Document> parSejourIds(Collection<Long> sejourIds) {
        return documents.parSejourIds(sejourIds);
    }

    public List<Document> parPatient(Long patientId, TypeDocument type) {
        return documents.parPatient(patientId, type);
    }

    public PageResultat<Document> rechercher(FiltreDocument filtre, Pagination pagination) {
        return documents.rechercher(filtre == null ? FiltreDocument.vide() : filtre, pagination);
    }

    private void publier(TypeEvenement type, Document document) {
        evenements.publier(new EvenementDocument(type, maintenant(), document));
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(horloge);
    }
}
