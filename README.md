# Application de Synchronisation de Dossiers

Application complète de synchronisation de dossiers entre disques (Windows/Linux) avec interface web Angular et backend Spring Boot.

## 🚀 Démarrage Rapide

```bash
# 1. Compiler et lancer le backend
cd backend
mvn spring-boot:run

# Si Maven n'est pas installé, utilisez le wrapper (depuis la racine) :
# Windows PowerShell : .\mvnw.cmd -f backend/pom.xml spring-boot:run
# Linux/WSL : ./mvnw -f backend/pom.xml spring-boot:run

# 2. L'API est accessible sur http://localhost:8081
# Testez : curl http://localhost:8081/api/sync-tasks

# 3. (Optionnel) Lancer le frontend Angular
cd frontend
npm install
npm start
# Interface web sur http://localhost:4200
```

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

- Java 21+ (ou Java 17+ minimum)
- Maven 3.6+ (ou utiliser le wrapper `mvnw.cmd` inclus)
- Node.js 18+
- npm 9+

### Backend (Spring Boot)

#### Option 1 : Maven installé (si Maven est dans le PATH)
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

#### Option 2 : Wrapper Maven (sans installation Maven requise)

**Windows PowerShell** :
```powershell
.\mvnw.cmd -f backend/pom.xml clean install
.\mvnw.cmd -f backend/pom.xml spring-boot:run
```

**Linux/WSL** :
```bash
./mvnw -f backend/pom.xml clean install
./mvnw -f backend/pom.xml spring-boot:run
```

Le serveur démarre sur **http://localhost:8081**

**Console H2** (base de données): **http://localhost:8081/h2-console**
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

## 🔍 Vérification et Gestion des Services

### Vérifier si les services sont actifs

#### Windows PowerShell
```powershell
# Vérifier le backend (port 8081)
netstat -ano | findstr :8081

# Vérifier le frontend (port 4200)
netstat -ano | findstr :4200

# Résultat attendu : lignes affichées = service actif, rien = service arrêté
```

#### Linux/WSL
```bash
# Vérifier le backend (port 8081)
lsof -i :8081
# ou
ss -tlnp | grep 8081

# Vérifier le frontend (port 4200)
lsof -i :4200
# ou
ss -tlnp | grep 4200
```

