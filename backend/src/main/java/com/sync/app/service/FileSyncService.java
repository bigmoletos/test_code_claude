package com.sync.app.service;

import com.sync.app.entity.FileMetadata;
import com.sync.app.entity.SyncLog;
import com.sync.app.entity.SyncTask;
import com.sync.app.repository.FileMetadataRepository;
import com.sync.app.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Service principal pour gérer la synchronisation de fichiers.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileSyncService {

    private final FileMetadataRepository fileMetadataRepository;
    private final SyncLogRepository syncLogRepository;

    @Value("${sync.chunk-size:8192}")
    private int chunkSize;

    // Suivi des synchronisations en cours
    private final Set<Long> runningSyncs = ConcurrentHashMap.newKeySet();

    /**
     * Exécute une synchronisation pour une tâche donnée.
     */
    @Transactional
    public SyncLog executeSync(SyncTask syncTask) {
        if (!runningSyncs.add(syncTask.getId())) {
            log.warn("Sync already running for task: {}", syncTask.getId());
            return null;
        }

        SyncLog syncLog = new SyncLog();
        syncLog.setSyncTask(syncTask);
        syncLog.setStartTime(LocalDateTime.now());
        syncLog.setStatus(SyncLog.SyncStatus.RUNNING);
        syncLog = syncLogRepository.save(syncLog);

        try {
            Path sourcePath = Paths.get(syncTask.getSourcePath());
            Path destPath = Paths.get(syncTask.getDestinationPath());

            // Validation des chemins
            if (!Files.exists(sourcePath)) {
                throw new IOException("Le chemin source n'existe pas: " + sourcePath);
            }

            if (!Files.exists(destPath)) {
                Files.createDirectories(destPath);
            }

            // Récupération des métadonnées existantes
            Map<String, FileMetadata> existingMetadata = new HashMap<>();
            fileMetadataRepository.findBySyncTask(syncTask).forEach(fm ->
                existingMetadata.put(fm.getFilePath(), fm)
            );

            Set<String> processedFiles = new HashSet<>();
            SyncStats stats = new SyncStats();

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
            for (String metaPath : existingMetadata.keySet()) {
                if (!processedFiles.contains(metaPath)) {
                    Path fileToDelete = destPath.resolve(metaPath);
                    if (Files.exists(fileToDelete)) {
                        Files.delete(fileToDelete);
                        stats.filesDeleted++;
                    }
                    fileMetadataRepository.delete(existingMetadata.get(metaPath));
                }
            }

            // Finalisation du log
            syncLog.setEndTime(LocalDateTime.now());
            syncLog.setStatus(SyncLog.SyncStatus.COMPLETED);
            syncLog.setFilesScanned(stats.filesScanned);
            syncLog.setFilesCopied(stats.filesCopied);
            syncLog.setFilesUpdated(stats.filesUpdated);
            syncLog.setFilesDeleted(stats.filesDeleted);
            syncLog.setFilesSkipped(stats.filesSkipped);
            syncLog.setTotalBytes(stats.totalBytes);
            syncLog.setDetails(String.format("Synchronisation réussie: %d fichiers traités", stats.filesScanned));

            return syncLogRepository.save(syncLog);

        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation", e);
            syncLog.setEndTime(LocalDateTime.now());
            syncLog.setStatus(SyncLog.SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            return syncLogRepository.save(syncLog);
        } finally {
            runningSyncs.remove(syncTask.getId());
        }
    }

    /**
     * Détermine si un fichier doit être copié.
     */
    private boolean shouldCopyFile(Path sourceFile, Path destFile, FileMetadata existing, boolean useChecksum) throws IOException {
        if (!Files.exists(destFile)) {
            return true;
        }

        BasicFileAttributes sourceAttrs = Files.readAttributes(sourceFile, BasicFileAttributes.class);
        BasicFileAttributes destAttrs = Files.readAttributes(destFile, BasicFileAttributes.class);

        // Comparaison par taille
        if (sourceAttrs.size() != destAttrs.size()) {
            return true;
        }

        // Comparaison par date de modification
        LocalDateTime sourceModified = LocalDateTime.ofInstant(
            sourceAttrs.lastModifiedTime().toInstant(), ZoneId.systemDefault()
        );

        if (existing != null && !sourceModified.equals(existing.getLastModified())) {
            return true;
        }

        // Comparaison par checksum si activé
        if (useChecksum && existing != null) {
            String currentChecksum = calculateChecksum(sourceFile);
            return !currentChecksum.equals(existing.getChecksum());
        }

        return false;
    }

    /**
     * Calcule le checksum SHA-256 d'un fichier.
     */
    private String calculateChecksum(Path file) throws IOException {
        try {
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
            return sb.toString();
        } catch (Exception e) {
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
                log.warn("Impossible de calculer le checksum pour: {}", relativePath);
            }
        }

        fileMetadataRepository.save(metadata);
    }

    /**
     * Vérifie si une synchronisation est en cours.
     */
    public boolean isSyncRunning(Long taskId) {
        return runningSyncs.contains(taskId);
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
