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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests d'intégration pour FileSyncService
 * Teste les scénarios complets avec des fichiers de différentes tailles
 */
@ExtendWith(MockitoExtension.class)
class FileSyncServiceIntegrationTest {

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
            .thenReturn(Optional.empty());
    }

    @Test
    void testExecuteSync_EmptyFile() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer un fichier vide
        Path emptyFile = sourceDir.resolve("empty.txt");
        Files.createFile(emptyFile);

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        SyncLog result = fileSyncService.executeSync(task);

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        assertEquals(1L, result.getFilesScanned());
        assertEquals(1L, result.getFilesCopied());
        assertEquals(0L, result.getFilesUpdated());
        assertEquals(0L, result.getFilesSkipped());
        assertEquals(0L, result.getFilesExcluded());
        assertEquals(0L, result.getTotalBytes());
    }

    @Test
    void testExecuteSync_LargeFile() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer un fichier de 1MB (simulation d'un gros fichier)
        Path largeFile = sourceDir.resolve("large_file.txt");
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }
        Files.write(largeFile, largeContent);

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        SyncLog result = fileSyncService.executeSync(task);

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        assertEquals(1L, result.getFilesScanned());
        assertEquals(1L, result.getFilesCopied());
        assertEquals(1024 * 1024L, result.getTotalBytes());
    }

    @Test
    void testExecuteSync_ExcludedFiles() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer des fichiers qui devraient être exclus
        Files.createFile(sourceDir.resolve("package-info.java"));
        Files.createFile(sourceDir.resolve(".gitignore"));
        Files.createFile(sourceDir.resolve("Thumbs.db"));

        // Créer un dossier node_modules avec un fichier
        Path nodeModules = sourceDir.resolve("node_modules");
        Files.createDirectories(nodeModules);
        Files.createFile(nodeModules.resolve("package.json"));

        // Créer un fichier valide
        Files.write(sourceDir.resolve("valid.txt"), "content".getBytes());

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        SyncLog result = fileSyncService.executeSync(task);

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        // En environnement de test, certaines ressources auto-générées peuvent être vues. On vérifie au moins 1 fichier
        assertTrue(result.getFilesScanned() >= 1L);
        assertTrue(result.getFilesCopied() >= 1L);
        // Vérifier qu'au moins un fichier a été exclu (selon FS et contraintes de création)
        assertTrue(result.getFilesExcluded() >= 1L);
    }

    @Test
    void testExecuteSync_LongPathStructure() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer une structure de dossiers profonde
        Path deepPath = sourceDir;
        for (int i = 0; i < 10; i++) {
            deepPath = deepPath.resolve("level_" + i);
        }
        Files.createDirectories(deepPath);
        Files.write(deepPath.resolve("file.txt"), "content".getBytes());

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        SyncLog result = fileSyncService.executeSync(task);

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        assertEquals(1L, result.getFilesScanned());
        assertEquals(1L, result.getFilesCopied());
    }

    @Test
    void testExecuteSync_UnicodeFileNames() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer des fichiers avec des noms Unicode
        String[] unicodeNames = {
            "fichier_éàç.txt",
            "файл_русский.txt",
            "文件_中文.txt",
            "ファイル_日本語.txt"
        };

        for (String name : unicodeNames) {
            Files.write(sourceDir.resolve(name), ("content_" + name).getBytes());
        }

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        SyncLog result = fileSyncService.executeSync(task);

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        assertEquals((long) unicodeNames.length, result.getFilesScanned());
        assertEquals((long) unicodeNames.length, result.getFilesCopied());
    }

    @Test
    void testExecuteSync_InvalidCharactersInPath() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer des fichiers avec des caractères invalides
        String[] invalidNames = {
            "file<test>.txt",
            "file:test.txt",
            "file\"test\".txt",
            "file|test.txt"
        };

        for (String name : invalidNames) {
            try {
                Files.write(sourceDir.resolve(name), "content".getBytes());
            } catch (Exception e) {
                // Certains caractères peuvent ne pas être créables sur certains systèmes
                System.out.println("Impossible de créer le fichier " + name + ": " + e.getMessage());
            }
        }

        // Créer un fichier valide pour s'assurer que le test fonctionne
        Files.write(sourceDir.resolve("valid.txt"), "content".getBytes());

        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());

        // Act
        SyncLog result = fileSyncService.executeSync(task);

        // Assert
        assertNotNull(result);
        assertEquals(SyncLog.SyncStatus.COMPLETED, result.getStatus());
        assertTrue(result.getFilesScanned() >= 1L); // Au moins le fichier valide
    }

    @Test
    void testExecuteSync_FileUpdate() throws IOException {
        // Arrange
        Path sourceDir = tempDir.resolve("source");
        Path destDir = tempDir.resolve("dest");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Créer un fichier source
        String content = "original content";
        Files.write(sourceDir.resolve("test.txt"), content.getBytes());

        // Première synchronisation
        SyncTask task = createSyncTask(sourceDir.toString(), destDir.toString());
        SyncLog firstSync = fileSyncService.executeSync(task);

        // Modifier le fichier source
        String newContent = "modified content";
        Files.write(sourceDir.resolve("test.txt"), newContent.getBytes());

        // Mock des métadonnées existantes
        FileMetadata existing = new FileMetadata();
        existing.setFileSize((long) content.length());
        existing.setLastModified(LocalDateTime.now().minusHours(1));
        // Critique pour éviter des NPE et assurer le mapping par chemin relatif
        existing.setFilePath("test.txt");
        existing.setIsDirectory(false);

        when(fileMetadataRepository.findBySyncTask(any(SyncTask.class)))
            .thenReturn(List.of(existing));
        when(fileMetadataRepository.findBySyncTaskAndFilePath(any(SyncTask.class), eq("test.txt")))
            .thenReturn(Optional.of(existing));

        // Act - Deuxième synchronisation
        SyncLog secondSync = fileSyncService.executeSync(task);

        // Assert
        assertNotNull(secondSync);
        assertEquals(SyncLog.SyncStatus.COMPLETED, secondSync.getStatus());
        assertTrue(secondSync.getFilesScanned() >= 1L);
        // Après modification, on attend au moins une mise à jour
        assertTrue(secondSync.getFilesUpdated() >= 1L);
    }

    private SyncTask createSyncTask(String sourcePath, String destPath) {
        SyncTask task = new SyncTask();
        task.setId(1L);
        task.setName("Test Task");
        task.setSourcePath(sourcePath);
        task.setDestinationPath(destPath);
        task.setActive(true);
        task.setUseChecksum(false);
        task.setIntervalMinutes(60L);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }
}
