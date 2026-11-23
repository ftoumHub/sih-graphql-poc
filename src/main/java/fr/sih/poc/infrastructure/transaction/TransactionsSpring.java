package fr.sih.poc.infrastructure.transaction;

import fr.sih.poc.application.Transactions;
import fr.sih.poc.domaine.commun.ErreurMetier;
import io.vavr.control.Either;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Implémentation du port Transactions avec un TransactionTemplate (démarcation programmatique). */
public class TransactionsSpring implements Transactions {

    private final TransactionTemplate transactionTemplate;

    public TransactionsSpring(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> Either<ErreurMetier, T> executer(Supplier<Either<ErreurMetier, T>> traitement) {
        return transactionTemplate.execute(statut -> {
            Either<ErreurMetier, T> resultat = traitement.get();
            if (resultat.isLeft()) {
                statut.setRollbackOnly(); // erreur métier => aucune écriture conservée
            }
            return resultat;
        });
    }
}
