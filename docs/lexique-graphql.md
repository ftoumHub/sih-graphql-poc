# Lexique GraphQL

Vocabulaire à connaître pour travailler avec GraphQL, et plus précisément avec **Spring for GraphQL**
tel qu'il est utilisé dans ce POC. Chaque terme est illustré, quand c'est possible, par un exemple
tiré du projet (schéma dans `src/main/resources/graphql/`, code dans `src/main/java/fr/sih/poc/api/graphql/`,
requêtes dans `requetes/requetes.http`).

Légende : 🟦 notion standard GraphQL · 🟩 spécifique à Spring for GraphQL / graphql-java · 🟧 propre à ce projet

---

## 1. Vue d'ensemble

GraphQL est un **langage de requête pour API**. Le serveur publie un **schéma** typé qui décrit tout
ce qu'on peut lire et modifier. Le client envoie une requête qui décrit **exactement** la forme
des données voulues, et la réponse JSON reprend cette forme.

```graphql
query {                    # opération
  patient(id: 1) {         # champ racine + argument
    nomAffiche             # champs sélectionnés
    sejours { numeroSejour }
  }
}
```

```json
{ "data": { "patient": { "nomAffiche": "...", "sejours": [ { "numeroSejour": "..." } ] } } }
```

Différences principales avec REST :

| REST | GraphQL |
|---|---|
| Une URL par ressource (`/patients/1`, `/patients/1/sejours`) | **Un seul point d'entrée** (`POST /graphql`) |
| Le serveur décide de la forme de la réponse | Le client choisit les champs |
| Plusieurs allers-retours pour assembler un écran | Une requête suffit (q37) |
| Codes HTTP pour les erreurs | Presque toujours HTTP 200, erreurs dans `errors` ou dans les données |
| Versionnement (`/v2/...`) | Évolution du schéma + `@deprecated` |

---

## 2. Le schéma (SDL)

**🟦 Schéma / SDL** (*Schema Definition Language*)
Contrat de l'API, écrit dans des fichiers `.graphqls`. Spring charge et fusionne tous les fichiers de
`classpath:graphql/` (`commun`, `patient`, `sejour`, `document`). Il est consultable sur
`http://localhost:8080/graphql/schema`.

**🟦 Approche *schema-first***
On écrit d'abord le schéma, puis le code Java qui l'implémente. C'est le choix de Spring for GraphQL
et de ce projet. L'approche inverse (*code-first*) génère le schéma à partir du code.

**🟦 Type objet** (`type`)
Ensemble de champs typés : `type Patient { id: ID! nomNaissance: String! ... }`.

**🟦 Champ** (*field*)
Propriété d'un type. Un champ peut renvoyer un scalaire, un objet ou une liste, et il peut prendre
des **arguments**.

**🟦 Types racines** : `Query`, `Mutation`, `Subscription`
Points d'entrée du graphe : `Query` pour la lecture, `Mutation` pour l'écriture, `Subscription` pour
le temps réel.

**🟦 `extend type`**
Ajoute des champs à un type déclaré ailleurs. Chaque fichier de domaine fait
`extend type Query { ... }` au lieu d'avoir une seule racine énorme.

**🟦 Non-null** (`!`) et **listes** (`[ ]`)
- `String` : peut valoir `null`.
- `String!` : jamais `null`.
- `[Sejour!]!` : la liste n'est jamais `null` et ne contient jamais `null` (elle peut être vide).

Si un champ non-null produit une erreur, le `null` **remonte** jusqu'au premier parent nullable.
C'est pourquoi `recherche` est volontairement nullable (e01).

**🟦 Scalaire** (`scalar`)
Type « feuille », sans sous-champs. Les scalaires prédéfinis sont `Int`, `Float`, `String`, `Boolean`
et `ID` (un identifiant, sérialisé en chaîne).

**🟦🟧 Scalaire personnalisé**
Scalaire défini par l'application, avec ses propres règles de sérialisation et de validation.
Le projet en déclare quatre dans `commun.graphqls` :

| Scalaire | Format | Exemple |
|---|---|---|
| `Date` | `yyyy-MM-dd` | `1948-03-12` |
| `DateHeure` | `yyyy-MM-dd'T'HH:mm:ss`, heure locale | `2026-11-18T09:30:00` |
| `IPP` | exactement 8 chiffres | `00010001` |
| `Telephone` | 10 chiffres commençant par 0 | `0612345678` |

Leur implémentation (`scalaires/Scalaires.java`) est un `GraphQLScalarType` avec un **Coercing**,
qui définit la conversion dans les deux sens (valeur Java ↔ JSON). Ils sont enregistrés dans
`GraphQlConfiguration` via un `RuntimeWiringConfigurer`. Une valeur mal formée est rejetée **avant**
l'exécution (e04, e08).

