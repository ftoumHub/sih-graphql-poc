# Analyse de la conception des requêtes (`requetes.http`)

Le fichier contient environ 60 requêtes au format GraphQL du client HTTP d'IntelliJ.
Elles sont classées par préfixe et vont progressivement du plus simple au plus avancé.
Cette analyse les croise avec le schéma (`src/main/resources/graphql/*.graphqls`) et les contrôleurs
(`src/main/java/fr/sih/poc/api/graphql/`).

## Organisation

| Préfixe | Thème | Ce qu'il montre |
|---|---|---|
| **q01–q17** | Bases du langage | variables, valeurs par défaut, alias, fragments, `@include`/`@skip`, `__typename`, introspection |
| **q20–q37** | Cas métier | navigation dans le graphe (patient ↔ séjour ↔ document), filtres en *input objects*, pagination, interface `Noeud`, statistiques, plusieurs racines dans une requête |
| **e01–e08** | Erreurs techniques | réponse partielle, validation du schéma, scalaires métier (`IPP`, `Date`), limites de profondeur et de coût |
| **m01–m21** | Mutations | un scénario à jouer **dans l'ordre** sur une base fraîche : création → admission → mutation → documents → sortie → annulation |
| **s01–s02** | Subscriptions | flux d'événements par WebSocket, filtré ou non |

## Les choix de conception à retenir

### 1. Deux sortes d'erreurs

- Les **erreurs métier attendues** sont renvoyées comme des données : chaque mutation renvoie une union
  `Resultat… = Entité | ErreurMetier`. Le client teste `__typename` et s'appuie sur `code`,
  une valeur stable de l'enum `CodeErreur`.
- Les **erreurs techniques** ou de requête (pagination invalide, recherche trop courte, profondeur, coût)
  arrivent dans le tableau `errors`. Le champ concerné vaut alors `null` (e01).
- Le piège est montré en m21 : si la requête ne sélectionne pas `... on ErreurMetier`, une erreur métier
  arrive sous la forme d'un **objet vide `{}`**. C'est pour cela que toutes les mutations réutilisent
  le fragment `...Erreur`.

### 2. Le problème N+1 face au chargement en lot (q24)

- `Patient.sejourEnCours` est résolu par un `@SchemaMapping`, donc une requête SQL **par patient**.
- `Patient.sejours` est résolu par un `@BatchMapping` (`RelationsController`), donc **une seule**
  requête pour toute la liste.
- Le SQL est affiché en DEBUG dans `application.yml`, ce qui permet de comparer les deux dans les logs.
- Toutes les relations entre agrégats (séjour→patient, document→séjour, UF→séjours…) passent par
  ce chargement en lot.

### 3. Seuls les champs demandés sont calculés

- `statistiques` : chaque indicateur est un `@SchemaMapping` distinct. Si vous n'en demandez qu'un,
  une seule requête SQL est exécutée (q36).
- `contenu(longueurMax:)` est un champ calculé qui prend un argument (q26), et un même champ peut
  porter des arguments différents grâce aux alias (q05, q25).

### 4. Protection contre les requêtes abusives (`GraphQlConfiguration`)

- Une profondeur maximale de 20 (e05).
- Un **coût** calculé avec la directive `@cout` du schéma : le coût est multiplié par `taille` pour
  les listes paginées et par `cardinalite` pour les listes non paginées, avec une limite de 5000 (e07).
  La requête est refusée **avant** d'être exécutée.

### 5. Des scalaires métier qui valident les entrées

`IPP` (8 chiffres), `Telephone`, `Date` et `DateHeure` sont vérifiés pendant la validation de la requête,
avant que le moindre code métier ne s'exécute (e04, e08).

### 6. Le polymorphisme

- `recherche` renvoie des `[Noeud]` : le client ajoute des fragments inline selon le type obtenu (q33, q34).
- Les subscriptions renvoient l'interface `EvenementSih`, implémentée par `EvenementSejour` et
  `EvenementDocument` (s01).

### 7. L'ordre des mutations

Plusieurs mutations envoyées dans une même requête s'exécutent **en série**, dans l'ordre où elles sont
écrites (m19). Les champs d'une query, eux, peuvent être résolus en parallèle.

## Pour le parcourir

1. Jouez q01 → q37 pour découvrir le langage et le modèle.
2. Jouez q24 en surveillant les logs SQL.
3. Faites e01 → e08.
4. Jouez le scénario m01 → m21 **d'un seul trait** : les ids codés en dur (13, 15, 24…) supposent
   une base neuve.
5. Ouvrez s01 dans un onglet avant de jouer les mutations pour voir passer les événements.