#### Via le navigateur
- **Backend API** : `http://localhost:8081/api/sync-tasks` (doit retourner du JSON)
- **Console H2** : `http://localhost:8081/h2-console` (doit afficher l'interface)
- **Frontend** : `http://localhost:4200` (doit afficher l'interface Angular)

### Arrêter les services

#### Arrêter le Backend

**Windows PowerShell :**
```powershell
# Trouver le PID du processus sur le port 8081
netstat -ano | findstr :8081
# Note le PID (dernière colonne)

# Tuer le processus (remplacer <PID> par le numéro)
taskkill /PID <PID> /F

# Exemple si PID = 12345
taskkill /PID 12345 /F
```

**Linux/WSL :**
```bash
# Trouver et tuer le processus en une commande
lsof -ti:8081 | xargs kill -9

# Ou manuellement
lsof -i :8081  # Noter le PID
kill -9 <PID>  # Remplacer <PID>
```

**Via le terminal où il tourne :**
- Appuyer sur `Ctrl+C` dans le terminal où Maven est lancé

#### Arrêter le Frontend

**Windows PowerShell :**
```powershell
# Trouver le PID
netstat -ano | findstr :4200

# Tuer le processus
taskkill /PID <PID> /F
```

**Linux/WSL :**
```bash
# Trouver et tuer
lsof -ti:4200 | xargs kill -9
```

**Via le terminal où il tourne :**
- Appuyer sur `Ctrl+C` dans le terminal où npm est lancé

### Redémarrer les services

#### Redémarrer le Backend

**Windows PowerShell :**
```powershell
# 1. Arrêter (si actif)
netstat -ano | findstr :8081
taskkill /PID <PID> /F

# 2. Relancer
.\mvnw.cmd -f backend/pom.xml spring-boot:run
```

**Linux/WSL :**
```bash
# 1. Arrêter (si actif)
lsof -ti:8081 | xargs kill -9

# 2. Relancer
cd backend
mvn spring-boot:run
```

#### Redémarrer le Frontend

```bash
# 1. Arrêter (si actif)
# Windows PowerShell
netstat -ano | findstr :4200
taskkill /PID <PID> /F

# Linux/WSL
lsof -ti:4200 | xargs kill -9

# 2. Relancer (tous OS)
cd frontend
npm start
```

### Lancer les deux services simultanément

**Option 1 : Deux terminaux séparés (recommandé)**
```bash
# Terminal 1 - Backend
cd backend
mvn spring-boot:run

# Terminal 2 - Frontend
cd frontend
npm start
```

**Option 2 : En arrière-plan (Linux/WSL)**
```bash
# Lancer le backend en arrière-plan
cd backend
nohup mvn spring-boot:run > ../backend.log 2>&1 &

# Lancer le frontend en arrière-plan
cd ../frontend
nohup npm start > ../frontend.log 2>&1 &

# Voir les logs
tail -f ../backend.log
tail -f ../frontend.log

# Arrêter les services
pkill -f "spring-boot:run"
pkill -f "ng serve"
```

### Script PowerShell pour gérer les services (Windows)

Créer un fichier `manage-services.ps1` :
```powershell
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('start','stop','restart','status')]
    [string]$Action
)

function Get-ServiceStatus {
    Write-Host "`n=== État des Services ===" -ForegroundColor Cyan

    $backend = netstat -ano | findstr :8081
    $frontend = netstat -ano | findstr :4200

    if ($backend) {
        Write-Host "✅ Backend (8081) : ACTIF" -ForegroundColor Green
    } else {
        Write-Host "❌ Backend (8081) : ARRÊTÉ" -ForegroundColor Red
    }

    if ($frontend) {
        Write-Host "✅ Frontend (4200) : ACTIF" -ForegroundColor Green
    } else {
        Write-Host "❌ Frontend (4200) : ARRÊTÉ" -ForegroundColor Red
    }
}

function Stop-Services {
    Write-Host "`nArrêt des services..." -ForegroundColor Yellow

    # Arrêter backend
    $backendPid = (netstat -ano | findstr :8081 | Select-Object -First 1) -replace '\s+', ' ' -split ' ' | Select-Object -Last 1
    if ($backendPid) {
        taskkill /PID $backendPid /F
        Write-Host "Backend arrêté (PID: $backendPid)" -ForegroundColor Green
    }

    # Arrêter frontend
    $frontendPid = (netstat -ano | findstr :4200 | Select-Object -First 1) -replace '\s+', ' ' -split ' ' | Select-Object -Last 1
    if ($frontendPid) {
        taskkill /PID $frontendPid /F
        Write-Host "Frontend arrêté (PID: $frontendPid)" -ForegroundColor Green
    }
}

function Start-Services {
    Write-Host "`nDémarrage des services..." -ForegroundColor Yellow

    # Démarrer backend
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend; mvn spring-boot:run"
    Write-Host "Backend démarré sur port 8081" -ForegroundColor Green

    # Attendre un peu
    Start-Sleep -Seconds 3

    # Démarrer frontend
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; npm start"
    Write-Host "Frontend démarré sur port 4200" -ForegroundColor Green
}

switch ($Action) {
    'status' { Get-ServiceStatus }
    'stop' { Stop-Services; Get-ServiceStatus }
    'start' { Start-Services; Start-Sleep -Seconds 5; Get-ServiceStatus }
    'restart' { Stop-Services; Start-Sleep -Seconds 2; Start-Services; Start-Sleep -Seconds 5; Get-ServiceStatus }
}
```

**Utilisation du script :**
```powershell
# Vérifier l'état
.\manage-services.ps1 status

# Démarrer les services
.\manage-services.ps1 start

# Arrêter les services
.\manage-services.ps1 stop

# Redémarrer les services
.\manage-services.ps1 restart
```

### Script Bash pour gérer les services (Linux/WSL)

Créer un fichier `manage-services.sh` :
```bash
#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

function check_status() {
    echo -e "${CYAN}\n=== État des Services ===${NC}"

    if lsof -Pi :8081 -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${GREEN}✅ Backend (8081) : ACTIF${NC}"
    else
        echo -e "${RED}❌ Backend (8081) : ARRÊTÉ${NC}"
    fi

    if lsof -Pi :4200 -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${GREEN}✅ Frontend (4200) : ACTIF${NC}"
    else
        echo -e "${RED}❌ Frontend (4200) : ARRÊTÉ${NC}"
    fi
}

function stop_services() {
    echo -e "${YELLOW}\nArrêt des services...${NC}"

    # Arrêter backend
    if lsof -Pi :8081 -sTCP:LISTEN -t >/dev/null ; then
        lsof -ti:8081 | xargs kill -9
        echo -e "${GREEN}Backend arrêté${NC}"
    fi

    # Arrêter frontend
    if lsof -Pi :4200 -sTCP:LISTEN -t >/dev/null ; then
        lsof -ti:4200 | xargs kill -9
        echo -e "${GREEN}Frontend arrêté${NC}"
    fi
}

function start_services() {
    echo -e "${YELLOW}\nDémarrage des services...${NC}"

    # Démarrer backend
    cd backend
    nohup mvn spring-boot:run > ../backend.log 2>&1 &
    echo -e "${GREEN}Backend démarré sur port 8081${NC}"
    cd ..

    # Démarrer frontend
    cd frontend
    nohup npm start > ../frontend.log 2>&1 &
    echo -e "${GREEN}Frontend démarré sur port 4200${NC}"
    cd ..
}

case "$1" in
    status)
        check_status
        ;;
    stop)
        stop_services
        check_status
        ;;
    start)
        start_services
        sleep 5
        check_status
        ;;
    restart)
        stop_services
        sleep 2
        start_services
        sleep 5
        check_status
        ;;
    *)
        echo "Usage: $0 {status|start|stop|restart}"
        exit 1
