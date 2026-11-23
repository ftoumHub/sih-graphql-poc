--liquibase formatted sql

--changeset sih:100-donnees-demo context:demo
--comment: Jeu de données de démonstration. Identifiants fixes pour que les requêtes du tutoriel soient reproductibles.

-- ---------------------------------------------------------------- Unités fonctionnelles
INSERT INTO unite_fonctionnelle (code, libelle, service, capacite) VALUES
    ('1201', 'Cardiologie - hospitalisation',      'Cardiologie',            24),
    ('1202', 'USIC (soins intensifs cardiologie)', 'Cardiologie',             8),
    ('2101', 'Chirurgie orthopédique',             'Chirurgie',              30),
    ('3001', 'Urgences adultes',                   'Urgences',               15),
    ('4101', 'Médecine interne',                   'Médecine',               28),
    ('5101', 'Unité de chirurgie ambulatoire',     'Chirurgie',              12),
    ('6101', 'Consultations externes',             'Consultations',           0);

-- ---------------------------------------------------------------- Patients
INSERT INTO patient (id, ipp, nom_naissance, nom_usage, prenom, date_naissance, sexe, telephone, date_creation) VALUES
    (1,  '00010001', 'MARTIN',   NULL,       'Jeanne',  '1948-03-12', 'F', '0612345678', '2026-01-10 09:00'),
    (2,  '00010002', 'BERNARD',  NULL,       'Louis',   '1957-07-21', 'M', '0623456789', '2026-02-03 14:20'),
    (3,  '00010003', 'DUBOIS',   'LEFEVRE',  'Claire',  '1985-11-02', 'F', '0634567890', '2026-03-15 08:45'),
    (4,  '00010004', 'PETIT',    NULL,       'Hugo',    '2012-05-30', 'M', NULL,         '2026-10-05 21:05'),
    (5,  '00010005', 'MOREAU',   NULL,       'Élise',   '1979-01-17', 'F', '0645678901', '2026-04-22 10:10'),
    (6,  '00010006', 'LAURENT',  NULL,       'Paul',    '1951-09-08', 'M', '0656789012', '2026-05-02 16:30'),
    (7,  '00010007', 'GARCIA',   NULL,       'Sofia',   '1966-12-24', 'F', '0667890123', '2026-08-14 03:12'),
    (8,  '00010008', 'ROUX',     NULL,       'Henri',   '1938-04-04', 'M', '0678901234', '2026-06-18 11:00'),
    (9,  '00010009', 'FOURNIER', NULL,       'Nathalie','1990-06-15', 'F', '0689012345', '2026-07-01 07:50'),
    (10, '00010010', 'GIRARD',   NULL,       'Lucas',   '2001-02-27', 'M', '0690123456', '2026-09-12 13:15'),
    (11, '00010011', 'BONNET',   'MERCIER',  'Marie',   '1972-10-19', 'F', '0601234567', '2026-10-30 09:40'),
    (12, '00010012', 'LAMBERT',  NULL,       'Camille', '1995-08-03', 'I', NULL,         '2026-09-30 22:40');

SELECT setval('patient_id_seq', (SELECT max(id) FROM patient));

