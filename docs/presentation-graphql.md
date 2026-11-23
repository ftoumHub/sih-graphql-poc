---
marp: true
title: GraphQL pour les débutants
paginate: true
---

# GraphQL
## Demander exactement ce dont on a besoin

Présentation pour développeurs débutants · 15 minutes
Exemples tirés du POC **SIH** (Spring Boot 3.5 / Spring for GraphQL)

<!-- Notes pédagogiques détaillées pour l'orateur : docs/presentation-graphql-notes-orateur.md -->

<!--
Notes orateur (0:00 → 0:30)
Se présenter. Annoncer le plan : le problème, le langage, le serveur, les pièges, une démo.
Fil rouge : un mini système d'information hospitalier (patients, séjours, documents).
-->

---

## Plan (15 min)

| Temps | Partie |
|---|---|
| 2 min | Le problème que résout GraphQL |
| 3 min | Le schéma : le contrat de l'API |
| 3 min | Écrire des requêtes |
| 3 min | Côté serveur : Spring for GraphQL |
| 2 min | Erreurs et sécurité |
| 2 min | Démo + questions |

---

## 1. Le problème : un écran « dossier patient » en REST

Pour afficher un patient, ses séjours et ses documents :

```
GET /patients/1                 → 25 champs, j'en affiche 4     (sur-récupération)
GET /patients/1/sejours         → 2e aller-retour
GET /sejours/12/documents       → 3e aller-retour…            (sous-récupération)
```

- Trop de données **et** pas assez à la fois
- Un nouvel écran ⇒ un nouvel endpoint, un nouveau DTO
- Le front attend le back

<!--
Notes orateur (0:30 → 2:30)
Demander : qui a déjà écrit 3 DTO différents pour la même entité ?
C'est le problème qu'a rencontré Facebook en 2012 avec ses applis mobiles → GraphQL, open source en 2015.
-->

---

## La réponse GraphQL : une requête, la forme exacte

```graphql
query {
  patient(id: 1) {
    nomAffiche
    sejours {
      numeroSejour
      documents { titre statut }
    }
  }
}
```

```json
{
   "data": {
      "patient": {
         "nomAffiche": "MARTIN Jean",
         "sejours": [
            {
               "numeroSejour": "...",
               "documents": [ ... ]
            }
         ]
      }
   }
}
```

- **Un seul endpoint** : `POST /graphql`
- Le **client** choisit les champs ; la réponse a la **même forme** que la requête
- **Un aller-retour** pour tout l'écran

<!--
Notes orateur
Insister : la réponse JSON est le "miroir" de la requête. C'est l'idée clé à retenir.
GraphQL n'est PAS une base de données ni un remplaçant de SQL : c'est une couche API.
-->

---

## 2. Le schéma : le contrat de l'API

Écrit en **SDL** dans `src/main/resources/graphql/*.graphqls`

```graphql
"Patient connu de l'établissement."
type Patient implements Noeud {
  id: ID!                 # ! = jamais null
  ipp: IPP!               # scalaire personnalisé
  nomNaissance: String!
  age: Int!               # champ calculé
  sejours: [Sejour!]!     # relation → c'est un GRAPHE
}

type Query {
  patient(id: ID!): Patient            # null si inexistant
  patients(filtre: FiltrePatient, page: Int = 0, taille: Int = 20): PatientPage!
}
```

- **Typé** et **auto-documenté** (les `"descriptions"`)
- Approche **schema-first** : on écrit le contrat, puis le code

<!--
Notes orateur (2:30 → 5:30)
Le schéma est la source de vérité partagée front/back. Il est consultable sur /graphql/schema.
-->

---

## Les briques du schéma

| Brique | Rôle | Exemple du projet |
|---|---|---|
| `type` | Objet avec des champs | `Patient`, `Sejour`, `Document` |
| **Scalaire** | Valeur « feuille » | `Int`, `String`, `ID`… et **`Date`, `IPP`, `Telephone`** |
| `enum` | Liste fermée | `StatutDocument { BROUILLON VALIDE }` |
| `input` | Objet **en entrée** | `CreerPatientInput` |
| `interface` | Champs communs | `Noeud { id }` |
| `union` | « L'un ou l'autre » | `ResultatPatient = Patient \| ErreurMetier` |
| Directive | Annotation `@…` | `@deprecated`, **`@cout`** (maison) |