esac
```

**Rendre le script exécutable et l'utiliser :**
```bash
chmod +x manage-services.sh

# Vérifier l'état
./manage-services.sh status

# Démarrer les services
./manage-services.sh start

# Arrêter les services
./manage-services.sh stop

# Redémarrer les services
./manage-services.sh restart
```

### Récapitulatif des Ports

| Service | Port | URL | Description |
|---------|------|-----|-------------|
| **Backend API** | 8081 | http://localhost:8081/api | REST API Spring Boot |
| **Console H2** | 8081 | http://localhost:8081/h2-console | Interface base de données |
| **Frontend** | 4200 | http://localhost:4200 | Interface Angular |

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

**Base URL**: `http://localhost:8081`

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

**Exemple d'utilisation**:
```bash
# Lister toutes les tâches
curl http://localhost:8081/api/sync-tasks

# Créer une nouvelle tâche
curl -X POST http://localhost:8081/api/sync-tasks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ma Synchronisation",
    "sourcePath": "C:\\Documents",
    "destinationPath": "D:\\Backup",
    "intervalMinutes": 120,
    "active": true,
    "useChecksum": true
  }'

# Déclencher manuellement une synchronisation
curl -X POST http://localhost:8081/api/sync-tasks/1/trigger
```

### Logs

```http
GET /api/sync-logs?page=0&size=20              # Tous les logs (paginés)
GET /api/sync-logs/task/{taskId}?page=0&size=20 # Logs d'une tâche
GET /api/sync-logs/{id}                        # Détails d'un log
```

**Exemple**:
```bash
# Récupérer les logs d'une tâche spécifique
curl http://localhost:8081/api/sync-logs/task/1?page=0&size=10
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
server:
  port: 8081                     # Port du serveur

spring:
  datasource:
    url: jdbc:h2:file:./data/syncdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true              # Console H2 activée
      path: /h2-console

sync:
  default-interval: 7200000      # Intervalle par défaut (2h en ms)
  max-concurrent-syncs: 3        # Max syncs simultanées
  chunk-size: 8192               # Taille buffer copie (8KB)
```

### Base de données

Par défaut, H2 embarquée. Pour passer en production:

1. Ajouter le driver (PostgreSQL, MySQL, etc.) dans `pom.xml`
2. Modifier `application.yml`:

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/syncdb
    driver-class-name: org.postgresql.Driver
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

## Dépannage

### Maven ne se lance pas

**Problème**: `mvn: command not found` ou `mvn` non reconnu

**Solution**:
- **Windows**: Utilisez le wrapper Maven inclus : `.\mvnw.cmd`
- **Linux/WSL**: Installez Maven : `sudo apt install maven`
- Ou utilisez le wrapper Maven qui télécharge automatiquement Maven

### Erreur "release version 17 not supported"

**Problème**: Version Java incompatible

**Solution**:
1. Vérifiez votre version Java : `java --version`
2. Le projet nécessite Java 21 (ou Java 17 minimum)
3. Installez Java 21 :
   - **Ubuntu/WSL**: `sudo apt install openjdk-21-jdk`
   - **Windows**: Téléchargez depuis [Adoptium](https://adoptium.net/)
4. Configurez JAVA_HOME si nécessaire

### L'application ne répond pas sur localhost:8081

**Problème**: Mauvais port configuré

**Solution**: L'application écoute sur le **port 8081**, pas 8080 ou 8081
- Utilisez `http://localhost:8081/api/sync-tasks`
- Modifiez `backend/src/main/resources/application.yml` pour changer le port si nécessaire

### Page blanche sur http://localhost:8081

**Problème**: Pas d'interface web sur le backend

**Solution**: Le backend est une API REST sans interface. Pour l'interface utilisateur :
- Lancez le frontend Angular : `cd frontend && npm start`
- Accédez à `http://localhost:4200`
- Ou utilisez directement l'API REST avec curl/Postman

### Erreur "Le chemin source n'existe pas"

**Problème**: Le chemin source spécifié n'existe pas

**Solution**:
1. Vérifiez que le chemin source existe sur votre système
2. Utilisez le format correct pour votre OS :
   - **Windows**: `C:\\Users\\Documents` (double backslash en JSON)
   - **Linux**: `/home/user/documents`
3. Assurez-vous d'avoir les permissions de lecture sur le source

### Problème avec WSL et Maven

**Problème**: Le wrapper Windows `.mvnw.cmd` ne fonctionne pas dans WSL

**Solution**: Dans WSL, utilisez Maven directement ou créez un wrapper Linux :
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

## Licence

MIT
