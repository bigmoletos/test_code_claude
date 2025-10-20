package com.sync.app.service;

import com.sync.app.entity.FileMetadata;
import com.sync.app.entity.SyncLog;
import com.sync.app.entity.SyncTask;
import com.sync.app.repository.FileMetadataRepository;
import com.sync.app.repository.SyncLogRepository;
import com.sync.app.util.CustomLogger;
import com.sync.app.util.CustomLogger.LogLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service principal pour gérer la synchronisation de fichiers.
 */
@Service
@RequiredArgsConstructor
public class FileSyncService {
    private static final CustomLogger logger = CustomLogger.getLogger("FileSyncService");
    private static final CustomLogger exclusionLogger = CustomLogger.getLogger("FileSyncExclusions");

    // Patterns de fichiers/dossiers à ignorer
    private static final Set<String> EXCLUDED_PATTERNS = Set.of(
        ".git", ".svn", ".hg", // Version control
        "node_modules", ".next", "dist", "build", "target", // Build directories
        ".idea", ".vscode", ".eclipse", // IDE files
        ".DS_Store", "Thumbs.db", "desktop.ini", // System files
        "__pycache__", ".pyc", ".pyo", // Python cache
        ".class", ".jar", ".war", // Java binaries
        "*.tmp", "*.temp", "*.log", // Temporary files
        "package-info.java" // Fichier Java problématique (annotations spéciales, erreurs WSL)
    );

    static {
        logger.setLevel(LogLevel.DEBUG)
              .setConsoleOutput(true)
              .setLogFile("./logs/file-sync.log")
              .setMaxFileSize(20 * 1024 * 1024) // 20MB
              .setMaxBackupFiles(10)
              .setCompressionEnabled(true);

        // Logger spécifique pour les exclusions
        exclusionLogger.setLevel(LogLevel.INFO)
                      .setConsoleOutput(false)  // Pas de spam console
                      .setLogFile("./logs/exclusions.log")
                      .setMaxFileSize(50 * 1024 * 1024) // 50MB (plus de données)
                      .setMaxBackupFiles(5)
                      .setCompressionEnabled(true);
    }

    private final FileMetadataRepository fileMetadataRepository;
    private final SyncLogRepository syncLogRepository;

    @Value("${sync.chunk-size:8192}")
    private int chunkSize;

    @Value("${sync.max-path-length:250}") // Limite de longueur du chemin (Windows: 260, Linux: 255)
    private int maxPathLength;

    @Value("${sync.max-filename-length:200}") // Limite de longueur du nom de fichier
    private int maxFileNameLength;

    @Value("${sync.exclude-package-info:true}") // Exclure les package-info.java (problèmes WSL)
    private boolean excludePackageInfo;

    // Suivi des synchronisations en cours
    private final Set<Long> runningSyncs = ConcurrentHashMap.newKeySet();

