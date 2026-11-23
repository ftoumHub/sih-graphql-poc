package fr.sih.poc.api.graphql.erreurs;

import fr.sih.poc.domaine.commun.CodeErreur;
import fr.sih.poc.domaine.commun.ErreurMetier;
import fr.sih.poc.domaine.commun.ErreurMetier.DetailErreur;
import io.vavr.control.Either;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Bean Validation intégrée au flux Either : au lieu de laisser Spring lever une
 * ConstraintViolationException (@Valid), on valide explicitement et on obtient
 * Left(ErreurMetier VALIDATION) avec un détail par champ invalide, ou Right(entree).
 * Toutes les violations sont renvoyées en une fois.
 */
@Component
public class ValidateurEntrees {

    private final Validator validator;

    public ValidateurEntrees(Validator validator) {
        this.validator = validator;
    }

    public <T> Either<ErreurMetier, T> valider(T entree) {
        Set<ConstraintViolation<T>> violations = validator.validate(entree);
        if (violations.isEmpty()) {
            return Either.right(entree);
        }
        List<DetailErreur> details = violations.stream()
                .map(v -> new DetailErreur(v.getPropertyPath().toString(), v.getMessage()))
                .sorted(Comparator.comparing(DetailErreur::champ))
                .toList();
        return Either.left(new ErreurMetier(CodeErreur.VALIDATION, "Données d'entrée invalides", details));
    }
}
