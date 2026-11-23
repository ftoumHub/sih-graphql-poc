package fr.sih.poc.config;

import fr.sih.poc.application.DocumentService;
import fr.sih.poc.application.PatientService;
import fr.sih.poc.application.RechercheService;
import fr.sih.poc.application.ReferentielService;
import fr.sih.poc.application.SejourService;
import fr.sih.poc.application.StatistiquesService;
import fr.sih.poc.application.Transactions;
import fr.sih.poc.domaine.document.DocumentRepository;
import fr.sih.poc.domaine.patient.PatientRepository;
import fr.sih.poc.domaine.referentiel.UniteFonctionnelleRepository;
import fr.sih.poc.domaine.sejour.SejourRepository;
import fr.sih.poc.domaine.statistiques.StatistiquesRepository;
import fr.sih.poc.infrastructure.evenement.FluxEvenementsSih;
import fr.sih.poc.infrastructure.persistance.DocumentJdbcRepository;
import fr.sih.poc.infrastructure.persistance.DocumentRepositoryAdapter;
import fr.sih.poc.infrastructure.persistance.PatientJdbcRepository;
import fr.sih.poc.infrastructure.persistance.PatientRepositoryAdapter;
import fr.sih.poc.infrastructure.persistance.SejourJdbcRepository;
import fr.sih.poc.infrastructure.persistance.SejourRepositoryAdapter;
import fr.sih.poc.infrastructure.persistance.StatistiquesJdbcAdapter;
import fr.sih.poc.infrastructure.persistance.UniteFonctionnelleJdbcRepository;
import fr.sih.poc.infrastructure.persistance.UniteFonctionnelleRepositoryAdapter;
import fr.sih.poc.infrastructure.transaction.TransactionsSpring;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Câblage explicite de l'hexagone : domaine et services applicatifs sont du Java pur,
 * c'est ici (et seulement ici) qu'on les relie aux adaptateurs Spring.
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

    @Bean
    Clock horloge() {
        return Clock.system(ZoneId.of("Europe/Paris"));
    }

    @Bean
    Transactions transactions(PlatformTransactionManager transactionManager) {
        return new TransactionsSpring(new TransactionTemplate(transactionManager));
    }

    @Bean
    FluxEvenementsSih fluxEvenementsSih() {
        return new FluxEvenementsSih();
    }

    // ----------------------------------------------------------------- adaptateurs de persistance

    @Bean
    PatientRepository patientRepository(PatientJdbcRepository repository, JdbcAggregateTemplate template) {
        return new PatientRepositoryAdapter(repository, template);
    }

    @Bean
    SejourRepository sejourRepository(SejourJdbcRepository repository, JdbcAggregateTemplate template, JdbcClient jdbcClient) {
        return new SejourRepositoryAdapter(repository, template, jdbcClient);
    }

    @Bean
    DocumentRepository documentRepository(DocumentJdbcRepository repository, JdbcAggregateTemplate template) {
        return new DocumentRepositoryAdapter(repository, template);
    }

    @Bean
    UniteFonctionnelleRepository uniteFonctionnelleRepository(UniteFonctionnelleJdbcRepository repository) {
        return new UniteFonctionnelleRepositoryAdapter(repository);
    }

    @Bean
    StatistiquesRepository statistiquesRepository(JdbcClient jdbcClient) {
        return new StatistiquesJdbcAdapter(jdbcClient);
    }

    // ----------------------------------------------------------------- services applicatifs

    @Bean
    PatientService patientService(PatientRepository patients, Transactions transactions, Clock horloge) {
        return new PatientService(patients, transactions, horloge);
    }

    @Bean
    SejourService sejourService(SejourRepository sejours, PatientRepository patients, DocumentRepository documents,
                                UniteFonctionnelleRepository ufs, FluxEvenementsSih evenements,
                                Transactions transactions, Clock horloge) {
        return new SejourService(sejours, patients, documents, ufs, evenements, transactions, horloge);
    }

    @Bean
    DocumentService documentService(DocumentRepository documents, SejourRepository sejours,
                                    FluxEvenementsSih evenements, Transactions transactions, Clock horloge) {
        return new DocumentService(documents, sejours, evenements, transactions, horloge);
    }

    @Bean
    ReferentielService referentielService(UniteFonctionnelleRepository ufs) {
        return new ReferentielService(ufs);
    }

    @Bean
    RechercheService rechercheService(PatientRepository patients, SejourRepository sejours, DocumentRepository documents) {
        return new RechercheService(patients, sejours, documents);
    }

    @Bean
    StatistiquesService statistiquesService(StatistiquesRepository statistiques) {
        return new StatistiquesService(statistiques);
    }
}