    /**
     * Vérifie si un fichier/dossier doit être exclu de la synchronisation
     */
    private boolean shouldExclude(Path path) {
        String fileName = path.getFileName().toString();
        String fullPath = path.toString();

        // Vérifier package-info.java en priorité (source fréquente de problèmes)
        if (excludePackageInfo && "package-info.java".equals(fileName)) {
            logger.warn("❌ EXCLUSION - package-info.java détecté (fichier problématique en WSL)");
            logger.info("   Chemin: {}", truncateString(fullPath, 200));

            exclusionLogger.warn("=== EXCLUSION: PACKAGE-INFO.JAVA ===");
            exclusionLogger.info("Fichier: package-info.java");
            exclusionLogger.info("Raison: Fichier Java spécial causant des erreurs 'Invalid argument' en WSL");
            exclusionLogger.info("Chemin: {}", fullPath);
            exclusionLogger.info("Config: sync.exclude-package-info={}", excludePackageInfo);
            exclusionLogger.info("Solution: Mettre sync.exclude-package-info=false dans application.yml pour forcer la copie");
            exclusionLogger.info("----------------------------------------");
            return true;
        }

        // Vérifier la longueur du nom de fichier
        if (fileName.length() > maxFileNameLength) {
            logger.warn("❌ EXCLUSION - Nom trop long ({} > {} caractères): {}",
                fileName.length(), maxFileNameLength, truncateString(fileName, 150));
            logger.info("   Chemin complet: {}", truncateString(fullPath, 200));

            // Log détaillé dans fichier exclusions
            exclusionLogger.warn("=== EXCLUSION: NOM TROP LONG ===");
            exclusionLogger.info("Fichier: {}", fileName);
            exclusionLogger.info("Longueur: {} caractères (max: {})", fileName.length(), maxFileNameLength);
            exclusionLogger.info("Chemin: {}", fullPath);
            exclusionLogger.info("----------------------------------------");
            return true;
        }

        // Vérifier la longueur du chemin complet
        if (fullPath.length() > maxPathLength) {
            logger.warn("❌ EXCLUSION - Chemin trop long ({} > {} caractères)",
                fullPath.length(), maxPathLength);
            logger.info("   Chemin: {}", truncateString(fullPath, 200));
            logger.info("   Fichier: {}", fileName);

            // Log détaillé dans fichier exclusions
            exclusionLogger.warn("=== EXCLUSION: CHEMIN TROP LONG ===");
            exclusionLogger.info("Fichier: {}", fileName);
            exclusionLogger.info("Longueur chemin: {} caractères (max: {})", fullPath.length(), maxPathLength);
            exclusionLogger.info("Chemin: {}", fullPath);
            exclusionLogger.info("----------------------------------------");
            return true;
        }

        // Vérifier les caractères invalides/problématiques dans le nom
        if (hasInvalidCharacters(fileName)) {
            logger.warn("❌ EXCLUSION - Caractères invalides détectés");
            logger.info("   Fichier: {}", fileName);
            logger.info("   Chemin: {}", truncateString(fullPath, 200));
            logger.info("   Caractères suspects: {}", findInvalidChars(fileName));

            // Log détaillé dans fichier exclusions
            exclusionLogger.warn("=== EXCLUSION: CARACTÈRES INVALIDES ===");
            exclusionLogger.info("Fichier: {}", fileName);
            exclusionLogger.info("Caractères invalides: {}", findInvalidChars(fileName));
            exclusionLogger.info("Chemin: {}", fullPath);
            exclusionLogger.info("----------------------------------------");
            return true;
        }

        // Vérifier les patterns exacts
        if (EXCLUDED_PATTERNS.contains(fileName)) {
            logger.debug("Exclusion (pattern exact): {}", fileName);
            return true;
        }

        // Vérifier les patterns avec wildcards
        for (String pattern : EXCLUDED_PATTERNS) {
            if (pattern.contains("*")) {
                String regex = pattern.replace(".", "\\.").replace("*", ".*");
                if (fileName.matches(regex)) {
                    logger.debug("Exclusion (pattern wildcard {}): {}", pattern, fileName);
                    return true;
                }
            }
        }

        // Vérifier si un dossier parent est exclu
        Path current = path.getParent();
        while (current != null) {
            String dirName = current.getFileName() != null ? current.getFileName().toString() : "";
            if (EXCLUDED_PATTERNS.contains(dirName)) {
                logger.debug("Exclusion (parent exclu): {} dans {}", fileName, dirName);
                return true;
            }
            current = current.getParent();
        }

        return false;
    }

    /**
     * Vérifie si un nom de fichier contient des caractères invalides
     * Caractères problématiques : < > : " | ? * et caractères de contrôle (0-31)
     */
    private boolean hasInvalidCharacters(String fileName) {
        // Caractères interdits sous Windows : < > : " / \ | ? *
        // Sous Linux : principalement / et le caractère null
        // On vérifie aussi les caractères de contrôle (ASCII 0-31)

        for (char c : fileName.toCharArray()) {
            // Caractères de contrôle (0-31)
            if (c < 32) {
                return true;
            }
            // Caractères invalides Windows (communs)
            if (c == '<' || c == '>' || c == ':' || c == '"' ||
                c == '|' || c == '?' || c == '*') {
                return true;
            }
            // Caractère null
            if (c == '\0') {
                return true;
            }
        }

        return false;
    }

