# SmartCampus


Titre : SmartCampus, plateforme de services du campus

## Fonctionnalités (Sprint 1)
- Connexion des étudiants avec e-mail institutionnel et mot de passe (SC-4)
- Création d'espaces par un administrateur (SC-16) via `POST /api/espaces`. Le code est unique et
  l'espace est créé automatiquement avec le statut `ACTIVE`.

## Structure
- `backend/` : API Spring Boot (Java 17, Maven)
- `frontend/` : interface React (Vite)
- `docs/` : cahier des charges et diagrammes
- `docker-compose.yml` : base PostgreSQL

## Suivi du travail
Chaque branche, commit et Pull Request contient la clé Jira du ticket (ex. `SC-11`).

## CI/CD

Continuous integration is managed by Jenkins. 