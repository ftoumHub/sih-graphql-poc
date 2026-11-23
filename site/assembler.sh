#!/usr/bin/env bash
# Rassemble la documentation éparse du dépôt (README, TUTORIEL, docs/, requetes/ANALYSE.md)
# dans .site-src/ en conservant l'arborescence, pour que les liens relatifs restent valides.
# Utilisé par le workflow GitHub Pages et pour la prévisualisation locale.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

rm -rf .site-src
mkdir -p .site-src/requetes
cp README.md .site-src/index.md
cp TUTORIEL.md .site-src/
cp -r docs .site-src/docs
cp requetes/ANALYSE.md .site-src/requetes/
echo "Sources du site assemblées dans .site-src/"
