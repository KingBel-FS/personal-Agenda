#!/bin/bash
# Script de déploiement — Personal Agenda
# Usage : ./deploy.sh

set -e

echo "==> Build et démarrage des services..."
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

echo "==> Services démarrés :"
docker compose -f docker-compose.prod.yml ps