    /**
     * Trouve et liste les caractères invalides dans un nom de fichier
     */
    private String findInvalidChars(String fileName) {
        StringBuilder invalid = new StringBuilder();
        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);
            if (c < 32) {
                invalid.append(String.format("'\\u%04x' (contrôle) à position %d, ", (int)c, i));
            } else if (c == '<' || c == '>' || c == ':' || c == '"' ||
                       c == '|' || c == '?' || c == '*') {
                invalid.append(String.format("'%c' à position %d, ", c, i));
            } else if (c == '\0') {
                invalid.append(String.format("'\\0' (null) à position %d, ", i));
            }
        }
        return invalid.length() > 0 ? invalid.substring(0, invalid.length() - 2) : "aucun";
    }

    /**
     * Tronque une chaîne pour l'affichage dans les logs
     */
    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "... (" + str.length() + " caractères)";
    }

    /**
     * Convertit un chemin Windows en chemin WSL si nécessaire.
     * Exemple: D:\programmation -> /mnt/d/programmation
     */
    private String convertWindowsPathToWsl(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        // Détecte un chemin Windows (ex: C:\, D:\, etc.)
        if (path.matches("^[A-Za-z]:\\\\.*")) {
            char driveLetter = Character.toLowerCase(path.charAt(0));
            String pathWithoutDrive = path.substring(2); // Enlève "C:"
            String wslPath = "/mnt/" + driveLetter + pathWithoutDrive.replace("\\", "/");
            logger.info("Conversion chemin Windows -> WSL: {} -> {}", path, wslPath);
            return wslPath;
        }

        return path;
    }

    /**
     * Exécute une synchronisation pour une tâche donnée.
     */
    public SyncLog executeSync(SyncTask syncTask) {
        logger.info("=== Début de synchronisation ===");
        logger.info("Tâche: {} (ID: {})", syncTask.getName(), syncTask.getId());
        logger.info("Source: {}", syncTask.getSourcePath());
        logger.info("Destination: {}", syncTask.getDestinationPath());
        logger.info("Utiliser checksum: {}", syncTask.getUseChecksum());

        if (!runningSyncs.add(syncTask.getId())) {
            logger.warn("Synchronisation déjà en cours pour la tâche: {} (ID: {})", syncTask.getName(), syncTask.getId());
            return null;
        }

        SyncLog syncLog = new SyncLog();
        syncLog.setSyncTask(syncTask);
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setStatus(SyncLog.SyncStatus.RUNNING);
        final SyncLog finalSyncLog = syncLogRepository.save(syncLog);
        logger.debug("SyncLog créé avec ID: {}", finalSyncLog.getId());

        try {
            // Conversion des chemins Windows vers WSL si nécessaire
            String sourcePathStr = convertWindowsPathToWsl(syncTask.getSourcePath());
            String destPathStr = convertWindowsPathToWsl(syncTask.getDestinationPath());

            Path sourcePath = Paths.get(sourcePathStr);
            Path destPath = Paths.get(destPathStr);

            // Validation des chemins
            if (!Files.exists(sourcePath)) {
                logger.error("Le chemin source n'existe pas: {}", sourcePath);
                throw new IOException("Le chemin source n'existe pas: " + sourcePath);
            }
            logger.debug("Chemin source validé: {}", sourcePath);

            if (!Files.exists(destPath)) {
                logger.info("Création du répertoire de destination: {}", destPath);
                Files.createDirectories(destPath);
            }
            logger.debug("Chemin destination validé: {}", destPath);

            // Récupération des métadonnées existantes
            Map<String, FileMetadata> existingMetadata = new HashMap<>();
            fileMetadataRepository.findBySyncTask(syncTask).forEach(fm ->
                existingMetadata.put(fm.getFilePath(), fm)
            );
            logger.info("Métadonnées existantes chargées: {} fichiers", existingMetadata.size());

            Set<String> processedFiles = new HashSet<>();
            SyncStats stats = new SyncStats();
            AtomicLong lastLogTime = new AtomicLong(System.currentTimeMillis());
            AtomicLong lastDbUpdateTime = new AtomicLong(System.currentTimeMillis());

            logger.info("Début du parcours des fichiers source...");

            // Parcours des fichiers source
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        // Vérifier si le fichier doit être exclu (patterns, longueur chemin/nom)
                        if (shouldExclude(file)) {
                            stats.filesExcluded++;
                            return FileVisitResult.CONTINUE;
                        }

                        String relativePath = sourcePath.relativize(file).toString();
                        processedFiles.add(relativePath);

                        Path destFile = destPath.resolve(relativePath);

                        // Vérifier la longueur du chemin de destination
                        String destFullPath = destFile.toString();
                        if (destFullPath.length() > maxPathLength) {
                            stats.filesExcluded++;
                            logger.warn("❌ EXCLUSION - Chemin DESTINATION trop long ({} > {} caractères)",
                                destFullPath.length(), maxPathLength);
                            logger.info("   Fichier: {}", file.getFileName());
                            logger.info("   Chemin source: {} caractères", file.toString().length());
                            logger.info("   Chemin dest: {} caractères", destFullPath.length());

                            exclusionLogger.warn("=== EXCLUSION: CHEMIN DESTINATION TROP LONG ===");
                            exclusionLogger.info("Fichier: {}", file.getFileName());
                            exclusionLogger.info("Chemin source: {}", file);
                            exclusionLogger.info("Longueur source: {} caractères", file.toString().length());
                            exclusionLogger.info("Chemin destination: {}", truncateString(destFullPath, 200));
                            exclusionLogger.info("Longueur destination: {} caractères (max: {})", destFullPath.length(), maxPathLength);
                            exclusionLogger.info("----------------------------------------");
                            return FileVisitResult.CONTINUE;
                        }

                        stats.filesScanned++;

                        FileMetadata existing = existingMetadata.get(relativePath);
                        boolean needsCopy = shouldCopyFile(file, destFile, existing, syncTask.getUseChecksum());

                        if (needsCopy) {
                            try {
                                // Créer répertoire parent si nécessaire
                                Files.createDirectories(destFile.getParent());

                                // Copier le fichier
                                Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING,
                                           StandardCopyOption.COPY_ATTRIBUTES);

                                if (existing == null) {
                                    stats.filesCopied++;
                                } else {
                                    stats.filesUpdated++;
                                }
                                stats.totalBytes += attrs.size();

                                // Mettre à jour les métadonnées
                                updateFileMetadata(syncTask, relativePath, file, attrs);
                            } catch (InvalidPathException ipe) {
                                stats.filesWithErrors++;
                                logger.error("❌ ERREUR COPIE - InvalidPathException");
                                logger.info("   Fichier: {}", file.getFileName());
                                logger.info("   Chemin source: {}", file);
                                logger.info("   Chemin dest: {}", destFile);
                                logger.info("   Raison: {}", ipe.getReason());
                                logger.info("   Message: {}", ipe.getMessage());
                            } catch (IOException ioe) {
                                stats.filesWithErrors++;
                                logger.error("❌ ERREUR COPIE - {} : {}",
                                    ioe.getClass().getSimpleName(), ioe.getMessage());
                                logger.info("   Fichier: {}", file.getFileName());
                                logger.info("   Chemin source: {}", file);
                                logger.info("   Chemin dest: {}", destFile);
                                logger.info("   Taille: {} octets", attrs.size());
                                if (ioe.getMessage() != null && ioe.getMessage().contains("Invalid argument")) {
                                    logger.warn("   ⚠️  Erreur 'Invalid argument' - Probable:");
                                    logger.warn("      - Caractères spéciaux dans le nom");
                                    logger.warn("      - Problème conversion Windows→WSL");
                                    logger.warn("      - Nom de fichier: {}", file.getFileName());
                                    logger.warn("      - Caractères suspects: {}", findInvalidChars(file.getFileName().toString()));
                                }
                            } catch (Exception e) {
                                stats.filesWithErrors++;
                                logger.error("❌ ERREUR COPIE - Exception inattendue: {}", e.getClass().getName());
                                logger.info("   Fichier: {}", file.getFileName());
                                logger.info("   Chemin source: {}", file);
                                logger.info("   Chemin dest: {}", destFile);
                                logger.info("   Message: {}", e.getMessage());
                                logger.error("   Stack trace:", e);
                            }
                        } else {
                            stats.filesSkipped++;
                        }
                    } catch (Exception e) {
                        stats.filesWithErrors++;
                        logger.warn("Erreur lors du traitement du fichier: {} - {}", file, e.getMessage());
                        // Continuer malgré l'erreur
                    }

                    // Afficher la progression tous les 100 fichiers
                    if (stats.filesScanned % 100 == 0) {
                        long currentTime = System.currentTimeMillis();

                        // Log console tous les 100 fichiers
                        if (currentTime - lastLogTime.get() > 5000) { // au moins toutes les 5 secondes
                            logger.info("Progression: {} fichiers scannés, {} copiés, {} mis à jour, {} ignorés, {} exclus, {} erreurs",
                                stats.filesScanned, stats.filesCopied, stats.filesUpdated, stats.filesSkipped,
                                stats.filesExcluded, stats.filesWithErrors);
                            lastLogTime.set(currentTime);
                        }

                        // Mise à jour en base toutes les 10 secondes
                        if (currentTime - lastDbUpdateTime.get() > 10000) {
                            updateSyncLogProgress(finalSyncLog, stats);
                            lastDbUpdateTime.set(currentTime);
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(sourcePath)) {
                        return FileVisitResult.CONTINUE;
                    }

                    // Vérifier si le dossier doit être exclu
                    if (shouldExclude(dir)) {
                        logger.debug("Dossier exclu: {}", dir.getFileName());
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    String relativePath = sourcePath.relativize(dir).toString();
                    processedFiles.add(relativePath);

                    Path destDir = destPath.resolve(relativePath);
                    if (!Files.exists(destDir)) {
                        Files.createDirectories(destDir);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            // Suppression des fichiers qui n'existent plus dans la source
            logger.info("Vérification des fichiers supprimés de la source...");
            int deletedCount = 0;
            for (String metaPath : existingMetadata.keySet()) {
                if (!processedFiles.contains(metaPath)) {
                    Path fileToDelete = destPath.resolve(metaPath);
                    if (Files.exists(fileToDelete)) {
                        logger.debug("Suppression du fichier: {}", metaPath);
                        Files.delete(fileToDelete);
                        stats.filesDeleted++;
                        deletedCount++;
                    }
                    fileMetadataRepository.delete(existingMetadata.get(metaPath));
                }
            }
            if (deletedCount > 0) {
                logger.info("{} fichiers supprimés (absents de la source)", deletedCount);
            }

            // Log final de la progression
            logger.info("=== Synchronisation terminée ===");
            logger.info("Fichiers scannés: {}", stats.filesScanned);
            logger.info("Fichiers copiés: {}", stats.filesCopied);
            logger.info("Fichiers mis à jour: {}", stats.filesUpdated);
            logger.info("Fichiers supprimés: {}", stats.filesDeleted);
            logger.info("Fichiers ignorés (inchangés): {}", stats.filesSkipped);
            logger.info("Fichiers exclus: {}", stats.filesExcluded);
            logger.info("Fichiers en erreur: {}", stats.filesWithErrors);
            logger.info("Volume total: {} octets ({} MB)", stats.totalBytes, stats.totalBytes / (1024 * 1024));

            // Finalisation du log
            finalSyncLog.setEndTime(LocalDateTime.now());
            finalSyncLog.setStatus(SyncLog.SyncStatus.COMPLETED);
            finalSyncLog.setFilesScanned(stats.filesScanned);
            finalSyncLog.setFilesCopied(stats.filesCopied);
            finalSyncLog.setFilesUpdated(stats.filesUpdated);
            finalSyncLog.setFilesDeleted(stats.filesDeleted);
            finalSyncLog.setFilesSkipped(stats.filesSkipped);
            finalSyncLog.setFilesExcluded(stats.filesExcluded);
            finalSyncLog.setFilesWithErrors(stats.filesWithErrors);
            finalSyncLog.setTotalBytes(stats.totalBytes);

            String details = String.format("Synchronisation réussie: %d fichiers traités", stats.filesScanned);
            if (stats.filesWithErrors > 0) {
                details += String.format(" (%d erreurs ignorées)", stats.filesWithErrors);
            }
            finalSyncLog.setDetails(details);

            return syncLogRepository.save(finalSyncLog);

        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation de la tâche: {}", syncTask.getName(), e);
            finalSyncLog.setEndTime(LocalDateTime.now());
            finalSyncLog.setStatus(SyncLog.SyncStatus.FAILED);
            finalSyncLog.setErrorMessage(e.getMessage());
            return syncLogRepository.save(finalSyncLog);
        } finally {
            logger.debug("Synchronisation terminée pour la tâche ID: {}", syncTask.getId());
            runningSyncs.remove(syncTask.getId());
        }
    }

    /**
     * Détermine si un fichier doit être copié.
     */
    private boolean shouldCopyFile(Path sourceFile, Path destFile, FileMetadata existing, boolean useChecksum) throws IOException {
        if (!Files.exists(destFile)) {
            logger.trace("Fichier {} n'existe pas à la destination -> copie nécessaire", sourceFile.getFileName());
            return true;
        }

        BasicFileAttributes sourceAttrs = Files.readAttributes(sourceFile, BasicFileAttributes.class);
        BasicFileAttributes destAttrs = Files.readAttributes(destFile, BasicFileAttributes.class);

        // Comparaison par taille
        if (sourceAttrs.size() != destAttrs.size()) {
            logger.trace("Taille différente pour {} ({} vs {}) -> copie nécessaire",
                sourceFile.getFileName(), sourceAttrs.size(), destAttrs.size());
            return true;
        }

        // Comparaison par date de modification
        LocalDateTime sourceModified = LocalDateTime.ofInstant(
            sourceAttrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()
        );

        if (existing != null && !sourceModified.equals(existing.getLastModified())) {
            logger.trace("Date de modification différente pour {} -> copie nécessaire", sourceFile.getFileName());
            return true;
        }

        // Comparaison par checksum si activé
        if (useChecksum && existing != null) {
            String currentChecksum = calculateChecksum(sourceFile);
            boolean checksumDifferent = !currentChecksum.equals(existing.getChecksum());
            if (checksumDifferent) {
                logger.trace("Checksum différent pour {} -> copie nécessaire", sourceFile.getFileName());
            }
            return checksumDifferent;
        }

        return false;
    }

    /**
     * Calcule le checksum SHA-256 d'un fichier.
     */
    private String calculateChecksum(Path file) throws IOException {
        try {
            logger.trace("Calcul du checksum pour: {}", file.getFileName());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[chunkSize];

            try (var is = Files.newInputStream(file)) {
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String checksum = sb.toString();
            logger.trace("Checksum calculé: {} pour {}", checksum.substring(0, 8) + "...", file.getFileName());
            return checksum;
        } catch (Exception e) {
            logger.error("Erreur lors du calcul du checksum pour: {}", file.getFileName(), e);
            throw new IOException("Erreur lors du calcul du checksum", e);
        }
    }

    /**
     * Met à jour les métadonnées d'un fichier.
     */
    private void updateFileMetadata(SyncTask syncTask, String relativePath, Path file, BasicFileAttributes attrs) {
        FileMetadata metadata = fileMetadataRepository
            .findBySyncTaskAndFilePath(syncTask, relativePath)
            .orElse(new FileMetadata());

        metadata.setSyncTask(syncTask);
        metadata.setFilePath(relativePath);
        metadata.setFileSize(attrs.size());
        metadata.setLastModified(LocalDateTime.ofInstant(
            attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()
        ));
        metadata.setLastSynced(LocalDateTime.now());
        metadata.setIsDirectory(attrs.isDirectory());

        if (syncTask.getUseChecksum() && !attrs.isDirectory()) {
            try {
                metadata.setChecksum(calculateChecksum(file));
            } catch (IOException e) {
                logger.warn("Impossible de calculer le checksum pour: {}", relativePath, e);
            }
        }

        fileMetadataRepository.save(metadata);
    }

    /**
     * Met à jour le SyncLog avec les statistiques en cours de synchronisation.
     */
    private void updateSyncLogProgress(SyncLog syncLog, SyncStats stats) {
        try {
            syncLog.setFilesScanned(stats.filesScanned);
            syncLog.setFilesCopied(stats.filesCopied);
            syncLog.setFilesUpdated(stats.filesUpdated);
            syncLog.setFilesDeleted(stats.filesDeleted);
            syncLog.setFilesSkipped(stats.filesSkipped);
            syncLog.setFilesExcluded(stats.filesExcluded);
            syncLog.setFilesWithErrors(stats.filesWithErrors);
            syncLog.setTotalBytes(stats.totalBytes);
            syncLog.setDetails(String.format("En cours: %d fichiers scannés...", stats.filesScanned));
            syncLogRepository.save(syncLog);
        } catch (Exception e) {
            logger.warn("Erreur lors de la mise à jour de la progression: {}", e.getMessage());
        }
    }

    /**
     * Vérifie si une synchronisation est en cours.
     */
    public boolean isSyncRunning(Long taskId) {
        boolean isRunning = runningSyncs.contains(taskId);
        logger.debug("Vérification synchronisation en cours pour tâche ID {}: {}", taskId, isRunning);
        return isRunning;
    }

    /**
     * Classe interne pour les statistiques de synchronisation.
     */
    private static class SyncStats {
        long filesScanned = 0;
        long filesCopied = 0;
        long filesUpdated = 0;
        long filesDeleted = 0;
        long filesSkipped = 0;
        long filesExcluded = 0;
        long filesWithErrors = 0;
        long totalBytes = 0;
    }
}