Trois points d'entrée : **`Query`** (lire) · **`Mutation`** (écrire) · **`Subscription`** (temps réel)

<!--
Notes orateur
Un scalaire personnalisé comme IPP (8 chiffres) est validé AVANT toute exécution : le code métier
ne reçoit jamais une valeur mal formée.
-->

---

## 3. Écrire des requêtes : les essentiels

```graphql
query DossierPatient($id: ID!, $avecDocs: Boolean = false) {   # nom + variables
  patient(id: $id) {
    ...Identite                                                # fragment
    lettres: documents(type: LETTRE_DE_LIAISON) { titre }      # alias + argument
    sejours { numeroSejour documents @include(if: $avecDocs) { titre } }  # directive
  }
}

fragment Identite on Patient { id ipp nomAffiche age }
```

```json
{ "id": 1, "avecDocs": true }
```

- **Variables** : les valeurs dynamiques passent à part (jamais de concaténation de chaînes)
- **Alias** : le même champ deux fois avec des arguments différents
- **Fragments** : réutiliser une sélection

<!--
Notes orateur (5:30 → 8:30)
Toutes ces requêtes existent dans requetes/requetes.http (q01 à q17) : à jouer en autonomie.
-->

---

## Polymorphisme et mutations

**Interface / union → fragments inline + `__typename`**

```graphql
query { recherche(texte: "cardi") {
    __typename
    ... on Patient  { nomAffiche }
    ... on Sejour   { numeroSejour motif }
} }
```

**Mutation : un verbe métier, un `input`, un résultat typé**

```graphql
mutation { admettrePatient(entree: { patientId: 13, type: HOSPITALISATION_COMPLETE, ufCode: "1202" }) {
    __typename
    ... on Sejour       { numeroSejour statut }
    ... on ErreurMetier { code message }
} }
```

Plusieurs mutations dans une requête ⇒ exécutées **en série**, dans l'ordre

<!--
Notes orateur
Nommer les mutations avec le vocabulaire métier (admettre, muter, sortir) plutôt qu'en CRUD (updateSejour).
-->

---

## 4. Côté serveur : Spring for GraphQL

Chaque champ a un **DataFetcher** (un « résolveur ») — on les écrit avec des annotations :

```java
@Controller
public class PatientController {

    @QueryMapping                              // Query.patient
    public Patient patient(@Argument Long id) { return patientService.trouver(id).orElse(null); }

    @SchemaMapping(typeName = "Patient")       // Patient.age (champ calculé)
    public int age(Patient patient) { return patient.age(LocalDate.now(horloge)); }
}
```

- Champs simples (`nom`, `prenom`) : lus **automatiquement** sur l'objet Java
- On n'écrit du code que pour les **points d'entrée**, les **champs calculés**, les **relations**
- Le **Repository** ne change pas : le DataFetcher l'appelle via le service, comme un contrôleur REST

<!--
Notes orateur (8:30 → 11:30)
Analogie : DataFetcher ≈ méthode de @RestController, mais à la granularité d'un CHAMP.
-->

---

## Le piège n°1 : le problème N+1

```graphql
patients(taille: 12) { contenu { nomAffiche  sejourEnCours { numeroSejour } } }
```

| Implémentation | Requêtes SQL |
|---|---|
| `@SchemaMapping` → appelé **par patient** | 1 + **12** |
| `@BatchMapping` → appelé **une fois** pour les 12 | 1 + **1** (`WHERE patient_id IN (...)`) |

```java
@BatchMapping(typeName = "Patient", field = "sejours")
public Map<Patient, List<Sejour>> sejoursDuPatient(List<Patient> patients) { ... }
```

`@BatchMapping` = un **DataLoader** : il **regroupe** les demandes d'un même niveau et les charge en lot

<!--
Notes orateur
Démo possible : requête q24, comparer les logs SQL (activés en DEBUG dans application.yml).
Règle simple : toute relation entre entités → @BatchMapping.
-->

---

## Comment les champs sont-ils « filtrés » ?

