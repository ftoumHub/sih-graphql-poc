#!/usr/bin/env bash
# Appels GraphQL "bruts" : un POST JSON avec query, variables et (optionnellement) operationName.
set -euo pipefail
URL=${URL:-http://localhost:8080/graphql}

appel() {
  echo "### $1"
  curl -s -w '\n(HTTP %{http_code})\n\n' "$URL" -H 'Content-Type: application/json' -d "$2"
}

appel "Query avec variables" \
  '{"query":"query($id: ID!) { patient(id: $id) { prenom nomAffiche age } }","variables":{"id":1}}'

appel "Mutation réussie (union : Patient)" \
  '{"query":"mutation($e: CreerPatientInput!) { creerPatient(entree: $e) { __typename ... on Patient { id ipp } ... on ErreurMetier { code message } } }",
    "variables":{"e":{"ipp":"00030001","nomNaissance":"Curl","prenom":"Test","dateNaissance":"1990-01-01","sexe":"M"}}}'

appel "Erreur métier (union : ErreurMetier, pas de tableau errors)" \
  '{"query":"mutation { sortirPatient(entree: {sejourId: 12, modeSortie: DOMICILE}) { __typename ... on Sejour { statut } ... on ErreurMetier { code message } } }"}'

appel "Erreur de query (tableau errors, HTTP 200 quand même)" \
  '{"query":"{ patients(taille: 500) { infoPage { totalElements } } }"}'

appel "Requête trop coûteuse (rejetée avant exécution)" \
  '{"query":"{ patients(taille: 100) { contenu { sejours { documents { contenu } } } } }"}'

appel "Document à plusieurs opérations : operationName choisit laquelle exécuter" \
  '{"query":"query A { patient(id: 1) { prenom } } query B { patient(id: 2) { prenom } }","operationName":"B"}'

echo "### Schéma"
curl -s "${URL}/schema" | head -20
