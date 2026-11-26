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

## Comptes de test (profil dev, données fictives)
Créés automatiquement au démarrage par `DevDataLoader`.

| Rôle | E-mail | Mot de passe |
|---|---|---|
| Administrateur | admin@ensakh.ma | admin1234 |
| Étudiant | test@usms.ac.ma | test1234 |
| Étudiant | sara.etudiant@usms.ac.ma | test1234 |
| Étudiant | ahmed.etudiant@usms.ac.ma | test1234 |
| Enseignant | enseignant@usms.ma | test1234 |
| Enseignant | nadia.enseignant@usms.ma | test1234 |

Espaces créés : un de chaque type (amphithéâtre, salle de cours, salle de réunion, bibliothèque, terrain, buvette).

## Suivi du travail
Chaque branche, commit et Pull Request contient la clé Jira du ticket (ex. `SC-11`).

## CI/CD

Continuous integration is managed by Jenkins. 