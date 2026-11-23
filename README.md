# POC GraphQL – Système d'information hospitalier

POC pédagogique **Spring Boot 3.5 / Spring for GraphQL / Java 17 / Maven** sur un mini SIH :
patients, séjours (et mouvements), documents médicaux, unités fonctionnelles.

➡️ **Le tutoriel complet est dans [TUTORIEL.md](TUTORIEL.md).**

📖 Vocabulaire GraphQL / Spring for GraphQL (`@SchemaMapping`, `@cout`, scalaires…) : [docs/lexique-graphql.md](docs/lexique-graphql.md).

🔄 DataFetcher, DataLoader et Repository (N+1, batch loading, filtrage des champs) : [docs/dataloader-datafetcher.md](docs/dataloader-datafetcher.md).

🎤 Présentation de 15 minutes pour développeurs débutants : [docs/presentation-graphql.md](docs/presentation-graphql.md) — avec ses [notes pour l'orateur](docs/presentation-graphql-notes-orateur.md).

🗺️ Diagrammes : [schéma GraphQL](docs/schema-graphql.md) · [base de données](docs/schema-bdd.md).

## Démarrage rapide

```bash
mvn spring-boot:run          # démarre PostgreSQL (docker compose), joue Liquibase, lance l'API
# ou
mvn spring-boot:test-run     # même chose avec un PostgreSQL Testcontainers jetable

mvn verify                   # tests (Docker requis pour Testcontainers)
```

- GraphiQL : http://localhost:8080/graphiql
- Endpoint : `POST http://localhost:8080/graphql`
- Subscriptions : `ws://localhost:8080/graphql-ws`
- Schéma : http://localhost:8080/graphql/schema

## Stack

| Brique | Rôle |
|---|---|
| Spring for GraphQL | Contrôleurs annotés, `@BatchMapping`, subscriptions, GraphiQL |
| Spring Data JDBC | Persistance des agrégats (records immuables) |
| Liquibase | Schéma + jeu de démonstration (contexte `demo`) |
| PostgreSQL 16 | Base de données |
| Vavr | `Either<ErreurMetier, T>` pour les erreurs métier |
| Bean Validation | Validation des inputs de mutation |
| Reactor | Bus d'événements des subscriptions |
| Testcontainers + GraphQlTester | Tests d'intégration |

## Contenu

```
src/main/resources/graphql/          schéma GraphQL documenté (4 fichiers)
src/main/resources/db/changelog/     migrations Liquibase (SQL formaté)
src/main/java/fr/sih/poc/
  domaine/        agrégats et règles métier (Java pur + Vavr)
  application/    cas d'usage (pipelines d'Either, transactions)
  infrastructure/ Spring Data JDBC, JdbcClient, bus Reactor
  api/graphql/    contrôleurs, scalaires, erreurs, calcul de coût
  config/         câblage explicite des beans
src/test/resources/graphql-test/     catalogue des requêtes (partagé tests / GraphiQL)
requetes/requetes.http               les mêmes requêtes pour le client HTTP d'IntelliJ
requetes/curl-exemples.sh            appels bruts avec curl
requetes/ANALYSE.md                  analyse de la conception des requêtes
docs/lexique-graphql.md              lexique GraphQL / Spring for GraphQL
docs/dataloader-datafetcher.md       DataFetcher, DataLoader et Repository
docs/presentation-graphql.md         présentation de 15 min pour débutants
docs/presentation-graphql-notes-orateur.md  notes pédagogiques pour l'orateur
docs/schema-graphql.md               diagrammes du schéma GraphQL (Mermaid)
docs/schema-bdd.md                   diagramme de la base de données (Mermaid)
```

## Statut

Le SQL Liquibase a été exécuté sur PostgreSQL 16, et le schéma ainsi que toutes les requêtes du catalogue
ont été validés avec graphql-js. Le code Java de production a été compilé contre des interfaces simulées des
bibliothèques. **Le build Maven complet n'a pas encore été exécuté** : lance `mvn verify` en premier.
