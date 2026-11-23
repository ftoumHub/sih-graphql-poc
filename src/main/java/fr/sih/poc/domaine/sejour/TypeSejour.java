package fr.sih.poc.domaine.sejour;

public enum TypeSejour {
    HOSPITALISATION_COMPLETE,
    AMBULATOIRE,
    URGENCES,
    CONSULTATION_EXTERNE;

    /** Un patient ne peut avoir qu'une seule prise en charge "lit" en cours à la fois. */
    public boolean occupeUnLit() {
        return this != CONSULTATION_EXTERNE;
    }
}
