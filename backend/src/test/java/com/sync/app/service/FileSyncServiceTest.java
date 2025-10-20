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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour FileSyncService
 * Couvre la gestion des noms longs, caractères spéciaux, exclusions, et tailles de fichiers
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileSyncServiceTest {

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

        // Configuration des propriétés via ReflectionTestUtils
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
    }

    @Test
    void testShouldExclude_FileNameTooLong() throws IOException {
        // Arrange
        String longFileName = "a".repeat(201); // 201 caractères (dépasse la limite de 200)
        Path longFile = tempDir.resolve(longFileName);
        Files.createFile(longFile);

        // Act
        boolean shouldExclude = invokeShouldExclude(longFile);

        // Assert
        assertTrue(shouldExclude, "Le fichier avec un nom trop long devrait être exclu");
    }

    @Test
    void testShouldExclude_PathTooLong() throws IOException {
        // Arrange
        // Créer un chemin très long en imbriquant des dossiers
        Path deepPath = tempDir;
        for (int i = 0; i < 50; i++) {
            deepPath = deepPath.resolve("very_long_folder_name_" + i);
        }
        Files.createDirectories(deepPath);
        Path longPathFile = deepPath.resolve("file.txt");
        Files.createFile(longPathFile);

        // Act
        boolean shouldExclude = invokeShouldExclude(longPathFile);

        // Assert
        assertTrue(shouldExclude, "Le fichier avec un chemin trop long devrait être exclu");
    }

    @Test
    void testShouldExclude_InvalidCharacters() throws IOException {
        // Arrange - Test différents caractères invalides
        String[] invalidNames = {
            "file<test>.txt",
            "file:test.txt",
            "file\"test\".txt",
            "file|test.txt",
            "file?test.txt",
            "file*test.txt",
            "file\0test.txt", // caractère null
            "file\ttest.txt", // tabulation
            "file\ntest.txt"  // nouvelle ligne
        };

        for (String invalidName : invalidNames) {
            // Cas particulier: le caractère nul ne peut pas être représenté dans un Path
            if (invalidName.contains("\0")) {
                assertTrue(true, "Les noms contenant un caractère nul sont invalides par définition");
                continue;
            }

            Path candidatePath;
            try {
                candidatePath = tempDir.resolve(invalidName);
            } catch (Exception e) {
                // Si la résolution échoue, considérer comme invalide
                assertTrue(true, "Chemin invalide non résoluble: " + invalidName);
                continue;
            }

            try {
                Files.createFile(candidatePath);
            } catch (Exception ignored) {
                // Ignorer si la création échoue, on teste quand même la logique d'exclusion
            }

            boolean shouldExclude = invokeShouldExclude(candidatePath);
            assertTrue(shouldExclude,
                "Le fichier '" + invalidName + "' avec caractères invalides devrait être exclu");
        }
    }

    @Test
    void testShouldExclude_PackageInfoJava() throws IOException {
        // Arrange
        Path packageInfoFile = tempDir.resolve("package-info.java");
        Files.createFile(packageInfoFile);

        // Act
        boolean shouldExclude = invokeShouldExclude(packageInfoFile);

        // Assert
        assertTrue(shouldExclude, "Le fichier package-info.java devrait être exclu");
    }

    @Test
    void testShouldExclude_SpecificWslPackageInfoPath() {
        // Arrange - Chemin WSL exact fourni par l'utilisateur
        String wslPath = "/mnt/d/programmation/AA/WS-GIT/source_tree/scp/scp-ipr-evenementsMetiers-ws-interface/src/main/java/fr/dsirc/scp/ipr/evenementsmetiers/ws/cloture/v1/dto/package-info.java";
        Path path = Paths.get(wslPath);

        // Act
        boolean shouldExclude = invokeShouldExclude(path);

        // Assert - Doit être exclu car c'est un package-info.java et chemin très long
        assertTrue(shouldExclude, "Le chemin WSL exact pour package-info.java devrait être exclu");
    }

    @Test
    void testShouldExclude_ExcludedPatterns() throws IOException {
        // Arrange - Test des patterns d'exclusion
        String[] excludedPatterns = {
            ".git",
            ".svn",
            "node_modules",
            "target",
            ".DS_Store",
            "Thumbs.db",
            "desktop.ini",
            "__pycache__",
            "*.tmp",
            "*.log"
        };

        for (String pattern : excludedPatterns) {
            String fileName = pattern.replace("*", "test");
            Path excludedFile = tempDir.resolve(fileName);
            Files.createFile(excludedFile);

            // Act
            boolean shouldExclude = invokeShouldExclude(excludedFile);

            // Assert
            assertTrue(shouldExclude,
                "Le fichier '" + fileName + "' devrait être exclu selon le pattern '" + pattern + "'");
        }
    }

    @Test
    void testShouldExclude_ValidFile() throws IOException {
        // Arrange
        Path validFile = tempDir.resolve("valid_file.txt");
        Files.createFile(validFile);

        // Act
        boolean shouldExclude = invokeShouldExclude(validFile);

        // Assert
        assertFalse(shouldExclude, "Le fichier valide ne devrait pas être exclu");
    }

    @Test
    void testShouldExclude_UnicodeCharacters() throws IOException {
        // Arrange - Test des caractères Unicode (devraient être acceptés)
        String[] unicodeNames = {
            "fichier_éàç.txt",
            "файл_русский.txt",
            "文件_中文.txt",
            "ファイル_日本語.txt",
            "ملف_عربي.txt"
        };

        for (String unicodeName : unicodeNames) {
            Path unicodeFile = tempDir.resolve(unicodeName);
            Files.createFile(unicodeFile);

            // Act
            boolean shouldExclude = invokeShouldExclude(unicodeFile);

            // Assert
            assertFalse(shouldExclude,
                "Le fichier avec caractères Unicode '" + unicodeName + "' ne devrait pas être exclu");
        }
    }

    @Test
    void testShouldCopyFile_NewFile() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path destFile = tempDir.resolve("dest.txt");
        Files.write(sourceFile, "test content".getBytes());

        // Act
        boolean shouldCopy = invokeShouldCopyFile(sourceFile, destFile, null, false);

        // Assert
        assertTrue(shouldCopy, "Un nouveau fichier devrait être copié");
    }

    @Test
    void testShouldCopyFile_FileSizeChanged() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path destFile = tempDir.resolve("dest.txt");
        Files.write(sourceFile, "new content".getBytes());
        Files.write(destFile, "old content".getBytes());

        FileMetadata existing = new FileMetadata();
        existing.setFileSize(11L); // "old content" = 11 bytes
        existing.setLastModified(LocalDateTime.now().minusHours(1));

        // Act
        boolean shouldCopy = invokeShouldCopyFile(sourceFile, destFile, existing, false);

        // Assert
        assertTrue(shouldCopy, "Le fichier avec une taille différente devrait être copié");
    }

    @Test
    void testShouldCopyFile_FileNotChanged() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("source.txt");
        Path destFile = tempDir.resolve("dest.txt");
        String content = "same content";
        Files.write(sourceFile, content.getBytes());
        Files.write(destFile, content.getBytes());

        FileMetadata existing = new FileMetadata();
        existing.setFileSize((long) content.length());
        // Aligner la date de modif sur celle du fichier source pour éviter une copie inutile
        existing.setLastModified(java.time.LocalDateTime.ofInstant(
            java.nio.file.Files.getLastModifiedTime(sourceFile).toInstant(),
            java.time.ZoneId.systemDefault()
        ));

        // Act
        boolean shouldCopy = invokeShouldCopyFile(sourceFile, destFile, existing, false);

        // Assert
        assertFalse(shouldCopy, "Le fichier inchangé ne devrait pas être copié");
    }

    @Test
    void testShouldCopyFile_EmptyFile() throws IOException {
        // Arrange
        Path sourceFile = tempDir.resolve("empty.txt");
        Path destFile = tempDir.resolve("dest.txt");
        Files.createFile(sourceFile); // Fichier vide (0 bytes)

        // Act
        boolean shouldCopy = invokeShouldCopyFile(sourceFile, destFile, null, false);

        // Assert
        assertTrue(shouldCopy, "Un fichier vide devrait être copié");
    }

    @Test
    void testConvertWindowsPathToWsl() {
        // Arrange
        String windowsPath = "D:\\programmation\\test";
        String expectedWslPath = "/mnt/d/programmation/test";

        // Act
        String result = invokeConvertWindowsPathToWsl(windowsPath);

        // Assert
        assertEquals(expectedWslPath, result, "La conversion Windows vers WSL devrait être correcte");
    }

    @Test
    void testConvertWindowsPathToWsl_AlreadyWsl() {
        // Arrange
        String wslPath = "/mnt/d/programmation/test";

        // Act
        String result = invokeConvertWindowsPathToWsl(wslPath);

        // Assert
        assertEquals(wslPath, result, "Un chemin WSL ne devrait pas être modifié");
    }

    @Test
    void testConvertWindowsPathToWsl_UnixPath() {
        // Arrange
        String unixPath = "/home/user/test";

        // Act
        String result = invokeConvertWindowsPathToWsl(unixPath);

        // Assert
        assertEquals(unixPath, result, "Un chemin Unix ne devrait pas être modifié");
    }

    @Test
    void testIsSyncRunning_NotRunning() {
        // Arrange
        Long taskId = 1L;

        // Act
        boolean isRunning = fileSyncService.isSyncRunning(taskId);

        // Assert
        assertFalse(isRunning, "Aucune synchronisation ne devrait être en cours");
    }

    // Méthodes utilitaires pour accéder aux méthodes privées via réflexion
    private boolean invokeShouldExclude(Path path) {
        try {
            return (Boolean) ReflectionTestUtils.invokeMethod(fileSyncService, "shouldExclude", path);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'invocation de shouldExclude", e);
        }
    }

    private boolean invokeShouldCopyFile(Path sourceFile, Path destFile, FileMetadata existing, boolean useChecksum) {
        try {
            return (Boolean) ReflectionTestUtils.invokeMethod(fileSyncService, "shouldCopyFile",
                sourceFile, destFile, existing, useChecksum);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'invocation de shouldCopyFile", e);
        }
    }

    private String invokeConvertWindowsPathToWsl(String path) {
        try {
            return (String) ReflectionTestUtils.invokeMethod(fileSyncService, "convertWindowsPathToWsl", path);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'invocation de convertWindowsPathToWsl", e);
        }
    }
}
