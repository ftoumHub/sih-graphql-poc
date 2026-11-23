# DataFetcher, DataLoader et Repository

Ce document s'adresse aux développeurs habitués au CRUD/REST et au pattern Repository.
Il explique deux notions centrales de GraphQL, le **DataFetcher** et le **DataLoader**, leur lien avec
le **Repository**, puis la façon dont les champs sont « filtrés » dans les réponses GraphQL.

Voir aussi le [lexique GraphQL](lexique-graphql.md).

---

## En une phrase

| Notion | Rôle | Couche |
|---|---|---|
| **Repository** | Aller chercher les données **en base** | Persistance |
| **DataFetcher** | Fournir la valeur **d'un champ** du schéma GraphQL | API, l'équivalent d'une méthode de contrôleur REST |
| **DataLoader** | **Regrouper** les appels de plusieurs DataFetchers en un seul, pour éviter le N+1 | Optimisation, entre l'API et le repository |

Ces trois notions ne sont pas concurrentes, elles s'empilent. Le repository ne disparaît pas avec GraphQL :
il est appelé **par** les DataFetchers, directement ou via un DataLoader.

```
Requête GraphQL
   └─► moteur graphql-java (parcourt la requête champ par champ)
         └─► DataFetcher du champ            (@QueryMapping, @SchemaMapping…)
               ├─► Service ─► Repository ─► SQL            (cas simple)
               └─► DataLoader ─► Service ─► Repository ─► SQL IN (...)   (cas groupé)
```

---

## 1. Le DataFetcher : l'équivalent d'une méthode de contrôleur, mais par champ

En REST/CRUD, vous avez **un endpoint par ressource** :

```java
@GetMapping("/patients/{id}")
PatientDto get(@PathVariable Long id) { return mapper.toDto(repo.findById(id)); }
```

En GraphQL, il n'y a qu'un endpoint, et chaque **champ** du schéma a son « mini-endpoint », appelé
DataFetcher (ou *resolver*). Dans Spring, vous ne les écrivez pas à la main : les annotations les créent
pour vous.

```java
@QueryMapping                         // DataFetcher du champ Query.patient
public Patient patient(@Argument Long id) { return patientService.trouver(id).orElse(null); }

@SchemaMapping(typeName = "Patient")  // DataFetcher du champ Patient.age
public int age(Patient patient) { return patient.age(LocalDate.now(horloge)); }
```

**Tous les champs ont un DataFetcher**, y compris `nom` ou `prenom`. Pour ces champs simples,
graphql-java utilise un DataFetcher par défaut (`PropertyDataFetcher`) qui se contente de lire le getter
ou le composant de record du même nom sur l'objet parent. Vous n'en écrivez un vous-même que pour :

- les **points d'entrée** (`Query`, `Mutation`) ;
- les **champs calculés** (`age`, `contenu(longueurMax)`) ;
- les **relations** qui demandent un autre accès à la base (`sejours`, `patient`…).

Le moteur exécute la requête **comme un arbre** : il appelle le DataFetcher racine, puis, pour chaque
objet obtenu, le DataFetcher de chaque sous-champ demandé, et ainsi de suite.

---

## 2. Le problème N+1, et pourquoi le DataLoader existe

Prenez la requête q24 de `requetes/requetes.http` :

```graphql
patients(taille: 12) { contenu { nomAffiche  sejourEnCours { numeroSejour } } }
```

Le moteur appelle `patients` une fois, puis le DataFetcher de `sejourEnCours` **une fois par patient**.
Avec un `@SchemaMapping` qui appelle le repository, on obtient :

```sql
SELECT ... FROM patient LIMIT 12;                         -- 1
SELECT ... FROM sejour WHERE patient_id = ? AND en cours; -- ×12  (enCoursDuPatient)
```

Soit 1 + N requêtes. En REST, on évite le problème en écrivant une jointure ou un DTO dédié, parce qu'on
connaît la forme de la réponse à l'avance. En GraphQL, c'est **le client** qui choisit la forme : le serveur
ne peut pas prévoir toutes les jointures possibles.

Le **DataLoader** règle ce problème. Au lieu de lancer la requête tout de suite, chaque DataFetcher dit
« j'aurai besoin des séjours du patient 3 ». Le DataLoader **accumule** les clés de tout un niveau de l'arbre,
puis fait **un seul appel** avec toutes ces clés :

```sql
SELECT ... FROM sejour WHERE patient_id IN (1,2,3,...,12);   -- 1 seule requête (parPatientIds)
```

Il redistribue ensuite les résultats à chaque patient. Il garde aussi un **cache pour la durée de la requête** :
la même clé n'est chargée qu'une fois.

Dans Spring, `@BatchMapping` crée et enregistre le DataLoader pour vous (`RelationsController`) :

