# Script simple pour tester le projet Java

Write-Host "Test du projet Java Spring Boot" -ForegroundColor Green

# Verifier Java
Write-Host "Verification de Java..." -ForegroundColor Yellow
java -version

# Telecharger Maven temporairement
$mavenUrl = "https://dlcdn.apache.org/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.zip"
$mavenZip = "maven-temp.zip"

Write-Host "Telechargement de Maven..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing

Write-Host "Extraction de Maven..." -ForegroundColor Yellow
Expand-Archive -Path $mavenZip -DestinationPath "." -Force

$mavenDir = Get-ChildItem -Directory | Where-Object { $_.Name -like "apache-maven-*" } | Select-Object -First 1
$mavenBin = "$($mavenDir.Name)\bin\mvn.cmd"

Write-Host "Compilation du projet..." -ForegroundColor Yellow
& $mavenBin clean compile

Write-Host "Execution des tests..." -ForegroundColor Yellow
& $mavenBin test

Write-Host "Lancement de l'application..." -ForegroundColor Yellow
Write-Host "L'application sera accessible sur http://localhost:8080" -ForegroundColor Cyan
Write-Host "Appuyez sur Ctrl+C pour arreter l'application" -ForegroundColor Cyan
& $mavenBin spring-boot:run

# Nettoyage
Write-Host "Nettoyage..." -ForegroundColor Yellow
Remove-Item $mavenZip -Force
Remove-Item $mavenDir -Recurse -Force

Write-Host "Test termine!" -ForegroundColor Green
