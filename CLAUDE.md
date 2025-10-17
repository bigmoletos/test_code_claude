# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Application de synchronisation de dossiers entre disques (Windows/Linux) avec interface web Angular et backend Spring Boot. L'application effectue des sauvegardes complètes et incrémentales automatiques basées sur la détection de modifications (date/checksum).

## Architecture

### Backend (Spring Boot)

**Structure des packages:**
- `entity/`: Entités JPA (SyncTask, SyncLog, FileMetadata)
- `repository/`: Repositories Spring Data JPA
- `service/`: Logique métier
  - `FileSyncService`: Gestion de la synchronisation de fichiers
  - `SyncSchedulerService`: Planification automatique des tâches
  - `SyncTaskService`: CRUD des tâches de synchronisation
- `controller/`: REST Controllers
- `dto/`: Data Transfer Objects
- `config/`: Configuration (CORS, etc.)

**Base de données:**
- H2 embarquée (fichier `./data/syncdb`)
- 3 tables principales: `sync_tasks`, `sync_logs`, `file_metadata`

**Logique de synchronisation:**
1. Parcours récursif du dossier source
2. Comparaison avec métadonnées stockées (date modification + checksum optionnel)
3. Copie/mise à jour des fichiers modifiés uniquement
4. Suppression des fichiers absents de la source
5. Sauvegarde des métadonnées pour prochaine synchro incrémentale

**Scheduler:**
- Tâche planifiée exécutée toutes les minutes
- Vérifie les tâches actives dont `nextSyncTime` est dépassé
- Exécution asynchrone pour ne pas bloquer le scheduler

### Frontend (Angular 17 Standalone)

**Structure:**
- `models/`: Interfaces TypeScript (SyncTask, SyncLog)
- `services/`: Services HTTP (SyncTaskService, SyncLogService)
- `components/`:
  - `task-list`: Liste des tâches avec actions (toggle, trigger, delete)
  - `task-form`: Formulaire création/édition de tâche
  - `log-list`: Historique des logs avec pagination

**Communication backend:**
- HttpClient pour appels REST
- Base URL: `http://localhost:8080/api`
- CORS configuré pour `http://localhost:4200`

**Gestion d'état:**
- Services injectés pour partage de données
- Rafraîchissement automatique des statuts de sync (polling 5s)
- Pagination côté serveur pour les logs

## Commandes

### Backend

```bash
# Depuis le dossier backend/
mvn clean install          # Compiler le projet
mvn spring-boot:run       # Démarrer l'application (port 8080)
mvn test                  # Exécuter les tests

# Console H2
# URL: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:file:./data/syncdb
# User: sa, Password: (vide)
```

### Frontend

```bash
# Depuis le dossier frontend/
npm install               # Installer les dépendances
npm start                 # Démarrer le serveur de dev (port 4200)
npm run build             # Build de production
```

## Endpoints API

### Tâches de synchronisation
- `GET /api/sync-tasks` - Liste toutes les tâches
- `GET /api/sync-tasks/{id}` - Détails d'une tâche
- `POST /api/sync-tasks` - Créer une tâche
- `PUT /api/sync-tasks/{id}` - Modifier une tâche
- `DELETE /api/sync-tasks/{id}` - Supprimer une tâche
- `POST /api/sync-tasks/{id}/toggle` - Activer/désactiver
- `POST /api/sync-tasks/{id}/trigger` - Déclencher manuellement
- `GET /api/sync-tasks/{id}/status` - Statut de la synchro

### Logs
- `GET /api/sync-logs?page=0&size=20` - Tous les logs (paginés)
- `GET /api/sync-logs/task/{taskId}?page=0&size=20` - Logs d'une tâche
- `GET /api/sync-logs/{id}` - Détails d'un log

## Configuration

### application.yml
- `sync.default-interval`: Intervalle par défaut (ms)
- `sync.max-concurrent-syncs`: Nombre max de syncs simultanées
- `sync.chunk-size`: Taille buffer pour copie fichiers

### Chemins Windows/Linux
L'application détecte automatiquement l'OS. Exemples de chemins valides:
- Windows: `C:\Users\Documents` ou `D:\Backup`
- Linux: `/home/user/documents` ou `/mnt/backup`

## Points importants

1. **Synchronisation thread-safe**: Utilisation de `ConcurrentHashMap.newKeySet()` pour tracker les syncs en cours

2. **Détection de modifications**:
   - Par défaut: date de modification + taille fichier
   - Optionnel: checksum SHA-256 (plus précis, plus lent)

3. **Métadonnées**: Stockées en base pour comparaisons incrémentales. Index sur `(sync_task_id, file_path)` pour performances.

4. **Logs détaillés**: Chaque exécution génère un log avec statistiques (fichiers copiés/mis à jour/supprimés/ignorés, volume, durée, erreurs)

5. **Gestion d'erreurs**: Les exceptions sont catchées et logguées. Le statut de la tâche passe à FAILED avec message d'erreur.

## Développement

Pour ajouter une nouvelle fonctionnalité de synchronisation:
1. Modifier `FileSyncService` pour la logique
2. Ajouter endpoint dans `SyncTaskController` si nécessaire
3. Mettre à jour le service Angular et l'UI correspondante
4. Tester avec différents types de fichiers et chemins

Pour modifier la planification:
1. Ajuster `@Scheduled(fixedRate = ...)` dans `SyncSchedulerService`
2. Modifier la logique de calcul de `nextSyncTime` dans `SyncTaskService`