**🟦 Énumération** (`enum`)
Liste fermée de valeurs : `enum StatutDocument { BROUILLON VALIDE }`. Par convention, les valeurs
sont en MAJUSCULES. Spring les convertit automatiquement en `enum` Java.

**🟦 Input** (`input`)
Type réservé aux **arguments**, utilisé pour passer un objet en entrée :
`creerPatient(entree: CreerPatientInput!)`. Un `input` ne peut pas contenir de champs objets ni
d'arguments. Dans le projet, chaque `input` a son équivalent Java (un *record*) dans `entrees/`.

**🟦 Interface** (`interface`)
Ensemble de champs communs à plusieurs types. Exemples : `Noeud` (implémentée par `Patient`,
`Sejour` et `Document`), `EvenementSih` (implémentée par `EvenementSejour` et `EvenementDocument`).

**🟦 Union** (`union`)
Un champ qui peut renvoyer un type **parmi plusieurs**, sans champ commun obligatoire. Le projet
s'en sert pour les résultats de mutations : `union ResultatPatient = Patient | ErreurMetier`.

**🟦 Description**
Texte entre `"..."` ou `"""..."""` placé avant un élément du schéma. Il est visible dans GraphiQL
et par introspection, et tient lieu de documentation de l'API.

**🟦 Directive**
Annotation préfixée par `@`, qui s'applique soit au schéma, soit à une requête.
- Directives de **requête** standard : `@include(if:)` et `@skip(if:)` (q12).
- Directives de **schéma** standard : `@deprecated(reason:)` (q17) et `@specifiedBy(url:)`.
- On peut déclarer ses propres directives avec `directive @nom(...) on ...`.

**🟧 `@cout`**
Directive **propre au projet**, déclarée dans `commun.graphqls`. Elle attribue un **coût** à un champ
pour limiter la complexité des requêtes et se protéger du déni de service :

```graphql
directive @cout(valeur: Int = 1, multiplicateur: String, cardinalite: Int) on FIELD_DEFINITION

documents(filtre: FiltreDocument, page: Int = 0, taille: Int = 20): DocumentPage! @cout(multiplicateur: "taille")
unitesFonctionnelles: [UniteFonctionnelle!]! @cout(cardinalite: 10)
contenu(longueurMax: Int): String @cout(valeur: 5)
```

Formule : `coût = (valeur + coût des sous-champs) × facteur`. Le facteur est la valeur de l'argument
désigné par `multiplicateur` (par exemple `taille`), sinon `cardinalite`, sinon 1. Le calcul est fait
par `securite/CalculateurCout.java`, branché sur la `MaxQueryComplexityInstrumentation` de graphql-java.
Au-delà de 5000 (`sih.graphql.cout-max`), la requête est refusée avant d'être exécutée (e07).

---

## 3. Écrire une requête (côté client)

**🟦 Opération**
Une requête GraphQL est une opération de type `query`, `mutation` ou `subscription`.

**🟦 Nom d'opération**
`query PatientParId { ... }`. Il est facultatif, mais recommandé pour les logs et le débogage.
On peut aussi utiliser la syntaxe raccourcie, sans mot-clé ni nom : `{ patient(id: 1) { id } }` (q04).

**🟦 Ensemble de sélection** (*selection set*)
Liste des champs demandés, entre accolades. Elle est **obligatoire** pour tout champ de type objet :
on ne peut pas demander « tout ».

**🟦 Argument**
Paramètre d'un champ : `patient(id: 1)`. N'importe quel champ peut en avoir, même un champ imbriqué :
`documents(type: LETTRE_DE_LIAISON)` (q25), `contenu(longueurMax: 50)` (q26).

**🟦 Variable**
Paramètre de l'opération, déclaré avec `$` et transmis à part au format JSON :

```graphql
query PatientParId($id: ID!) { patient(id: $id) { id } }
```
```json
{ "id": 1 }
```

Une variable peut avoir une **valeur par défaut** : `($taille: Int = 5)` (q07). Il vaut toujours
mieux passer les valeurs dynamiques en variables plutôt que de les concaténer dans la requête.

**🟦 Alias**
Renomme un champ dans la réponse, ce qui permet de demander plusieurs fois le même champ avec
des arguments différents : `lettres: documents(type: LETTRE_DE_LIAISON)` (q05, q25, m19).

**🟦 Fragment**
Sélection de champs nommée et réutilisable :

```graphql
fragment Erreur on ErreurMetier { code message details { champ message } }
... ...Erreur      # utilisation (« spread »)
```

