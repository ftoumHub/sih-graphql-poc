#!/usr/bin/env bash
# Lance le POC SIH GraphQL : PostgreSQL (docker compose) -> Spring Boot -> Chrome sur GraphiQL.
# Usage : lancer.sh [url]   (url par défaut : http://localhost:8080/graphiql)
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
URL="${1:-http://localhost:8080/graphiql}"
LOG="$RACINE/target/spring-boot-run.log"
PIDFILE="$RACINE/target/spring-boot-run.pid"
cd "$RACINE"
mkdir -p target

# --- 1. Docker Compose (v2 "docker compose" ou v1 "docker-compose") ---------
if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
else
  echo "ERREUR : ni 'docker compose' ni 'docker-compose' n'est disponible." >&2; exit 1
fi
docker info >/dev/null 2>&1 || { echo "ERREUR : le démon Docker ne répond pas (Docker Desktop démarré ?)." >&2; exit 1; }

echo "==> Démarrage de PostgreSQL avec : ${COMPOSE[*]} up -d"
"${COMPOSE[@]}" up -d

echo "==> Attente de PostgreSQL (pg_isready)..."
for i in $(seq 1 60); do
  if "${COMPOSE[@]}" exec -T postgres pg_isready -U sih -d sih >/dev/null 2>&1; then
    echo "    PostgreSQL prêt."; break
  fi
  [ "$i" -eq 60 ] && { echo "ERREUR : PostgreSQL pas prêt après 60 s." >&2; "${COMPOSE[@]}" logs --tail 30 postgres; exit 1; }
  sleep 1
done

# --- 2. Spring Boot -----------------------------------------------------------
if curl -fs -o /dev/null "$URL"; then
  echo "==> L'application répond déjà sur $URL : pas de relance."
else
  if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "==> Un 'mvn spring-boot:run' (PID $(cat "$PIDFILE")) tourne déjà, on attend qu'il réponde."
  else
    echo "==> Lancement de Spring Boot (logs : $LOG)"
    # Le support docker compose intégré à Spring Boot est désactivé : la base vient d'être
    # démarrée explicitement ci-dessus (évite un 2e projet compose sur le port 5432).
    nohup mvn -q spring-boot:run \
      -Dspring-boot.run.arguments=--spring.docker.compose.enabled=false \
      >"$LOG" 2>&1 &
    echo $! >"$PIDFILE"
  fi

  echo "==> Attente de $URL (max 300 s)..."
  for i in $(seq 1 300); do
    if curl -fs -o /dev/null "$URL"; then echo "    Application prête."; break; fi
    if ! kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
      echo "ERREUR : Spring Boot s'est arrêté. Fin du log :" >&2; tail -n 60 "$LOG" >&2; exit 1
    fi
    [ "$i" -eq 300 ] && { echo "ERREUR : pas de réponse après 300 s. Fin du log :" >&2; tail -n 60 "$LOG" >&2; exit 1; }
    sleep 1
  done
fi

# --- 3. Chrome ----------------------------------------------------------------
CHROME_WIN="/mnt/c/Program Files/Google/Chrome/Application/chrome.exe"
CHROME_WIN86="/mnt/c/Program Files (x86)/Google/Chrome/Application/chrome.exe"
echo "==> Ouverture de Chrome sur $URL"
if [ -x "$CHROME_WIN" ]; then
  "$CHROME_WIN" "$URL" >/dev/null 2>&1 &
elif [ -x "$CHROME_WIN86" ]; then
  "$CHROME_WIN86" "$URL" >/dev/null 2>&1 &
elif command -v cmd.exe >/dev/null 2>&1; then
  cmd.exe /c start chrome "$URL" >/dev/null 2>&1
elif command -v google-chrome >/dev/null 2>&1; then
  google-chrome "$URL" >/dev/null 2>&1 &
elif command -v open >/dev/null 2>&1; then
  open -a "Google Chrome" "$URL"
elif command -v xdg-open >/dev/null 2>&1; then
  xdg-open "$URL" >/dev/null 2>&1 &
else
  echo "Chrome introuvable : ouvre manuellement $URL"
fi

echo
echo "Projet lancé."
echo "  GraphiQL      : http://localhost:8080/graphiql"
echo "  Endpoint      : POST http://localhost:8080/graphql"
echo "  Schéma        : http://localhost:8080/graphql/schema"
echo "  Logs Spring   : $LOG"
echo "  Arrêt         : kill \$(cat $PIDFILE) ; ${COMPOSE[*]} stop"
