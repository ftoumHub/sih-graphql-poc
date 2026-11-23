---
name: lancer-le-projet
description: Démarre le POC SIH GraphQL de bout en bout — PostgreSQL via docker compose, puis l'application Spring Boot (mvn spring-boot:run), puis ouvre Chrome sur GraphiQL (http://localhost:8080/graphiql). À utiliser quand on demande de lancer, démarrer ou ouvrir le projet / l'application / GraphiQL.
---

# Lancer le projet

POC Spring Boot 3.5 / Spring for GraphQL / Java 17 / Maven adossé à PostgreSQL 16.

| Élément | Valeur |
|---|---|
| Base | service `postgres` de `docker-compose.yml` — port 5432, base/utilisateur/mot de passe `sih` |
| Application | `mvn spring-boot:run` (Liquibase joue le schéma + le jeu de démo au démarrage) |
| URL de démarrage | http://localhost:8080/graphiql |
| Autres URLs | `POST /graphql`, `ws://localhost:8080/graphql-ws`, `/graphql/schema` |

## Procédure

Lance le script fourni, en tâche de fond Bash (`run_in_background: true`), car le démarrage Maven peut prendre plusieurs minutes au premier lancement :

```bash
bash .claude/skills/lancer-le-projet/scripts/lancer.sh
```

Si l'utilisateur précise une autre page (ex. le schéma), passe-la en argument :
`bash .claude/skills/lancer-le-projet/scripts/lancer.sh http://localhost:8080/graphql/schema`

Le script enchaîne :

1. **Docker Compose** — détecte `docker compose` (v2) ou, à défaut, `docker-compose` (v1, seul présent sur ce poste WSL), vérifie que le démon Docker répond, fait `up -d`, puis attend `pg_isready`.
2. **Spring Boot** — si l'URL répond déjà, ne relance rien. Sinon lance `mvn -q spring-boot:run` en arrière-plan avec `--spring.docker.compose.enabled=false` (la base est déjà démarrée à l'étape 1 ; ça évite que le support compose intégré de Spring Boot crée un second projet compose qui entrerait en conflit sur le port 5432). Logs : `target/spring-boot-run.log`, PID : `target/spring-boot-run.pid`. Attend jusqu'à 300 s que l'URL réponde.
3. **Chrome** — ouvre l'URL avec le Chrome Windows (`/mnt/c/Program Files/Google/Chrome/Application/chrome.exe`) depuis WSL, avec repli sur `cmd.exe /c start chrome`, `google-chrome`, `open -a` (macOS) ou `xdg-open`.

Une fois le script terminé, indique à l'utilisateur les URLs et la commande d'arrêt affichées en fin de sortie.

## En cas d'échec

- **Démon Docker absent** : demander de démarrer Docker Desktop (intégration WSL activée), puis relancer.
- **Port 5432 déjà pris** (autre PostgreSQL ou ancien conteneur) : `docker ps` pour identifier le conteneur ; ne pas le supprimer sans l'accord de l'utilisateur.
- **Port 8080 déjà pris** par autre chose que l'appli : le signaler, ne pas tuer le processus sans accord.
- **Spring Boot s'arrête** : le script affiche la fin de `target/spring-boot-run.log` ; lire l'erreur (compilation, Liquibase, connexion BDD) et la diagnostiquer. Le README précise que le build Maven complet n'a pas encore été validé : une erreur de compilation est possible.

## Arrêt

```bash
kill $(cat target/spring-boot-run.pid)
docker-compose stop        # ou : docker compose stop
```

Les données sont conservées dans le volume `sih-data` ; `down -v` les efface (demander confirmation avant).