**🟦 Fragment inline**
`... on Sejour { numeroSejour }`. Il sélectionne des champs **selon le type réel** de l'objet,
ce qui est indispensable avec les interfaces et les unions (q33, m01, s01).

**🟦 `__typename`**
Méta-champ disponible sur tous les types. Il renvoie le nom du type concret (`"Patient"`,
`"ErreurMetier"`, etc.) et sert à savoir quelle branche d'une union on a reçue (q13).

**🟦 Introspection**
Interrogation du schéma lui-même via `__schema` et `__type` (q14–q17). C'est ce que GraphiQL utilise
pour l'autocomplétion. On la désactive souvent en production.

**🟦 GraphiQL**
Interface web interactive pour écrire et tester des requêtes : `http://localhost:8080/graphiql`.

---

## 4. Exécution côté serveur

**🟦 Résolveur** (*resolver*) / **🟩 DataFetcher**
Fonction qui produit la valeur d'un champ. Dans graphql-java, on l'appelle `DataFetcher`. Par défaut,
un champ est lu sur l'objet parent (getter ou composant de record du même nom). On n'écrit
de résolveur que pour les champs calculés ou les relations.

**🟦 Objet source / parent**
Objet sur lequel on résout un champ. Pour `Patient.sejours`, la source est le `Patient`.

**🟩 `@Controller`**
Dans Spring for GraphQL, les résolveurs sont des méthodes de beans annotés `@Controller`
(`PatientController`, `SejourController`, etc.).

**🟩 `@QueryMapping`, `@MutationMapping`, `@SubscriptionMapping`**
Lient une méthode à un champ de `Query`, `Mutation` ou `Subscription`. Par défaut, le nom de la méthode
est celui du champ.

```java
@QueryMapping
public Patient patient(@Argument Long id) { ... }
```

**🟩 `@SchemaMapping`**
Annotation générale qui lie une méthode à **n'importe quel champ de n'importe quel type**.
`@QueryMapping` n'en est qu'un raccourci (`@SchemaMapping(typeName = "Query")`).

```java
@SchemaMapping(typeName = "Patient")          // champ "age" du type Patient
public int age(Patient patient) { ... }       // le 1er paramètre = l'objet source

@SchemaMapping(typeName = "Document", field = "contenu")
public String contenu(Document doc, @Argument Integer longueurMax) { ... }
```

Elle est appelée **une fois par objet source**. Sur une liste de 20 patients, cela fait 20 appels,
et 20 requêtes SQL si la méthode interroge la base : c'est le problème N+1.

**🟩 `@BatchMapping`**
Variante « par lot » de `@SchemaMapping`. La méthode reçoit **toutes les sources** d'un même niveau
et renvoie une `Map<source, valeur>`. On obtient une seule requête `WHERE id IN (...)`
(`RelationsController`).

```java
@BatchMapping(typeName = "Patient", field = "sejours")
public Map<Patient, List<Sejour>> sejoursDuPatient(List<Patient> patients) { ... }
```

Chaque source doit avoir une entrée dans la `Map`, sinon le champ vaut `null`.

