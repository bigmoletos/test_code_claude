#!/bin/bash

# Script d'installation complète du projet de synchronisation de dossiers
# Compatible Linux et WSL

set -e  # Arrêt en cas d'erreur

echo "=========================================="
echo "Installation du projet Folder Sync"
echo "=========================================="
echo ""

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction pour afficher des messages
info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Vérification de Java
info "Vérification de Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        info "Java $JAVA_VERSION détecté ✓"
    else
        error "Java 17 ou supérieur requis. Version détectée: $JAVA_VERSION"
        exit 1
    fi
else
    error "Java n'est pas installé"
    echo "Installez Java 17+ avec:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "  Fedora: sudo dnf install java-17-openjdk"
    exit 1
fi

# Vérification de Maven
info "Vérification de Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | grep "Apache Maven" | cut -d' ' -f3)
    info "Maven $MVN_VERSION détecté ✓"
else
    error "Maven n'est pas installé"
    echo "Installez Maven avec:"
    echo "  Ubuntu/Debian: sudo apt install maven"
    echo "  Fedora: sudo dnf install maven"
    exit 1
fi

# Vérification de Node.js
info "Vérification de Node.js..."
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$NODE_VERSION" -ge 18 ]; then
        info "Node.js v$(node -v | cut -d'v' -f2) détecté ✓"
    else
        error "Node.js 18+ requis. Version détectée: v$(node -v)"
        exit 1
    fi
else
    error "Node.js n'est pas installé"
    echo "Installez Node.js 18+ depuis https://nodejs.org/"
    exit 1
fi

# Vérification de npm
info "Vérification de npm..."
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm -v)
    info "npm $NPM_VERSION détecté ✓"
else
    error "npm n'est pas installé"
    exit 1
fi

echo ""
echo "=========================================="
echo "Installation du Backend (Spring Boot)"
echo "=========================================="
echo ""

cd backend

# Nettoyage des builds précédents
if [ -d "target" ]; then
    info "Nettoyage du dossier target..."
    rm -rf target
fi

# Téléchargement et installation des dépendances Maven
info "Téléchargement des dépendances Maven..."
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    info "Backend installé avec succès ✓"
else
    error "Échec de l'installation du backend"
    exit 1
fi

cd ..

echo ""
echo "=========================================="
echo "Installation du Frontend (Angular)"
echo "=========================================="
echo ""

cd frontend

# Nettoyage des installations précédentes
if [ -d "node_modules" ]; then
    info "Nettoyage du dossier node_modules..."
    rm -rf node_modules
fi

if [ -d "dist" ]; then
    info "Nettoyage du dossier dist..."
    rm -rf dist
fi

if [ -d ".angular" ]; then
    rm -rf .angular
fi

# Installation des dépendances npm
info "Installation des dépendances npm..."
npm install

if [ $? -eq 0 ]; then
    info "Frontend installé avec succès ✓"
else
    error "Échec de l'installation du frontend"
    exit 1
fi

# Installation de Angular CLI globalement si non présent
if ! command -v ng &> /dev/null; then
    warn "Angular CLI n'est pas installé globalement"
    read -p "Voulez-vous installer Angular CLI globalement? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        info "Installation de Angular CLI..."
        npm install -g @angular/cli
    fi
fi

cd ..

echo ""
echo "=========================================="
echo "Création des dossiers nécessaires"
echo "=========================================="
echo ""

# Création du dossier pour la base de données
if [ ! -d "backend/data" ]; then
    info "Création du dossier backend/data..."
    mkdir -p backend/data
fi

# Création du dossier logs
if [ ! -d "logs" ]; then
    info "Création du dossier logs..."
    mkdir -p logs
fi

echo ""
echo "=========================================="
echo "Vérification de la configuration"
echo "=========================================="
echo ""

# Vérification des fichiers de configuration
if [ -f "backend/src/main/resources/application.yml" ]; then
    info "Fichier application.yml présent ✓"
else
    error "Fichier application.yml manquant"
    exit 1
fi

if [ -f "frontend/package.json" ]; then
    info "Fichier package.json présent ✓"
else
    error "Fichier package.json manquant"
    exit 1
fi

if [ -f "frontend/angular.json" ]; then
    info "Fichier angular.json présent ✓"
else
    error "Fichier angular.json manquant"
    exit 1
fi

echo ""
echo "=========================================="
echo "Installation terminée avec succès! 🎉"
echo "=========================================="
echo ""
echo "Pour démarrer l'application:"
echo ""
echo "1. Backend (dans un terminal):"
echo "   cd backend"
echo "   mvn spring-boot:run"
echo "   → http://localhost:8080"
echo ""
echo "2. Frontend (dans un autre terminal):"
echo "   cd frontend"
echo "   npm start"
echo "   → http://localhost:4200"
echo ""
echo "3. Console H2 (base de données):"
echo "   → http://localhost:8080/h2-console"
echo "   JDBC URL: jdbc:h2:file:./data/syncdb"
echo "   Username: sa"
echo "   Password: (laisser vide)"
echo ""
echo "Pour plus d'informations, consultez README.md"
echo ""