1. **Analyse** du texte de la requête
2. **Validation** contre le schéma (champs, types, scalaires, profondeur, coût) → rien ne s'exécute si erreur
3. **Exécution** : seuls les DataFetchers des **champs demandés** sont appelés
4. **Réponse** : construite **à partir de la sélection**, pas de l'objet Java

- Pas de `sejours` demandé ⇒ aucune requête SQL pour les séjours
- Attention : le `SELECT` du repository charge toujours l'objet complet — le gain porte sur les **relations** et **champs calculés**

<!--
Notes orateur
C'est la différence avec Jackson en REST qui sérialise tout le DTO.
-->

---

## 5. Les erreurs : deux familles

```json
{ "data": { "patient": { "prenom": "Jean" }, "recherche": null },
  "errors": [ { "message": "...", "path": ["recherche"] } ] }
```

- Réponse HTTP **200** presque toujours ; **réponse partielle** possible (`data` + `errors`)
- **Erreurs techniques / de requête** → tableau `errors`
- **Erreurs métier attendues** (choix du projet) → **dans les données**, via l'union `Resultat…`

```graphql
... on ErreurMetier { code message details { champ message } }   # toujours le sélectionner !
```

<!--
Notes orateur (11:30 → 13:30)
Piège m21 : sans ce fragment, une erreur métier arrive comme un objet vide {}.
Le client s'appuie sur `code` (enum CodeErreur), stable et traduisible, pas sur le message.
-->

---

## Sécurité : le client écrit les requêtes… donc on borne

| Risque | Parade dans le projet |
|---|---|
| Requête infiniment imbriquée (patient → séjour → patient…) | **Profondeur max 20** |
| Requête énorme (listes × listes) | **Coût max 5000** via la directive **`@cout`** |
| Liste non bornée | **Pagination** obligatoire, taille ∈ [1, 100] |
| Données mal formées | **Scalaires** validés avant exécution |

```graphql
documents(...): DocumentPage! @cout(multiplicateur: "taille")
```

→ requête refusée **avant** exécution

<!--
Notes orateur
En production, on désactive souvent aussi l'introspection et on peut n'accepter que des requêtes persistées.
-->

---

## GraphQL ou REST ?

| GraphQL brille quand… | REST reste plus simple quand… |
|---|---|
| Plusieurs clients (web, mobile) aux besoins différents | API simple, CRUD, peu de relations |
| Données très reliées (un graphe) | Cache HTTP important (GET + CDN) |
| Écrans qui agrègent beaucoup de sources | Téléchargement de fichiers |
| Besoin d'un contrat typé et auto-documenté | Équipe peu familière, délai court |

GraphQL ne remplace pas tout : c'est **un outil de plus**.

---

## 6. Démo (2 min)

1. Démarrer : `mvn spring-boot:run` (PostgreSQL sur `localhost:5432`)
2. Ouvrir **GraphiQL** : http://localhost:8080/graphiql — autocomplétion grâce à l'introspection
3. Jouer :
   - **q20** dossier patient complet (un aller-retour)
   - **q24** N+1 vs batch → regarder les logs SQL
   - **m01** création de patient → union `ResultatPatient`
   - **e07** requête trop coûteuse → refusée

<!--
Notes orateur (13:30 → 15:00)
Si pas de temps : montrer seulement q20 et q24.
-->

---

## À retenir

1. **Un endpoint**, un **schéma typé**, le **client choisit** les champs
2. La réponse a **la forme de la requête**
3. Côté Spring : `@QueryMapping`, `@SchemaMapping`, **`@BatchMapping`** contre le N+1
4. Erreurs métier **dans les données**, erreurs techniques dans `errors`
5. **Borner** : profondeur, coût, pagination

### Pour aller plus loin
- `docs/lexique-graphql.md` — le vocabulaire
- `docs/schema-graphql.md` · `docs/schema-bdd.md` — diagrammes du schéma et de la base
- `docs/dataloader-datafetcher.md` — DataFetcher, DataLoader, Repository
- `requetes/requetes.http` + `requetes/ANALYSE.md` — 60 requêtes commentées
- `TUTORIEL.md` · https://graphql.org/learn/

**Questions ?**
