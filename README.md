# Application de Synchronisation de Dossiers

Application complète de synchronisation de dossiers entre disques (Windows/Linux) avec interface web Angular et backend Spring Boot.

## Fonctionnalités

- ✅ **Sauvegarde complète** du disque source vers destination
- ✅ **Synchronisation incrémentale** automatique (toutes les 2h par défaut, configurable)
- ✅ **Détection intelligente des changements** (date de modification + checksum SHA-256 optionnel)
- ✅ **Interface web Angular** pour configuration et monitoring
- ✅ **Logs détaillés** avec statistiques et historique
- ✅ **Planification automatique** avec Spring Scheduler
- ✅ **Multi-tâches** : gérer plusieurs synchronisations simultanées
- ✅ **Compatible Windows et Linux**

## Architecture

```
.
├── backend/                    # Spring Boot API
│   ├── src/main/java/com/sync/app/
│   │   ├── entity/            # Entités JPA
│   │   ├── repository/        # Repositories
│   │   ├── service/           # Logique métier
│   │   ├── controller/        # REST Controllers
│   │   └── dto/               # Data Transfer Objects
│   └── src/main/resources/
│       └── application.yml    # Configuration
│
└── frontend/                   # Angular UI
    ├── src/app/
    │   ├── components/        # Composants UI
    │   ├── services/          # Services HTTP
    │   └── models/            # Interfaces TypeScript
    └── package.json
```

## Installation et Démarrage

### Prérequis

- Java 17+
- Maven 3.6+
- Node.js 18+
- npm 9+

### Backend (Spring Boot)

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Le serveur démarre sur http://localhost:8080

**Console H2** (base de données): http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/syncdb`
- Username: `sa`
- Password: (vide)

### Frontend (Angular)

```bash
cd frontend
npm install
npm start
```

L'interface web est accessible sur http://localhost:4200

## Utilisation

### 1. Créer une tâche de synchronisation

- Cliquer sur "Nouvelle Tâche"
- Renseigner:
  - **Nom**: nom descriptif de la tâche
  - **Chemin source**: dossier à sauvegarder (ex: `C:\Users\Documents` ou `/home/user/docs`)
  - **Chemin destination**: dossier de sauvegarde (ex: `D:\Backup` ou `/mnt/backup`)
  - **Intervalle**: fréquence de synchronisation en minutes (120 = 2h)
  - **Utiliser checksum**: activer pour détection précise (SHA-256)

### 2. Gérer les tâches

- **Activer/Désactiver**: toggle pour contrôler l'exécution automatique
- **Synchroniser**: déclencher manuellement une synchronisation
- **Modifier**: éditer les paramètres
- **Supprimer**: supprimer la tâche et ses métadonnées
- **Logs**: consulter l'historique des exécutions

### 3. Consulter les logs

- Vue détaillée de chaque synchronisation:
  - Fichiers scannés/copiés/mis à jour/supprimés
  - Volume total transféré
  - Durée d'exécution
  - Statut (COMPLETED/FAILED/RUNNING)
  - Messages d'erreur éventuels

## API REST

### Tâches de synchronisation

```http
GET    /api/sync-tasks              # Liste toutes les tâches
POST   /api/sync-tasks              # Créer une tâche
GET    /api/sync-tasks/{id}         # Détails d'une tâche
PUT    /api/sync-tasks/{id}         # Modifier une tâche
DELETE /api/sync-tasks/{id}         # Supprimer une tâche
POST   /api/sync-tasks/{id}/toggle  # Activer/désactiver
POST   /api/sync-tasks/{id}/trigger # Déclencher manuellement
GET    /api/sync-tasks/{id}/status  # Statut de la synchronisation
```

### Logs

```http
GET /api/sync-logs?page=0&size=20              # Tous les logs (paginés)
GET /api/sync-logs/task/{taskId}?page=0&size=20 # Logs d'une tâche
GET /api/sync-logs/{id}                        # Détails d'un log
```

## Fonctionnement Technique

### Synchronisation Complète (première exécution)

1. Parcours récursif du dossier source
2. Copie de tous les fichiers vers la destination
3. Création des répertoires nécessaires
4. Enregistrement des métadonnées (chemin, taille, date, checksum)

### Synchronisation Incrémentale (exécutions suivantes)

1. Parcours du dossier source
2. Comparaison avec les métadonnées stockées:
   - Taille différente → copie
   - Date de modification différente → copie
   - Checksum différent (si activé) → copie
   - Identique → ignoré
3. Suppression des fichiers absents de la source
4. Mise à jour des métadonnées

### Planification Automatique

- Scheduler Spring vérifie toutes les minutes les tâches actives
- Exécution si `nextSyncTime` est dépassé
- Mise à jour automatique de `lastSyncTime` et `nextSyncTime`
- Exécution asynchrone pour ne pas bloquer le scheduler

## Configuration Avancée

### application.yml

```yaml
sync:
  default-interval: 7200000    # Intervalle par défaut (2h en ms)
  max-concurrent-syncs: 3      # Max syncs simultanées
  chunk-size: 8192             # Taille buffer copie (8KB)
```

### Base de données

Par défaut, H2 embarquée. Pour passer en production:

1. Ajouter le driver (PostgreSQL, MySQL, etc.) dans `pom.xml`
2. Modifier `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/syncdb
    username: user
    password: pass
  jpa:
    hibernate:
      ddl-auto: update
```

## Développement

### Tests

```bash
# Backend
cd backend
mvn test

# Frontend
cd frontend
npm test
```

### Build Production

```bash
# Backend (JAR exécutable)
cd backend
mvn clean package
java -jar target/folder-sync-1.0.0.jar

# Frontend
cd frontend
npm run build
# Fichiers générés dans dist/
```

## Sécurité

⚠️ **Important**: N'oubliez pas de:
- Configurer l'authentification pour usage en production
- Sécuriser la console H2 (désactiver en prod)
- Valider les chemins de fichiers pour éviter directory traversal
- Mettre en place HTTPS pour l'API

## Licence

MIT
