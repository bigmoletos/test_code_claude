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

    static {
        logger.setLevel(LogLevel.DEBUG)
              .setConsoleOutput(true)
              .setLogFile("./logs/file-sync.log")
              .setMaxFileSize(20 * 1024 * 1024) // 20MB
              .setMaxBackupFiles(10)
              .setCompressionEnabled(true);
    }

    private final FileMetadataRepository fileMetadataRepository;
    private final SyncLogRepository syncLogRepository;

    @Value("${sync.chunk-size:8192}")
    private int chunkSize;

    // Suivi des synchronisations en cours
    private final Set<Long> runningSyncs = ConcurrentHashMap.newKeySet();

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
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativePath = sourcePath.relativize(file).toString();
                    processedFiles.add(relativePath);

                    Path destFile = destPath.resolve(relativePath);
                    stats.filesScanned++;

                    FileMetadata existing = existingMetadata.get(relativePath);
                    boolean needsCopy = shouldCopyFile(file, destFile, existing, syncTask.getUseChecksum());

                    if (needsCopy) {
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
                    } else {
                        stats.filesSkipped++;
                    }

                    // Afficher la progression tous les 100 fichiers
                    if (stats.filesScanned % 100 == 0) {
                        long currentTime = System.currentTimeMillis();

                        // Log console tous les 100 fichiers
                        if (currentTime - lastLogTime.get() > 5000) { // au moins toutes les 5 secondes
                            logger.info("Progression: {} fichiers scannés, {} copiés, {} mis à jour, {} ignorés",
                                stats.filesScanned, stats.filesCopied, stats.filesUpdated, stats.filesSkipped);
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
            logger.info("Volume total: {} octets ({} MB)", stats.totalBytes, stats.totalBytes / (1024 * 1024));

            // Finalisation du log
            finalSyncLog.setEndTime(LocalDateTime.now());
            finalSyncLog.setStatus(SyncLog.SyncStatus.COMPLETED);
            finalSyncLog.setFilesScanned(stats.filesScanned);
            finalSyncLog.setFilesCopied(stats.filesCopied);
            finalSyncLog.setFilesUpdated(stats.filesUpdated);
            finalSyncLog.setFilesDeleted(stats.filesDeleted);
            finalSyncLog.setFilesSkipped(stats.filesSkipped);
            finalSyncLog.setTotalBytes(stats.totalBytes);
            finalSyncLog.setDetails(String.format("Synchronisation réussie: %d fichiers traités", stats.filesScanned));

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
        long totalBytes = 0;
    }
}
