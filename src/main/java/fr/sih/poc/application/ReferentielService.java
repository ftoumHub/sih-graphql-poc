package fr.sih.poc.application;

import fr.sih.poc.domaine.referentiel.UniteFonctionnelle;
import fr.sih.poc.domaine.referentiel.UniteFonctionnelleRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ReferentielService {

    private final UniteFonctionnelleRepository unitesFonctionnelles;

    public ReferentielService(UniteFonctionnelleRepository unitesFonctionnelles) {
        this.unitesFonctionnelles = unitesFonctionnelles;
    }

    public List<UniteFonctionnelle> unitesFonctionnelles() {
        return unitesFonctionnelles.toutes();
    }

    public Optional<UniteFonctionnelle> uniteFonctionnelle(String code) {
        return unitesFonctionnelles.parCode(code);
    }

    public List<UniteFonctionnelle> parCodes(Collection<String> codes) {
        return unitesFonctionnelles.parCodes(codes);
    }
}
