package fr.sih.poc.infrastructure.persistance;

import fr.sih.poc.domaine.document.Document;
import fr.sih.poc.domaine.document.StatutDocument;
import fr.sih.poc.domaine.document.TypeDocument;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("document")
public record DocumentEntity(
        @Id Long id,
        Long sejourId,
        Long patientId,
        String typeDocument,
        String titre,
        String contenu,
        String auteur,
        String statut,
        LocalDateTime dateCreation,
        LocalDateTime dateValidation,
        String validePar) {

    public DocumentEntity withId(Long nouvelId) {
        return new DocumentEntity(nouvelId, sejourId, patientId, typeDocument, titre, contenu, auteur, statut,
                dateCreation, dateValidation, validePar);
    }

    static DocumentEntity depuis(Document d) {
        return new DocumentEntity(d.id(), d.sejourId(), d.patientId(), d.type().name(), d.titre(), d.contenu(),
                d.auteur(), d.statut().name(), d.dateCreation(), d.dateValidation(), d.validePar());
    }

    Document versDomaine() {
        return new Document(id, sejourId, patientId, TypeDocument.valueOf(typeDocument), titre, contenu, auteur,
                StatutDocument.valueOf(statut), dateCreation, dateValidation, validePar);
    }
}
