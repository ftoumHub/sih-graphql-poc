package fr.sih.poc.infrastructure.persistance;

import fr.sih.poc.domaine.referentiel.UniteFonctionnelle;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Référentiel en lecture seule : l'identifiant est le code métier (pas de génération). */
@Table("unite_fonctionnelle")
public record UniteFonctionnelleEntity(@Id String code, String libelle, String service, int capacite) {

    UniteFonctionnelle versDomaine() {
        return new UniteFonctionnelle(code, libelle, service, capacite);
    }
}
