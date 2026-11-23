package fr.sih.poc.domaine.referentiel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port secondaire : accès au référentiel des UF. */
public interface UniteFonctionnelleRepository {

    List<UniteFonctionnelle> toutes();

    Optional<UniteFonctionnelle> parCode(String code);

    List<UniteFonctionnelle> parCodes(Collection<String> codes);
}