```java
@BatchMapping(typeName = "Patient", field = "sejours")
public Map<Patient, List<Sejour>> sejoursDuPatient(List<Patient> patients) {
    // reçoit les 12 patients d'un coup → 1 appel repository → Map patient -> séjours
}
```

C'est exactement ce que montre q24 dans le projet : `sejourEnCours` est en `@SchemaMapping` (N+1, visible
dans les logs SQL) et `sejours` en `@BatchMapping` (1 requête).

---

## 3. Similarités et différences avec le Repository

| | Repository | DataFetcher | DataLoader |
|---|---|---|---|
| **Question posée** | « Donne-moi ces lignes en base » | « Quelle est la valeur de ce champ pour cet objet ? » | « Donne-moi les valeurs pour toutes ces clés d'un coup » |
| **Granularité** | Une entité, un agrégat | **Un champ** d'un type | Un lot de clés |
| **Sait-il qu'on fait du GraphQL ?** | Non | Oui (arguments, objet source, contexte) | Non, c'est un utilitaire générique |
| **Équivalent CRUD** | Le même | La méthode du `@RestController` | Un `findAllByIdIn(ids)` appelé au bon moment |
| **Appelé par** | Service / DataLoader | Moteur GraphQL | DataFetcher |

**Similarités**

- Le DataLoader ressemble à un repository « par lot » (`findAllById(Collection)`), avec un cache, comme
  le cache de premier niveau de JPA.
- Le DataFetcher joue le même rôle qu'un contrôleur : c'est une couche fine qui traduit l'appel API en appel
  au service.

**Différences**

- En CRUD, **un appel HTTP correspond à un appel de contrôleur**. En GraphQL, **un appel HTTP déclenche
  des dizaines de DataFetchers**, un par champ et par objet.
- En CRUD, c'est le développeur du serveur qui optimise les requêtes pour un endpoint connu. En GraphQL,
  il faut que l'optimisation **tienne quelle que soit la requête** : c'est pour cela que le DataLoader est
  presque indispensable.
- Le repository garde la même forme. Il a seulement besoin de méthodes « par lot »
  (`parPatientIds(Collection)`, `enCoursParUf(Collection)`), là où le CRUD se contente souvent de `findById`.

---

## 4. Comment les champs sont-ils « filtrés » dans la réponse ?

Le serveur ne filtre pas a posteriori un gros objet. Il ne **construit que ce qui est demandé**.
Trois mécanismes jouent :

### a) Le moteur ne parcourt que la sélection

La requête est d'abord analysée puis validée contre le schéma. Ensuite le moteur parcourt **l'ensemble
de sélection** et n'appelle le DataFetcher que des champs présents dans la requête. Si vous ne demandez pas
`sejours`, `sejoursDuPatient` n'est **jamais appelé**, et aucune requête SQL n'est lancée pour les séjours.

### b) La réponse est construite à partir de la sélection, pas de l'objet Java

Votre `Patient` Java peut avoir 15 attributs. Si la requête demande `{ id prenom }`, le moteur crée une `Map`
qui ne contient que `id` et `prenom`, dans l'ordre de la requête, puis la sérialise. Les autres attributs
ne sont jamais lus. C'est l'opposé du CRUD, où Jackson sérialise **tout** le DTO, ce qui vous oblige
à multiplier les DTO ou à utiliser `@JsonView`.

### c) Ce que ce mécanisme ne fait pas : réduire le `SELECT` en base

Par défaut, le repository charge l'**objet complet**. Un `{ id }` sur un patient fait quand même un
`SELECT *` sur la table `patient`. Le gain se fait au niveau des **relations et champs calculés**
(les DataFetchers non appelés), pas des colonnes. Si on veut aller plus loin :

- découper les champs coûteux en DataFetchers séparés. Le projet le fait pour `statistiques` : chaque
  indicateur a son `@SchemaMapping` et sa requête SQL, donc demander un seul indicateur ne lance qu'une
  requête (q36) ;
- lire la sélection demandée via `DataFetchingFieldSelectionSet`, un paramètre injectable dans la méthode,
  par exemple pour ne faire une jointure que si `sejours` est demandé. Le projet ne s'en sert pas :
  c'est une optimisation avancée.

### Résumé du cycle de vie d'une requête

1. **Analyse** du texte de la requête.
2. **Validation** contre le schéma : champs existants, types, scalaires (`IPP`), profondeur, coût (`@cout`).
   En cas d'erreur, rien n'est exécuté.
3. **Exécution** : le moteur parcourt l'arbre et appelle les DataFetchers des seuls champs demandés.
   Les DataLoaders regroupent les appels niveau par niveau.
4. **Construction de la réponse** : un JSON qui reprend **exactement** la forme de la sélection,
   plus `errors` s'il y a lieu.