**🟩 `@Argument` / `@Arguments`**
Injecte un argument GraphQL dans un paramètre de méthode, converti en type Java
(`Long`, `enum`, record d'input…). `@Arguments` injecte tous les arguments dans un seul objet.

**🟦 Problème N+1**
Charger une liste (1 requête), puis une relation pour chaque élément (N requêtes). C'est le piège
de performance classique en GraphQL. q24 le met en évidence : `sejourEnCours` (`@SchemaMapping`)
contre `sejours` (`@BatchMapping`), à comparer dans les logs SQL.

**🟦 DataLoader / batch loading**
Mécanisme qui regroupe les demandes individuelles d'un même niveau en un seul appel par lot,
avec un cache pour la durée de la requête. `@BatchMapping` enregistre un DataLoader automatiquement.

**🟦 Exécution des champs**
Les champs d'une `query` peuvent être résolus en parallèle. Les champs racines d'une `mutation`
sont exécutés **en série**, dans l'ordre (m19).

**🟩 Instrumentation**
Point d'extension de graphql-java, qui intervient à chaque étape (analyse, validation, exécution).
Le projet en déclare deux dans `GraphQlConfiguration` :
- `MaxQueryDepthInstrumentation` : profondeur maximale de 20 (e05) ;
- `MaxQueryComplexityInstrumentation` : coût maximal de 5000, calculé avec `@cout` (e07).

**🟩 `RuntimeWiringConfigurer`**
Bean qui permet de compléter le « câblage » du schéma : scalaires, DataFetchers manuels, résolution
des types abstraits.

**🟦 Résolution de type** (*type resolution*)
Pour une interface ou une union, le serveur doit déterminer le type concret de chaque objet.
Spring le déduit du nom de la classe Java (`Patient` → `Patient`).

---

## 5. Erreurs

**🟦 Réponse GraphQL**
Objet JSON qui contient `data` et, le cas échéant, `errors` (et parfois `extensions`). Une même
réponse peut contenir **à la fois** des données et des erreurs : c'est la **réponse partielle** (e01).

**🟦 Tableau `errors`**
Chaque entrée contient un `message`, un `path` (le champ concerné), des `locations` et des
`extensions` (`classification`, code…).

**🟦 Erreurs de validation**
Requête incorrecte vis-à-vis du schéma : champ inconnu (e02), variable manquante (e06), scalaire
invalide (e04). La requête est rejetée **sans être exécutée**, et `data` est absent ou `null`.

**🟧 Erreurs métier « en tant que données »**
Choix de conception du projet : les erreurs **attendues** (règle métier, validation des entrées)
ne passent pas par `errors`. Elles sont renvoyées dans l'union `Resultat…` sous forme d'objet
`ErreurMetier { code message details }`. Le client teste `__typename` et s'appuie sur `code`
(enum `CodeErreur`). Si la requête oublie le fragment `... on ErreurMetier`, elle reçoit
un objet vide (m21).

**🟩 `DataFetcherExceptionResolver`**
Composant Spring qui transforme une exception Java en erreur GraphQL. Dans le projet,
`erreurs/GestionnaireErreursGraphql.java` traduit les `ErreurMetierException` et masque le message
des erreurs inattendues.

---

## 6. Temps réel

**🟦 Subscription**
Abonnement : le client reste connecté et reçoit un message à chaque événement (s01, s02).

**🟩 Transport WebSocket / `graphql-transport-ws`**
Protocole utilisé pour les subscriptions, exposé sur `/graphql-ws`
(`spring.graphql.websocket.path`).

**🟩 `Flux`**
Une méthode `@SubscriptionMapping` renvoie un `Flux` (Reactor), et chaque élément émis est envoyé
au client (`EvenementsController`, `FluxEvenementsSih`).

---

## 7. Sécurité et performance

| Notion | Description | Dans le projet |
|---|---|---|
| **Profondeur de requête** | Nombre de niveaux d'imbrication. Les relations cycliques (patient → séjour → patient…) permettent des requêtes infinies | Limite 20 (e05) |
| **Complexité / coût** | Estimation de la charge d'une requête avant de l'exécuter | `@cout`, limite 5000 (e07) |
| **Pagination** | Ne jamais renvoyer une liste non bornée | `page`/`taille` + `InfoPage`, taille dans [1, 100] (q08, e03) |
| **Requêtes persistées** | Le client n'envoie qu'un identifiant de requête pré-enregistrée | Non utilisé |
| **Introspection en production** | Souvent désactivée pour ne pas exposer le schéma | Activée (POC) |

**🟦 Pagination par offset ou par curseur**
- Le projet utilise l'**offset** : `page` et `taille`.
- La pagination par **curseur** (spécification *Relay Connections* : `edges`, `node`, `pageInfo`,
  `after`, `first`) est plus robuste quand les données changent entre deux pages. Spring for GraphQL
  la prend en charge (`ScrollSubrange`).

---

## 8. Conventions de nommage du projet 🟧

- Champs en `camelCase`, types en `PascalCase`, valeurs d'énumération en `MAJUSCULES`.
- Suffixe `Input` pour les types d'entrée (`CreerPatientInput`).
- Préfixe `Resultat` pour les unions de retour des mutations (`ResultatSejour`).
- Mutations nommées avec un verbe métier (`admettrePatient`, `muterPatient`, `sortirPatient`),
  et non sur le modèle CRUD (`updateSejour`).
- Un seul argument `entree` par mutation, ce qui permet de faire évoluer l'input sans casser
  la signature.

---

## Pour aller plus loin

- Spécification GraphQL : https://spec.graphql.org/
- Apprendre GraphQL : https://graphql.org/learn/
- Spring for GraphQL : https://docs.spring.io/spring-graphql/reference/
- graphql-java : https://www.graphql-java.com/documentation/getting-started
- DataFetcher, DataLoader et Repository : [`dataloader-datafetcher.md`](dataloader-datafetcher.md)
- Catalogue de requêtes du projet : [`requetes/requetes.http`](../requetes/requetes.http) et son analyse
  [`requetes/ANALYSE.md`](../requetes/ANALYSE.md)