-- ---------------------------------------------------------------- Séjours
INSERT INTO sejour (id, numero_sejour, patient_id, type_sejour, statut, uf_courante, motif, date_entree, date_sortie, mode_sortie) VALUES
    (1,  '2026000001', 1,  'HOSPITALISATION_COMPLETE', 'CLOS',     '1201', 'Insuffisance cardiaque décompensée', '2026-09-02 10:15', '2026-09-09 14:00', 'DOMICILE'),
    (2,  '2026000002', 1,  'HOSPITALISATION_COMPLETE', 'EN_COURS', '1201', 'Récidive de dyspnée',                '2026-11-14 08:30', NULL,               NULL),
    (3,  '2026000003', 2,  'HOSPITALISATION_COMPLETE', 'EN_COURS', '2101', 'Prothèse totale de hanche droite',   '2026-11-16 07:00', NULL,               NULL),
    (4,  '2026000004', 3,  'AMBULATOIRE',              'CLOS',     '5101', 'Cholécystectomie coelioscopique',    '2026-10-20 07:30', '2026-10-20 17:00', 'DOMICILE'),
    (5,  '2026000005', 3,  'CONSULTATION_EXTERNE',     'CLOS',     '6101', 'Consultation post-opératoire J15',   '2026-11-03 10:00', '2026-11-03 10:30', 'DOMICILE'),
    (6,  '2026000006', 4,  'URGENCES',                 'CLOS',     '3001', 'Traumatisme du poignet gauche',      '2026-10-05 21:10', '2026-10-06 01:30', 'DOMICILE'),
    (7,  '2026000007', 5,  'URGENCES',                 'EN_COURS', '3001', 'Douleur thoracique',                 '2026-11-18 19:45', NULL,               NULL),
    (8,  '2026000008', 6,  'HOSPITALISATION_COMPLETE', 'ANNULE',   '4101', 'Bilan programmé (annulé)',           '2026-11-10 09:00', NULL,               NULL),
    (9,  '2026000009', 6,  'HOSPITALISATION_COMPLETE', 'EN_COURS', '4101', 'Altération de l''état général',      '2026-11-12 11:00', NULL,               NULL),
    (10, '2026000010', 7,  'HOSPITALISATION_COMPLETE', 'CLOS',     '1201', 'Infarctus du myocarde',              '2026-08-14 03:20', '2026-08-25 11:00', 'TRANSFERT'),
    (11, '2026000011', 8,  'HOSPITALISATION_COMPLETE', 'EN_COURS', '4101', 'Pneumopathie',                       '2026-11-01 16:40', NULL,               NULL),
    (12, '2026000012', 9,  'AMBULATOIRE',              'EN_COURS', '5101', 'Arthroscopie du genou droit',        '2026-11-18 07:15', NULL,               NULL),
    (13, '2026000013', 11, 'CONSULTATION_EXTERNE',     'CLOS',     '6101', 'Consultation de cardiologie',        '2026-11-17 14:00', '2026-11-17 14:20', 'DOMICILE'),
    (14, '2026000014', 12, 'URGENCES',                 'CLOS',     '3001', 'Malaise vagal',                      '2026-09-30 22:45', '2026-10-01 03:10', 'DOMICILE');

SELECT setval('sejour_id_seq', (SELECT max(id) FROM sejour));

-- ---------------------------------------------------------------- Mouvements (ordre = index dans la List<Mouvement>)
INSERT INTO mouvement (sejour_id, ordre, type_mouvement, uf, date_mouvement) VALUES
    (1, 0, 'ENTREE',   '3001', '2026-09-02 10:15'),
    (1, 1, 'MUTATION', '1202', '2026-09-02 18:00'),
    (1, 2, 'MUTATION', '1201', '2026-09-04 09:00'),
    (1, 3, 'SORTIE',   '1201', '2026-09-09 14:00'),
    (2, 0, 'ENTREE',   '1201', '2026-11-14 08:30'),
    (3, 0, 'ENTREE',   '2101', '2026-11-16 07:00'),
    (4, 0, 'ENTREE',   '5101', '2026-10-20 07:30'),
    (4, 1, 'SORTIE',   '5101', '2026-10-20 17:00'),
    (5, 0, 'ENTREE',   '6101', '2026-11-03 10:00'),
    (5, 1, 'SORTIE',   '6101', '2026-11-03 10:30'),
    (6, 0, 'ENTREE',   '3001', '2026-10-05 21:10'),
    (6, 1, 'SORTIE',   '3001', '2026-10-06 01:30'),
    (7, 0, 'ENTREE',   '3001', '2026-11-18 19:45'),
    (8, 0, 'ENTREE',   '4101', '2026-11-10 09:00'),
    (9, 0, 'ENTREE',   '3001', '2026-11-12 11:00'),
    (9, 1, 'MUTATION', '4101', '2026-11-12 17:30'),
    (10, 0, 'ENTREE',   '1202', '2026-08-14 03:20'),
    (10, 1, 'MUTATION', '1201', '2026-08-17 10:00'),
    (10, 2, 'SORTIE',   '1201', '2026-08-25 11:00'),
    (11, 0, 'ENTREE',   '4101', '2026-11-01 16:40'),
    (12, 0, 'ENTREE',   '5101', '2026-11-18 07:15'),
    (13, 0, 'ENTREE',   '6101', '2026-11-17 14:00'),
    (13, 1, 'SORTIE',   '6101', '2026-11-17 14:20'),
    (14, 0, 'ENTREE',   '3001', '2026-09-30 22:45'),
    (14, 1, 'SORTIE',   '3001', '2026-10-01 03:10');

