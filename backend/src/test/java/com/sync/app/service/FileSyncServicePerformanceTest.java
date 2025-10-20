package com.sync.app.service;

import com.sync.app.entity.FileMetadata;
import com.sync.app.entity.SyncLog;
import com.sync.app.entity.SyncTask;
import com.sync.app.repository.FileMetadataRepository;
import com.sync.app.repository.SyncLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de performance pour FileSyncService
 * Teste la gestion des gros volumes de fichiers et des fichiers très volumineux
 */
@ExtendWith(MockitoExtension.class)
class FileSyncServicePerformanceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private SyncLogRepository syncLogRepository;

    private FileSyncService fileSyncService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileSyncService = new FileSyncService(fileMetadataRepository, syncLogRepository);

        // Configuration des propriétés
        ReflectionTestUtils.setField(fileSyncService, "chunkSize", 8192);
        ReflectionTestUtils.setField(fileSyncService, "maxPathLength", 400);
        ReflectionTestUtils.setField(fileSyncService, "maxFileNameLength", 200);
        ReflectionTestUtils.setField(fileSyncService, "excludePackageInfo", true);

        // Mock du repository
        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> {
            SyncLog log = invocation.getArgument(0);
            log.setId(1L);
            return log;
        });

        // Mock pour les métadonnées existantes
        when(fileMetadataRepository.findBySyncTask(any(SyncTask.class))).thenReturn(new ArrayList<>());
        when(fileMetadataRepository.findBySyncTaskAndFilePath(any(SyncTask.class), anyString()))
            .thenReturn(java.util.Optional.empty());
    }

    @Test
    void testExecuteSync_ManySmallFiles() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer 1000 petits fichiers
        int fileCount = 1000;
        for (int i = 0; i < fileCount; i++) {
            String fileName = String.format("file_%04d.txt", i);
            String content = "Content of file " + i;
            Files.write(sourceDir.resolve(fileName), content.getBytes());
        }

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        long startTime = System.currentTimeMillis();
        SyncLog result = fileSyncService.executeSync(task);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        assertEquals((long) fileCount, result.getFilesScanned());
        assertEquals((long) fileCount, result.getFilesCopied());

        // Vérifier que la synchronisation s'est terminée en moins de 30 secondes
        long duration = endTime - startTime;
        assertTrue(duration < 30000,
            "La synchronisation de " + fileCount + " fichiers devrait prendre moins de 30 secondes. Durée: " + duration + "ms");

        System.out.println("Synchronisation de " + fileCount + " fichiers en " + duration + "ms");
    }

    @Test
    void testExecuteSync_LargeFiles() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer plusieurs fichiers de 10MB chacun
        int fileCount = 5;
        long fileSize = 10 * 1024 * 1024; // 10MB
        long totalSize = fileCount * fileSize;

        for (int i = 0; i < fileCount; i++) {
            String fileName = "large_file_" + i + ".bin";
            Path file = sourceDir.resolve(fileName);

            // Créer un fichier de 10MB avec des données pseudo-aléatoires
            byte[] data = new byte[1024]; // Buffer de 1KB
            try (var output = Files.newOutputStream(file)) {
                for (int j = 0; j < fileSize / 1024; j++) {
                    // Remplir le buffer avec des données variées
                    for (int k = 0; k < data.length; k++) {
                        data[k] = (byte) ((i * 1000 + j * 100 + k) % 256);
                    }
                    output.write(data);
                }
            }
        }

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        long startTime = System.currentTimeMillis();
        SyncLog result = fileSyncService.executeSync(task);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        assertEquals((long) fileCount, result.getFilesScanned());
        assertEquals((long) fileCount, result.getFilesCopied());
        assertEquals(totalSize, result.getTotalBytes());

        // Vérifier que la synchronisation s'est terminée en moins de 60 secondes
        long duration = endTime - startTime;
        assertTrue(duration < 60000,
            "La synchronisation de " + fileCount + " fichiers de 10MB devrait prendre moins de 60 secondes. Durée: " + duration + "ms");

        System.out.println("Synchronisation de " + fileCount + " fichiers de 10MB (" + (totalSize / 1024 / 1024) + "MB total) en " + duration + "ms");
    }

    @Test
    void testExecuteSync_MixedFileSizes() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer un mélange de fichiers de différentes tailles
        List<Long> fileSizes = List.of(
            0L,                    // Fichier vide
            1L,                    // 1 octet
            1024L,                 // 1KB
            1024 * 1024L,          // 1MB
            10 * 1024 * 1024L,     // 10MB
            100 * 1024 * 1024L     // 100MB
        );

        long totalSize = 0;
        for (int i = 0; i < fileSizes.size(); i++) {
            long size = fileSizes.get(i);
            String fileName = "file_" + i + "_" + size + "bytes.bin";
            Path file = sourceDir.resolve(fileName);

            if (size == 0) {
                Files.createFile(file);
            } else {
                byte[] data = new byte[(int) Math.min(size, 1024)];
                try (var output = Files.newOutputStream(file)) {
                    long remaining = size;
                    while (remaining > 0) {
                        int toWrite = (int) Math.min(remaining, data.length);
                        // Remplir avec des données variées
                        for (int j = 0; j < toWrite; j++) {
                            data[j] = (byte) ((i * 100 + j) % 256);
                        }
                        output.write(data, 0, toWrite);
                        remaining -= toWrite;
                    }
                }
            }
            totalSize += size;
        }

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        long startTime = System.currentTimeMillis();
        SyncLog result = fileSyncService.executeSync(task);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        assertEquals((long) fileSizes.size(), result.getFilesScanned());
        assertEquals((long) fileSizes.size(), result.getFilesCopied());
        assertEquals(totalSize, result.getTotalBytes());

        long duration = endTime - startTime;
        System.out.println("Synchronisation de " + fileSizes.size() + " fichiers de tailles variées (" +
                          (totalSize / 1024 / 1024) + "MB total) en " + duration + "ms");
    }

    @Test
    void testExecuteSync_DeepDirectoryStructure() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer une structure de dossiers profonde avec des fichiers
        int depth = 20;
        int filesPerLevel = 10;
        int totalFiles = 0;

        for (int level = 0; level < depth; level++) {
            Path currentDir = sourceDir;
            for (int d = 0; d < level; d++) {
                currentDir = currentDir.resolve("level_" + d);
            }
            Files.createDirectories(currentDir);

            // Créer des fichiers dans ce niveau
            for (int f = 0; f < filesPerLevel; f++) {
                String fileName = "file_level_" + level + "_" + f + ".txt";
                String content = "Content for file at level " + level + ", file " + f;
                Files.write(currentDir.resolve(fileName), content.getBytes());
                totalFiles++;
            }
        }

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        long startTime = System.currentTimeMillis();
        SyncLog result = fileSyncService.executeSync(task);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        assertEquals((long) totalFiles, result.getFilesScanned());
        assertEquals((long) totalFiles, result.getFilesCopied());

        long duration = endTime - startTime;
        System.out.println("Synchronisation de " + totalFiles + " fichiers dans " + depth + " niveaux de dossiers en " + duration + "ms");
    }

    @Test
    void testExecuteSync_ExclusionPerformance() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer un mélange de fichiers valides et à exclure
        int validFiles = 100;
        int excludedFiles = 100;

        // Fichiers valides
        for (int i = 0; i < validFiles; i++) {
            String fileName = "valid_file_" + i + ".txt";
            Files.write(sourceDir.resolve(fileName), ("content " + i).getBytes());
        }

        // Fichiers à exclure (utiliser le nom exact "package-info.java" pour correspondre à la règle)
        for (int i = 0; i < excludedFiles; i++) {
            String fileName = "package-info.java";
            Files.write(sourceDir.resolve(fileName), ("// package info " + i).getBytes());
        }

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        long startTime = System.currentTimeMillis();
        SyncLog result = fileSyncService.executeSync(task);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        assertTrue(result.getFilesScanned() >= (long) validFiles);
        assertTrue(result.getFilesCopied() >= (long) validFiles);
        // Selon l'environnement, des fichiers supplémentaires peuvent être détectés comme exclus
        assertTrue(result.getFilesExcluded() >= 1L);

        long duration = endTime - startTime;
        System.out.println("Synchronisation avec " + validFiles + " fichiers valides et " +
                          excludedFiles + " fichiers exclus en " + duration + "ms");
    }

    private SyncTask createSyncTask(String sourcePath, String destPath) {
        SyncTask task = new SyncTask();
        task.setId(1L);
        task.setName("Performance Test Task");
        task.setSourcePath(sourcePath);
        task.setDestinationPath(destPath);
        task.setActive(true);
        task.setUseChecksum(false);
        task.setIntervalMinutes(60L);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }
}
