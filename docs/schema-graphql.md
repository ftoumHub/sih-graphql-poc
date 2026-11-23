# Diagramme du schéma GraphQL

Représentation visuelle du schéma GraphQL de l'application, écrit dans `src/main/resources/graphql/`
(`commun.graphqls`, `patient.graphqls`, `sejour.graphqls`, `document.graphqls`).

Les diagrammes sont au format **Mermaid**. Ils s'affichent directement sur GitHub et GitLab, dans IntelliJ
(plugin Mermaid ou aperçu Markdown) et dans VS Code (extension *Markdown Preview Mermaid Support*).

Pour plus de lisibilité, le schéma est découpé en cinq vues :

1. [Les points d'entrée](#1-les-points-dentrée-query-mutation-subscription) : `Query`, `Mutation`, `Subscription`
2. [Le graphe des objets métier](#2-le-graphe-des-objets-métier) : `Patient`, `Sejour`, `Document`…
3. [Les résultats de mutation et les erreurs](#3-résultats-de-mutation-et-erreurs-métier) : unions `Resultat…`
4. [Les événements temps réel](#4-événements-temps-réel-subscription)
5. [Les énumérations, inputs et scalaires](#5-énumérations-inputs-et-scalaires)

Notation : `!` = non null, `[X]` = liste de X, `«interface»`, `«union»`, `«enum»`, `«input»` = nature du type.

---

## 1. Les points d'entrée (Query, Mutation, Subscription)

Toute requête commence par un champ d'un de ces trois types racines.

```mermaid
classDiagram
    direction LR

    class Query {
        <<racine>>
        patient(id) Patient
        patientParIpp(ipp) Patient
        patients(filtre, page, taille) PatientPage!
        sejour(id) Sejour
        sejourParNumero(numero) Sejour
        sejours(filtre, page, taille) SejourPage!
        document(id) Document
        documents(filtre, page, taille) DocumentPage!
        unitesFonctionnelles() [UniteFonctionnelle!]!
        uniteFonctionnelle(code) UniteFonctionnelle
        recherche(texte, limiteParType) [Noeud!]
        statistiques() Statistiques!
    }

    class Mutation {
        <<racine>>
        creerPatient(entree) ResultatPatient!
        modifierPatient(entree) ResultatPatient!
        admettrePatient(entree) ResultatSejour!
        muterPatient(entree) ResultatSejour!
        sortirPatient(entree) ResultatSejour!
        annulerSejour(entree) ResultatSejour!
        ajouterDocument(entree) ResultatDocument!
        validerDocument(entree) ResultatDocument!
        supprimerDocument(entree) ResultatSuppression!
    }

    class Subscription {
        <<racine>>
        evenements(patientId, types) EvenementSih!
    }
```

| Domaine | Lecture (`Query`) | Écriture (`Mutation`) |
|---|---|---|
| Patients | `patient`, `patientParIpp`, `patients` | `creerPatient`, `modifierPatient` |
| Séjours | `sejour`, `sejourParNumero`, `sejours` | `admettrePatient`, `muterPatient`, `sortirPatient`, `annulerSejour` |
| Documents | `document`, `documents` | `ajouterDocument`, `validerDocument`, `supprimerDocument` |
| Transverse | `recherche`, `statistiques`, `unitesFonctionnelles`, `uniteFonctionnelle` | — |

Les listes `patients`, `sejours` et `documents` sont **paginées** : elles renvoient une `…Page`
avec `contenu` et `infoPage`.

---

## 2. Le graphe des objets métier

C'est le cœur du schéma : les types et leurs **relations navigables**. Toutes les flèches peuvent être
parcourues dans une requête (patient → séjours → documents → patient…), d'où le nom « graphe ».

```mermaid
classDiagram
    direction TB

    class Noeud {
        <<interface>>
        ID! id
    }

    class Patient {
        ID! id
        IPP! ipp
        String! nomNaissance
        String nomUsage
        String! nomAffiche ⚙
        String! prenom
        Date! dateNaissance
        Int! age ⚙
        Boolean! estMineur ⚙
        Sexe! sexe
        Telephone telephone
        DateHeure! dateCreation
        sejours() [Sejour!]! 🔗
        sejourEnCours() Sejour 🔗
        documents(type) [Document!]! 🔗
    }

    class Sejour {
        ID! id
        String! numeroSejour
        TypeSejour! type
        StatutSejour! statut
        String motif
        DateHeure! dateEntree
        DateHeure dateSortie
        ModeSortie modeSortie
        Int! dureeJours ⚙
        Boolean! estEnCours ⚙
        patient() Patient! 🔗
        ufCourante() UniteFonctionnelle! 🔗
        mouvements() [Mouvement!]!
        documents() [Document!]! 🔗
    }

    class Mouvement {
        TypeMouvement! type
        DateHeure! dateMouvement
        String! ufCode ⛔ deprecated
        uniteFonctionnelle() UniteFonctionnelle! 🔗
    }

    class Document {
        ID! id
        TypeDocument! type
        String! titre
        contenu(longueurMax) String ⚙
        Int! taille ⚙
        String! auteur
        StatutDocument! statut
        DateHeure! dateCreation
        DateHeure dateValidation
        String validePar
        sejour() Sejour! 🔗
        patient() Patient! 🔗
    }

    class UniteFonctionnelle {
        String! code
        String! libelle
        String! service
        Int! capacite
        sejoursEnCours() [Sejour!]! 🔗
        Float tauxOccupation ⚙🔗
    }

    Noeud <|.. Patient
    Noeud <|.. Sejour
    Noeud <|.. Document

    Patient "1" --> "0..*" Sejour : sejours / sejourEnCours
    Patient "1" --> "0..*" Document : documents
    Sejour "0..*" --> "1" Patient : patient
    Sejour "1" *-- "1..*" Mouvement : mouvements
    Sejour "1" --> "0..*" Document : documents
    Sejour "0..*" --> "1" UniteFonctionnelle : ufCourante
    Mouvement "0..*" --> "1" UniteFonctionnelle : uniteFonctionnelle
    Document "0..*" --> "1" Sejour : sejour
    Document "0..*" --> "1" Patient : patient
    UniteFonctionnelle "1" --> "0..*" Sejour : sejoursEnCours
```

**Légende**

| Symbole | Signification | Côté serveur |
|---|---|---|
| ⚙ | Champ **calculé**, absent de la base | `@SchemaMapping` (ex. `age`, `contenu(longueurMax)`) ou méthode du record Java |
| 🔗 | **Relation** vers un autre agrégat, chargée à la demande | `@BatchMapping` dans `RelationsController` (sauf `sejourEnCours`, laissé exprès en `@SchemaMapping` pour montrer le N+1) |
| ⛔ | Champ **déprécié** (`@deprecated`) | Toujours servi, mais signalé par l'introspection (requête q17) |
| `*--` | Composition : les mouvements **appartiennent** au séjour | Chargés avec le séjour (même agrégat Spring Data JDBC) |

**Types de pagination** (non représentés ci-dessus) :

```mermaid
classDiagram
    direction LR
    class PatientPage {
        [Patient!]! contenu
        InfoPage! infoPage
    }
    class SejourPage {
        [Sejour!]! contenu
        InfoPage! infoPage
    }
    class DocumentPage {
        [Document!]! contenu
        InfoPage! infoPage
    }
    class InfoPage {
        Int! page
        Int! taille
        Int! totalElements
        Int! totalPages
        Boolean! estDernierePage
    }
    PatientPage --> InfoPage
    SejourPage --> InfoPage
    DocumentPage --> InfoPage
```

**Statistiques** : chaque champ est calculé par une requête SQL dédiée, exécutée seulement si le champ est demandé.

```mermaid
classDiagram
    direction LR
    class Statistiques {
        Int! nombrePatients
        Int! nombreSejoursEnCours
        [OccupationUf!]! occupationParUf
        [CompteurDocuments!]! documentsParTypeEtStatut
        Float dureeMoyenneSejoursClosJours
    }
    class OccupationUf {
        String! ufCode
        String! libelle
        Int! capacite
        Int! patientsPresents
        Float tauxOccupation
    }
    class CompteurDocuments {
        TypeDocument! type
        StatutDocument! statut
        Int! nombre
    }
    Statistiques --> OccupationUf
    Statistiques --> CompteurDocuments
```

---

## 3. Résultats de mutation et erreurs métier

Chaque mutation renvoie une **union** : soit l'objet dans son nouvel état, soit une `ErreurMetier`.
Le client distingue les deux avec `__typename` et des fragments inline (`... on ErreurMetier { code }`).

```mermaid
classDiagram
    direction LR

    class ResultatPatient {
        <<union>>
    }
    class ResultatSejour {
        <<union>>
    }
    class ResultatDocument {
        <<union>>
    }
    class ResultatSuppression {
        <<union>>
    }

    class ErreurMetier {
        CodeErreur! code
        String! message
        [DetailErreur!]! details
    }
    class DetailErreur {
        String! champ
        String! message
    }
    class DocumentSupprime {
        ID! id
    }

    ResultatPatient ..> Patient
    ResultatPatient ..> ErreurMetier
    ResultatSejour ..> Sejour
    ResultatSejour ..> ErreurMetier
    ResultatDocument ..> Document
    ResultatDocument ..> ErreurMetier
    ResultatSuppression ..> DocumentSupprime
    ResultatSuppression ..> ErreurMetier
    ErreurMetier --> "0..*" DetailErreur : details
```

| Mutation | Union renvoyée | Codes d'erreur possibles |
|---|---|---|
| `creerPatient` | `ResultatPatient` | `VALIDATION`, `IPP_DEJA_UTILISE`, `DATE_NAISSANCE_FUTURE` |
| `modifierPatient` | `ResultatPatient` | `VALIDATION`, `RESSOURCE_INTROUVABLE` |
| `admettrePatient` | `ResultatSejour` | `VALIDATION`, `RESSOURCE_INTROUVABLE`, `SEJOUR_DEJA_EN_COURS`, `DATE_FUTURE` |
| `muterPatient` | `ResultatSejour` | `RESSOURCE_INTROUVABLE`, `SEJOUR_NON_EN_COURS`, `MUTATION_MEME_UF`, `CHRONOLOGIE_INVALIDE` |
| `sortirPatient` | `ResultatSejour` | `RESSOURCE_INTROUVABLE`, `SEJOUR_NON_EN_COURS`, `LETTRE_LIAISON_MANQUANTE`, `CHRONOLOGIE_INVALIDE` |
| `annulerSejour` | `ResultatSejour` | `RESSOURCE_INTROUVABLE`, `SEJOUR_NON_EN_COURS` |
| `ajouterDocument` | `ResultatDocument` | `VALIDATION`, `RESSOURCE_INTROUVABLE`, `SEJOUR_ANNULE` |
| `validerDocument` | `ResultatDocument` | `VALIDATION`, `RESSOURCE_INTROUVABLE`, `DOCUMENT_DEJA_VALIDE` |
| `supprimerDocument` | `ResultatSuppression` | `RESSOURCE_INTROUVABLE`, `DOCUMENT_VALIDE_NON_SUPPRIMABLE` |

Les codes `PAGINATION_INVALIDE` et `RECHERCHE_TROP_COURTE` concernent des **queries** : ils arrivent dans
le tableau `errors` de la réponse, pas dans une union.

---

## 4. Événements temps réel (Subscription)

`Subscription.evenements` renvoie l'interface `EvenementSih`. Une mutation réussie publie un événement,
qui est envoyé aux abonnés par WebSocket (`/graphql-ws`).

```mermaid
classDiagram
    direction LR

    class EvenementSih {
        <<interface>>
        TypeEvenement! type
        DateHeure! horodatage
    }
    class EvenementSejour {
        TypeEvenement! type
        DateHeure! horodatage
        Sejour! sejour
    }
    class EvenementDocument {
        TypeEvenement! type
        DateHeure! horodatage
        Document! document
    }

    EvenementSih <|.. EvenementSejour
    EvenementSih <|.. EvenementDocument
    EvenementSejour --> Sejour : état APRÈS l'événement
    EvenementDocument --> Document : état APRÈS l'événement
```

| Mutation | Événement publié | Type |
|---|---|---|
| `admettrePatient` | `ADMISSION` | `EvenementSejour` |
| `muterPatient` | `MUTATION` | `EvenementSejour` |
| `sortirPatient` | `SORTIE` | `EvenementSejour` |
| `annulerSejour` | `ANNULATION` | `EvenementSejour` |
| `ajouterDocument` | `DOCUMENT_AJOUTE` | `EvenementDocument` |
| `validerDocument` | `DOCUMENT_VALIDE` | `EvenementDocument` |

---

## 5. Énumérations, inputs et scalaires

### Scalaires personnalisés

| Scalaire | Format | Exemple | Validé |
|---|---|---|---|
| `Date` | `yyyy-MM-dd` | `1948-03-12` | avant exécution |
| `DateHeure` | `yyyy-MM-dd'T'HH:mm:ss`, heure locale | `2026-11-18T09:30:00` | avant exécution |
| `IPP` | exactement 8 chiffres | `00010001` | avant exécution |
| `Telephone` | 10 chiffres commençant par 0 | `0612345678` | avant exécution |

### Énumérations

| Enum | Valeurs |
|---|---|
| `Sexe` | `M`, `F`, `I` |
| `TypeSejour` | `HOSPITALISATION_COMPLETE`, `AMBULATOIRE`, `URGENCES`, `CONSULTATION_EXTERNE` |
| `StatutSejour` | `EN_COURS`, `CLOS`, `ANNULE` |
| `TypeMouvement` | `ENTREE`, `MUTATION`, `SORTIE` |
| `ModeSortie` | `DOMICILE`, `TRANSFERT`, `DECES`, `CONTRE_AVIS_MEDICAL` |
| `TypeDocument` | `COMPTE_RENDU_HOSPITALISATION`, `COMPTE_RENDU_OPERATOIRE`, `COMPTE_RENDU_CONSULTATION`, `COMPTE_RENDU_IMAGERIE`, `LETTRE_DE_LIAISON`, `ORDONNANCE`, `RESULTAT_BIOLOGIE` |
| `StatutDocument` | `BROUILLON`, `VALIDE` |
| `TypeEvenement` | `ADMISSION`, `MUTATION`, `SORTIE`, `ANNULATION`, `DOCUMENT_AJOUTE`, `DOCUMENT_VALIDE` |
| `CodeErreur` | voir le tableau de la section 3 (+ `PAGINATION_INVALIDE`, `RECHERCHE_TROP_COURTE`) |

### Cycles de vie

```mermaid
stateDiagram-v2
    direction LR
    state "Séjour" as S {
        [*] --> EN_COURS : admettrePatient
        EN_COURS --> EN_COURS : muterPatient
        EN_COURS --> CLOS : sortirPatient
        EN_COURS --> ANNULE : annulerSejour
        CLOS --> [*]
        ANNULE --> [*]
    }
```

```mermaid
stateDiagram-v2
    direction LR
    state "Document" as D {
        [*] --> BROUILLON : ajouterDocument
        BROUILLON --> VALIDE : validerDocument
        BROUILLON --> [*] : supprimerDocument
        VALIDE --> [*]
    }
```

### Inputs (arguments des mutations et filtres)

```mermaid
classDiagram
    direction LR
    class CreerPatientInput {
        <<input>>
        IPP! ipp
        String! nomNaissance
        String nomUsage
        String! prenom
        Date! dateNaissance
        Sexe! sexe
        Telephone telephone
    }
    class ModifierPatientInput {
        <<input>>
        ID! patientId
        String nomNaissance
        String nomUsage
        String prenom
        Telephone telephone
    }
    class AdmissionInput {
        <<input>>
        ID! patientId
        TypeSejour! type
        String! ufCode
        String motif
        DateHeure dateEntree
    }
    class MutationPatientInput {
        <<input>>
        ID! sejourId
        String! ufDestination
        DateHeure dateMutation
    }
    class SortieInput {
        <<input>>
        ID! sejourId
        ModeSortie! modeSortie
        DateHeure dateSortie
    }
    class AnnulationSejourInput {
        <<input>>
        ID! sejourId
    }
    class AjoutDocumentInput {
        <<input>>
        ID! sejourId
        TypeDocument! type
        String! titre
        String contenu
        String! auteur
    }
    class ValidationDocumentInput {
        <<input>>
        ID! documentId
        String! validePar
    }
    class SuppressionDocumentInput {
        <<input>>
        ID! documentId
    }
```

```mermaid
classDiagram
    direction LR
    class FiltrePatient {
        <<input>>
        String nom
        String prenom
        Sexe sexe
        Date neApres
        Date neAvant
    }
    class FiltreSejour {
        <<input>>
        ID patientId
        [StatutSejour!] statuts
        TypeSejour type
        String ufCode
        DateHeure entreApres
        DateHeure entreAvant
    }
    class FiltreDocument {
        <<input>>
        ID patientId
        ID sejourId
        [TypeDocument!] types
        StatutDocument statut
        String auteur
        String titreContient
    }
```

---

Voir aussi : [schéma de la base de données](schema-bdd.md), qui montre comment ces types sont stockés,
et [lexique GraphQL](lexique-graphql.md) pour le vocabulaire.