-- ---------------------------------------------------------------- Documents
INSERT INTO document (id, sejour_id, patient_id, type_document, titre, contenu, auteur, statut, date_creation, date_validation, valide_par) VALUES
    (1,  1,  1,  'COMPTE_RENDU_HOSPITALISATION', 'CRH - Insuffisance cardiaque décompensée',
         'Patiente de 78 ans admise via les urgences pour dyspnée de stade IV. Passage USIC 36h. Évolution favorable sous diurétiques IV.',
         'Dr Claire Durand', 'VALIDE', '2026-09-09 11:00', '2026-09-09 13:30', 'Dr Claire Durand'),
    (2,  1,  1,  'LETTRE_DE_LIAISON', 'Lettre de liaison de sortie',
         'Traitement de sortie : furosémide 40 mg/j, bisoprolol 2,5 mg/j. Consultation de contrôle à 1 mois.',
         'Dr Claire Durand', 'VALIDE', '2026-09-09 12:00', '2026-09-09 13:45', 'Dr Claire Durand'),
    (3,  1,  1,  'ORDONNANCE', 'Ordonnance de sortie',
         'Furosémide 40 mg : 1 cp le matin. Bisoprolol 2,5 mg : 1 cp le matin.',
         'Dr Claire Durand', 'VALIDE', '2026-09-09 12:10', '2026-09-09 13:50', 'Dr Claire Durand'),
    (4,  1,  1,  'RESULTAT_BIOLOGIE', 'BNP et ionogramme d''entrée',
         'BNP 1850 pg/mL. Na 134 mmol/L. K 4,1 mmol/L. Créatinine 112 µmol/L.',
         'Laboratoire', 'VALIDE', '2026-09-02 11:30', '2026-09-02 12:00', 'Dr Paul Biologiste'),
    (5,  2,  1,  'RESULTAT_BIOLOGIE', 'Bilan biologique d''entrée',
         'BNP 1320 pg/mL. Troponine négative.',
         'Laboratoire', 'VALIDE', '2026-11-14 09:45', '2026-11-14 10:00', 'Dr Paul Biologiste'),
    (6,  2,  1,  'COMPTE_RENDU_HOSPITALISATION', 'CRH - Récidive de dyspnée (en cours de rédaction)',
         'Brouillon : patiente réhospitalisée pour récidive...',
         'Dr Claire Durand', 'BROUILLON', '2026-11-17 16:00', NULL, NULL),
    (7,  3,  2,  'COMPTE_RENDU_OPERATOIRE', 'CRO - Prothèse totale de hanche droite',
         'Voie d''abord postéro-latérale. Mise en place d''une PTH cimentée. Pas d''incident per-opératoire.',
         'Dr Marc Leroy', 'VALIDE', '2026-11-16 12:30', '2026-11-16 13:00', 'Dr Marc Leroy'),
    (8,  3,  2,  'ORDONNANCE', 'Anticoagulation post-opératoire',
         'Enoxaparine 4000 UI/j pendant 35 jours.',
         'Dr Marc Leroy', 'BROUILLON', '2026-11-17 09:00', NULL, NULL),
    (9,  4,  3,  'COMPTE_RENDU_OPERATOIRE', 'CRO - Cholécystectomie coelioscopique',
         'Cholécystectomie par 4 trocarts. Cholangiographie normale.',
         'Dr Sarah Klein', 'VALIDE', '2026-10-20 11:00', '2026-10-20 11:30', 'Dr Sarah Klein'),
    (10, 4,  3,  'LETTRE_DE_LIAISON', 'Lettre de liaison - chirurgie ambulatoire',
         'Sortie autorisée. Antalgiques de palier 1. Consultation à J15.',
         'Dr Sarah Klein', 'VALIDE', '2026-10-20 16:30', '2026-10-20 16:45', 'Dr Sarah Klein'),
    (11, 5,  3,  'COMPTE_RENDU_CONSULTATION', 'Consultation post-opératoire J15',
         'Cicatrices propres. Reprise du travail autorisée.',
         'Dr Sarah Klein', 'VALIDE', '2026-11-03 10:30', '2026-11-03 10:35', 'Dr Sarah Klein'),
    (12, 6,  4,  'COMPTE_RENDU_IMAGERIE', 'Radiographie du poignet gauche',
         'Pas de fracture visible. Tuméfaction des parties molles.',
         'Dr Ines Radio', 'VALIDE', '2026-10-05 22:30', '2026-10-05 22:45', 'Dr Ines Radio'),
    (13, 6,  4,  'LETTRE_DE_LIAISON', 'Lettre de liaison des urgences',
         'Entorse bénigne du poignet. Attelle 10 jours.',
         'Dr Yann Urgentiste', 'VALIDE', '2026-10-06 01:15', '2026-10-06 01:20', 'Dr Yann Urgentiste'),
    (14, 9,  6,  'RESULTAT_BIOLOGIE', 'NFS, CRP',
         'Hb 10,2 g/dL. CRP 85 mg/L.',
         'Laboratoire', 'VALIDE', '2026-11-12 13:00', '2026-11-12 13:30', 'Dr Paul Biologiste'),
    (15, 9,  6,  'RESULTAT_BIOLOGIE', 'Bilan hépatique',
         'ASAT 45 UI/L. ALAT 52 UI/L. GGT 120 UI/L.',
         'Laboratoire', 'VALIDE', '2026-11-13 08:00', '2026-11-13 08:20', 'Dr Paul Biologiste'),
    (16, 9,  6,  'COMPTE_RENDU_IMAGERIE', 'Scanner thoraco-abdomino-pelvien',
         'Brouillon en attente de relecture.',
         'Dr Ines Radio', 'BROUILLON', '2026-11-14 15:00', NULL, NULL),
    (17, 10, 7,  'COMPTE_RENDU_OPERATOIRE', 'Coronarographie et angioplastie',
         'Occlusion de l''IVA proximale. Angioplastie avec pose d''un stent actif.',
         'Dr Antoine Morel', 'VALIDE', '2026-08-14 06:00', '2026-08-14 07:00', 'Dr Antoine Morel'),
    (18, 10, 7,  'COMPTE_RENDU_HOSPITALISATION', 'CRH - Infarctus du myocarde',
         'SCA ST+ antérieur traité par angioplastie primaire. Transfert en SSR cardiologique.',
         'Dr Claire Durand', 'VALIDE', '2026-08-25 09:00', '2026-08-25 10:00', 'Dr Claire Durand'),
    (19, 10, 7,  'LETTRE_DE_LIAISON', 'Lettre de liaison - transfert SSR',
         'Double antiagrégation plaquettaire 12 mois.',
         'Dr Claire Durand', 'VALIDE', '2026-08-25 09:30', '2026-08-25 10:15', 'Dr Claire Durand'),
    (20, 11, 8,  'RESULTAT_BIOLOGIE', 'Hémocultures',
         'Streptococcus pneumoniae sur 2 flacons sur 4.',
         'Laboratoire', 'VALIDE', '2026-11-03 10:00', '2026-11-03 10:30', 'Dr Paul Biologiste'),
    (21, 11, 8,  'ORDONNANCE', 'Antibiothérapie',
         'Amoxicilline 1 g x 3/j IV pendant 7 jours.',
         'Dr Nadia Benali', 'VALIDE', '2026-11-03 11:00', '2026-11-03 11:10', 'Dr Nadia Benali'),
    (22, 13, 11, 'COMPTE_RENDU_CONSULTATION', 'Consultation de cardiologie',
         'Souffle systolique fonctionnel. Échographie cardiaque normale.',
         'Dr Antoine Morel', 'VALIDE', '2026-11-17 14:20', '2026-11-17 14:30', 'Dr Antoine Morel'),
    (23, 14, 12, 'LETTRE_DE_LIAISON', 'Lettre de liaison des urgences',
         'Malaise vagal typique. ECG normal. Retour à domicile.',
         'Dr Yann Urgentiste', 'VALIDE', '2026-10-01 03:00', '2026-10-01 03:05', 'Dr Yann Urgentiste');

SELECT setval('document_id_seq', (SELECT max(id) FROM document));
--rollback DELETE FROM document; DELETE FROM mouvement; DELETE FROM sejour; DELETE FROM patient; DELETE FROM unite_fonctionnelle;
