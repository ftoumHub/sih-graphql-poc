package fr.sih.poc.application;

import fr.sih.poc.domaine.commun.ErreurMetier;
import io.vavr.control.Either;

import java.util.function.Supplier;

/**
 * Port de démarcation transactionnelle. Les services applicatifs restent du Java pur
 * (pas de @Transactional).
 *
 * Contrat important : une erreur métier n'est plus une exception, donc elle ne provoquerait
 * pas de rollback "automatique". L'implémentation DOIT annuler la transaction quand le
 * traitement renvoie un Left, pour ne jamais committer une écriture partielle.
 */
public interface Transactions {

    <T> Either<ErreurMetier, T> executer(Supplier<Either<ErreurMetier, T>> traitement);
}
