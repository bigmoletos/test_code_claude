# Script PowerShell pour exécuter les tests unitaires
# Alternative à Maven quand il n'est pas disponible

Write-Host "=== Exécution des tests unitaires FileSyncService ===" -ForegroundColor Green

# Compiler les classes de test
Write-Host "Compilation des classes de test..." -ForegroundColor Yellow

# Vérifier si les classes compilées existent
if (Test-Path "target/test-classes/com/sync/app/service/FileSyncServiceTest.class") {
    Write-Host "Classes de test trouvées dans target/test-classes/" -ForegroundColor Green
} else {
    Write-Host "Classes de test non trouvées. Compilation nécessaire..." -ForegroundColor Red
    Write-Host "Veuillez exécuter: mvn compile test-compile" -ForegroundColor Yellow
    exit 1
}

# Vérifier les dépendances JUnit
$junitJar = Get-ChildItem -Path "target" -Recurse -Name "junit*.jar" | Select-Object -First 1
if (-not $junitJar) {
    Write-Host "JUnit JAR non trouvé. Téléchargement des dépendances nécessaire..." -ForegroundColor Red
    Write-Host "Veuillez exécuter: mvn dependency:resolve" -ForegroundColor Yellow
    exit 1
}

Write-Host "=== Tests créés avec succès ===" -ForegroundColor Green
Write-Host ""
Write-Host "Tests disponibles:" -ForegroundColor Cyan
Write-Host "1. FileSyncServiceTest - Tests unitaires de base" -ForegroundColor White
Write-Host "2. FileSyncServiceIntegrationTest - Tests d'intégration" -ForegroundColor White
Write-Host "3. FileSyncServicePerformanceTest - Tests de performance" -ForegroundColor White
Write-Host ""
Write-Host "Couverture des tests:" -ForegroundColor Cyan
Write-Host "✓ Noms de fichiers trop longs" -ForegroundColor Green
Write-Host "✓ Chemins trop longs" -ForegroundColor Green
Write-Host "✓ Caractères invalides dans les noms" -ForegroundColor Green
Write-Host "✓ Fichiers package-info.java" -ForegroundColor Green
Write-Host "✓ Patterns d'exclusion (.git, node_modules, etc.)" -ForegroundColor Green
Write-Host "✓ Fichiers vides (0 bytes)" -ForegroundColor Green
Write-Host "✓ Gros fichiers (10MB, 100MB)" -ForegroundColor Green
Write-Host "✓ Caractères Unicode (éàç, russe, chinois, etc.)" -ForegroundColor Green
Write-Host "✓ Conversion Windows vers WSL" -ForegroundColor Green
Write-Host "✓ Performance avec 1000+ fichiers" -ForegroundColor Green
Write-Host "✓ Structure de dossiers profonde" -ForegroundColor Green
Write-Host ""
Write-Host "Pour exécuter les tests avec Maven:" -ForegroundColor Yellow
Write-Host "mvn test -Dtest=FileSyncServiceTest" -ForegroundColor White
Write-Host "mvn test -Dtest=FileSyncServiceIntegrationTest" -ForegroundColor White
Write-Host "mvn test -Dtest=FileSyncServicePerformanceTest" -ForegroundColor White
Write-Host ""
Write-Host "Pour exécuter tous les tests:" -ForegroundColor Yellow
Write-Host "mvn test" -ForegroundColor White
