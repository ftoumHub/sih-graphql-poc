package fr.sih.poc.domaine.evenement;

/** Port secondaire de publication des événements métier. */
public interface PublicateurEvenements {

    void publier(EvenementSih evenement);
}
