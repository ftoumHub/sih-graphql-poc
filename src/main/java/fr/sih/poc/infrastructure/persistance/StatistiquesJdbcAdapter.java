package fr.sih.poc.infrastructure.persistance;

import fr.sih.poc.domaine.document.StatutDocument;
import fr.sih.poc.domaine.document.TypeDocument;
import fr.sih.poc.domaine.statistiques.CompteurDocuments;
import fr.sih.poc.domaine.statistiques.OccupationUf;
import fr.sih.poc.domaine.statistiques.StatistiquesRepository;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

/**
 * Lecture "reporting" : du SQL d'agrégation pur, sans passer par les agrégats.
 * C'est le bon outil pour ce besoin (CQRS léger) : pas besoin de charger des séjours pour les compter.
 */
public class StatistiquesJdbcAdapter implements StatistiquesRepository {

    private final JdbcClient jdbc;

    public StatistiquesJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long nombrePatients() {
        return jdbc.sql("SELECT count(*) FROM patient").query(Long.class).single();
    }

    @Override
    public long nombreSejoursEnCours() {
        return jdbc.sql("SELECT count(*) FROM sejour WHERE statut = 'EN_COURS'").query(Long.class).single();
    }

    @Override
    public List<OccupationUf> occupationParUf() {
        return jdbc.sql("""
                        SELECT uf.code, uf.libelle, uf.capacite, count(s.id) AS presents
                        FROM unite_fonctionnelle uf
                        LEFT JOIN sejour s ON s.uf_courante = uf.code AND s.statut = 'EN_COURS'
                        GROUP BY uf.code, uf.libelle, uf.capacite
                        ORDER BY uf.code
                        """)
                .query((rs, i) -> new OccupationUf(rs.getString("code"), rs.getString("libelle"),
                        rs.getInt("capacite"), rs.getInt("presents")))
                .list();
    }

    @Override
    public List<CompteurDocuments> documentsParTypeEtStatut() {
        return jdbc.sql("""
                        SELECT type_document, statut, count(*) AS nombre
                        FROM document
                        GROUP BY type_document, statut
                        ORDER BY type_document, statut
                        """)
                .query((rs, i) -> new CompteurDocuments(TypeDocument.valueOf(rs.getString("type_document")),
                        StatutDocument.valueOf(rs.getString("statut")), rs.getLong("nombre")))
                .list();
    }

    @Override
    public Double dureeMoyenneSejoursClosJours() {
        // avg() renvoie NULL s'il n'y a aucun séjour clos : on lit une liste pour accepter la valeur nulle.
        List<Double> resultat = jdbc.sql("""
                        SELECT round(avg(date_sortie::date - date_entree::date), 1)
                        FROM sejour WHERE statut = 'CLOS'
                        """)
                .query(Double.class).list();
        return resultat.isEmpty() ? null : resultat.get(0);
    }
}
