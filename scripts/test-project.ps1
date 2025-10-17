# Script pour tester le projet Java sans installer Maven globalement

Write-Host "🚀 Test du projet Java Spring Boot" -ForegroundColor Green

# Vérifier Java
Write-Host "📋 Vérification de Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "✅ Java trouvé: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Java n'est pas installé ou pas dans le PATH" -ForegroundColor Red
    exit 1
}

# Créer un dossier temporaire pour Maven
$mavenDir = ".\temp-maven"
if (Test-Path $mavenDir) {
    Remove-Item $mavenDir -Recurse -Force
}
New-Item -ItemType Directory -Path $mavenDir | Out-Null

Write-Host "📥 Téléchargement de Maven..." -ForegroundColor Yellow

# Télécharger Maven
$mavenUrl = "https://dlcdn.apache.org/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.zip"
$mavenZip = "$mavenDir\maven.zip"

try {
    Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
    Write-Host "✅ Maven téléchargé" -ForegroundColor Green
} catch {
    Write-Host "❌ Erreur lors du téléchargement de Maven" -ForegroundColor Red
    exit 1
}

# Extraire Maven
Write-Host "📦 Extraction de Maven..." -ForegroundColor Yellow
Expand-Archive -Path $mavenZip -DestinationPath $mavenDir -Force
$mavenHome = Get-ChildItem -Path $mavenDir -Directory | Where-Object { $_.Name -like "apache-maven-*" } | Select-Object -First 1
$mavenBin = "$($mavenHome.FullName)\bin\mvn.cmd"

Write-Host "🔨 Compilation du projet..." -ForegroundColor Yellow

# Compiler le projet
try {
    & $mavenBin clean compile
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Compilation réussie!" -ForegroundColor Green
    } else {
        Write-Host "❌ Erreur de compilation" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Erreur lors de la compilation" -ForegroundColor Red
}

Write-Host "🧪 Exécution des tests..." -ForegroundColor Yellow

# Exécuter les tests
try {
    & $mavenBin test
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Tests réussis!" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Certains tests ont échoué" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Erreur lors de l'exécution des tests" -ForegroundColor Red
}

Write-Host "🏃 Lancement de l'application..." -ForegroundColor Yellow

# Lancer l'application
try {
    Write-Host "🌐 L'application sera accessible sur http://localhost:8080" -ForegroundColor Cyan
    Write-Host "⏹️ Appuyez sur Ctrl+C pour arrêter l'application" -ForegroundColor Cyan
    & $mavenBin spring-boot:run
} catch {
    Write-Host "❌ Erreur lors du lancement de l'application" -ForegroundColor Red
}

# Nettoyage
Write-Host "🧹 Nettoyage..." -ForegroundColor Yellow
Remove-Item $mavenDir -Recurse -Force

Write-Host "✨ Test terminé!" -ForegroundColor Green
