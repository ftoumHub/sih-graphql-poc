--liquibase formatted sql

--changeset sih:001-unite-fonctionnelle
--comment: Référentiel des unités fonctionnelles (UF) de l'établissement
CREATE TABLE unite_fonctionnelle (
    code      VARCHAR(10)  PRIMARY KEY,
    libelle   VARCHAR(100) NOT NULL,
    service   VARCHAR(100) NOT NULL,
    capacite  INTEGER      NOT NULL CHECK (capacite >= 0)
);
--rollback DROP TABLE unite_fonctionnelle;

--changeset sih:002-patient
--comment: Agrégat Patient. L'IPP (identifiant permanent du patient) est unique dans l'établissement.
CREATE TABLE patient (
    id             BIGSERIAL    PRIMARY KEY,
    ipp            VARCHAR(20)  NOT NULL UNIQUE,
    nom_naissance  VARCHAR(100) NOT NULL,
    nom_usage      VARCHAR(100),
    prenom         VARCHAR(100) NOT NULL,
    date_naissance DATE         NOT NULL,
    sexe           VARCHAR(1)   NOT NULL CHECK (sexe IN ('M', 'F', 'I')),
    telephone      VARCHAR(20),
    date_creation  TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_patient_nom_naissance ON patient (upper(nom_naissance));
--rollback DROP TABLE patient;

--changeset sih:003-sejour
--comment: Agrégat Séjour (racine) + ses mouvements (entités internes à l'agrégat)
CREATE SEQUENCE seq_numero_sejour START WITH 1000;

CREATE TABLE sejour (
    id             BIGSERIAL    PRIMARY KEY,
    numero_sejour  VARCHAR(20)  NOT NULL UNIQUE,
    patient_id     BIGINT       NOT NULL REFERENCES patient (id),
    type_sejour    VARCHAR(30)  NOT NULL
        CHECK (type_sejour IN ('HOSPITALISATION_COMPLETE', 'AMBULATOIRE', 'URGENCES', 'CONSULTATION_EXTERNE')),
    statut         VARCHAR(20)  NOT NULL CHECK (statut IN ('EN_COURS', 'CLOS', 'ANNULE')),
    uf_courante    VARCHAR(10)  NOT NULL REFERENCES unite_fonctionnelle (code),
    motif          VARCHAR(255),
    date_entree    TIMESTAMP    NOT NULL,
    date_sortie    TIMESTAMP,
    mode_sortie    VARCHAR(30)
        CHECK (mode_sortie IS NULL OR mode_sortie IN ('DOMICILE', 'TRANSFERT', 'DECES', 'CONTRE_AVIS_MEDICAL')),
    CONSTRAINT ck_sejour_dates CHECK (date_sortie IS NULL OR date_sortie >= date_entree)
);
CREATE INDEX idx_sejour_patient ON sejour (patient_id);
CREATE INDEX idx_sejour_statut_uf ON sejour (statut, uf_courante);

-- Spring Data JDBC gère une List<Mouvement> avec :
--   - une colonne de référence vers la racine (sejour_id)
--   - une colonne d'index de liste (ordre)
-- À chaque sauvegarde du séjour, les mouvements sont supprimés puis ré-insérés.
CREATE TABLE mouvement (
    sejour_id       BIGINT      NOT NULL REFERENCES sejour (id) ON DELETE CASCADE,
    ordre           INTEGER     NOT NULL,
    type_mouvement  VARCHAR(20) NOT NULL CHECK (type_mouvement IN ('ENTREE', 'MUTATION', 'SORTIE')),
    uf              VARCHAR(10) NOT NULL REFERENCES unite_fonctionnelle (code),
    date_mouvement  TIMESTAMP   NOT NULL,
    PRIMARY KEY (sejour_id, ordre)
);
--rollback DROP TABLE mouvement; DROP TABLE sejour; DROP SEQUENCE seq_numero_sejour;

--changeset sih:004-document
--comment: Agrégat Document, rattaché à un séjour (et au patient, dénormalisé pour les recherches)
CREATE TABLE document (
    id               BIGSERIAL    PRIMARY KEY,
    sejour_id        BIGINT       NOT NULL REFERENCES sejour (id),
    patient_id       BIGINT       NOT NULL REFERENCES patient (id),
    type_document    VARCHAR(40)  NOT NULL CHECK (type_document IN (
                         'COMPTE_RENDU_HOSPITALISATION', 'COMPTE_RENDU_OPERATOIRE', 'COMPTE_RENDU_CONSULTATION',
                         'COMPTE_RENDU_IMAGERIE', 'LETTRE_DE_LIAISON', 'ORDONNANCE', 'RESULTAT_BIOLOGIE')),
    titre            VARCHAR(255) NOT NULL,
    contenu          TEXT,
    auteur           VARCHAR(100) NOT NULL,
    statut           VARCHAR(20)  NOT NULL CHECK (statut IN ('BROUILLON', 'VALIDE')),
    date_creation    TIMESTAMP    NOT NULL,
    date_validation  TIMESTAMP,
    valide_par       VARCHAR(100)
);
CREATE INDEX idx_document_sejour ON document (sejour_id);
CREATE INDEX idx_document_patient ON document (patient_id);
--rollback DROP TABLE document;
