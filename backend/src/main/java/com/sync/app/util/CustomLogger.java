package com.sync.app.util;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * Logger générique thread-safe avec gestion avancée des niveaux de log et rotation automatique.
 *
 * Niveaux de log disponibles (par ordre de sévérité):
 * - TRACE: Information très détaillée pour le debugging
 * - DEBUG: Information de debugging
 * - INFO: Information générale
 * - WARN: Avertissements
 * - ERROR: Erreurs
 * - FATAL: Erreurs critiques
 *
 * Fonctionnalités de rotation:
 * - Rotation par taille de fichier (ex: 10MB)
 * - Suppression des logs anciens par nombre de fichiers
 * - Suppression par taille totale du dossier
 * - Gestion du pourcentage d'espace disque
 * - Compression automatique des logs archivés (.gz)
 *
 * Exemple d'utilisation:
 * <pre>
 * CustomLogger logger = CustomLogger.getLogger("MonApplication");
 * logger.setLevel(LogLevel.INFO)
 *       .setLogFile("./logs/app.log")
 *       .setMaxFileSize(10 * 1024 * 1024)  // 10MB
 *       .setMaxBackupFiles(5)
 *       .setCompressionEnabled(true);
 * logger.info("Application démarrée");
 * </pre>
 *
 * @author Claude Code
 * @version 2.0
 */
public class CustomLogger {

    /**
     * Énumération des niveaux de log
     */
    public enum LogLevel {
        TRACE(0, "TRACE"),
        DEBUG(1, "DEBUG"),
        INFO(2, "INFO"),
        WARN(3, "WARN"),
        ERROR(4, "ERROR"),
        FATAL(5, "FATAL");

        private final int priority;
        private final String label;

        LogLevel(int priority, String label) {
            this.priority = priority;
            this.label = label;
        }

        public int getPriority() {
            return priority;
        }

        public String getLabel() {
            return label;
        }
    }

    // Configuration de base
    private String loggerName;
    private LogLevel currentLevel;
    private boolean consoleOutput;
    private String logFilePath;
    private SimpleDateFormat dateFormat;
    private final ReentrantReadWriteLock lock;
    private boolean includeStackTrace;

    // Configuration de rotation
    private long maxFileSize;              // Taille max d'un fichier (0 = pas de limite)
    private int maxBackupFiles;            // Nombre max de fichiers de backup (0 = illimité)
    private long totalSizeCap;             // Taille totale max du dossier logs (0 = pas de limite)
    private int maxAgeDays;                // Age max des logs en jours (0 = pas de limite)
    private boolean compressionEnabled;    // Compression des fichiers archivés
    private double maxDiskUsagePercent;    // % max d'utilisation du disque (0 = désactivé)

    // Valeurs par défaut (inspirées de Log4j2/Logback)
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int DEFAULT_MAX_BACKUP_FILES = 7;
    private static final long DEFAULT_TOTAL_SIZE_CAP = 100 * 1024 * 1024; // 100MB
    private static final int DEFAULT_MAX_AGE_DAYS = 30;

    /**
     * Constructeur privé - utilisez getLogger() pour créer une instance
     */
    private CustomLogger(String name) {
        this.loggerName = name;
        this.currentLevel = LogLevel.INFO;
        this.consoleOutput = true;
        this.logFilePath = null;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        this.lock = new ReentrantReadWriteLock();
        this.includeStackTrace = false;

        // Configuration par défaut de rotation
        this.maxFileSize = 0;  // Désactivé par défaut
        this.maxBackupFiles = 0;
        this.totalSizeCap = 0;
        this.maxAgeDays = 0;
        this.compressionEnabled = false;
        this.maxDiskUsagePercent = 0;
    }

    /**
     * Obtient une instance de logger pour le nom spécifié
     */
    public static CustomLogger getLogger(String name) {
        return new CustomLogger(name);
    }

    /**
     * Obtient une instance de logger pour la classe spécifiée
     */
    public static CustomLogger getLogger(Class<?> clazz) {
        return new CustomLogger(clazz.getSimpleName());
    }

    // ==================== CONFIGURATION DE BASE ====================

    public CustomLogger setLevel(LogLevel level) {
        lock.writeLock().lock();
        try {
            this.currentLevel = level;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public CustomLogger setConsoleOutput(boolean enabled) {
        lock.writeLock().lock();
        try {
            this.consoleOutput = enabled;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public CustomLogger setLogFile(String filePath) {
        lock.writeLock().lock();
        try {
            this.logFilePath = filePath;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public CustomLogger setDateFormat(String pattern) {
        lock.writeLock().lock();
        try {
            this.dateFormat = new SimpleDateFormat(pattern);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public CustomLogger setIncludeStackTrace(boolean include) {
        lock.writeLock().lock();
        try {
            this.includeStackTrace = include;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== CONFIGURATION DE ROTATION ====================

    /**
     * Définit la taille maximale d'un fichier de log avant rotation
     *
     * @param maxSize Taille en octets (0 = pas de limite)
     * @return Cette instance pour chaînage
     */
    public CustomLogger setMaxFileSize(long maxSize) {
        lock.writeLock().lock();
        try {
            this.maxFileSize = maxSize;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Définit le nombre maximum de fichiers de backup à conserver
     *
     * @param maxFiles Nombre de fichiers (0 = illimité)
     * @return Cette instance pour chaînage
     */
    public CustomLogger setMaxBackupFiles(int maxFiles) {
        lock.writeLock().lock();
        try {
            this.maxBackupFiles = maxFiles;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Définit la taille totale maximale du dossier de logs
     *
     * @param totalSize Taille en octets (0 = pas de limite)
     * @return Cette instance pour chaînage
     */
    public CustomLogger setTotalSizeCap(long totalSize) {
        lock.writeLock().lock();
        try {
            this.totalSizeCap = totalSize;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Définit l'âge maximum des logs en jours
     *
     * @param days Nombre de jours (0 = pas de limite)
     * @return Cette instance pour chaînage
     */
    public CustomLogger setMaxAgeDays(int days) {
        lock.writeLock().lock();
        try {
            this.maxAgeDays = days;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Active ou désactive la compression des fichiers archivés
     *
     * @param enabled true pour compresser en .gz
     * @return Cette instance pour chaînage
     */
    public CustomLogger setCompressionEnabled(boolean enabled) {
        lock.writeLock().lock();
        try {
            this.compressionEnabled = enabled;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Définit le pourcentage maximum d'utilisation du disque pour les logs
     *
     * @param percent Pourcentage (0-100, 0 = désactivé)
     * @return Cette instance pour chaînage
     */
    public CustomLogger setMaxDiskUsagePercent(double percent) {
        lock.writeLock().lock();
        try {
            if (percent < 0 || percent > 100) {
                throw new IllegalArgumentException("Le pourcentage doit être entre 0 et 100");
            }
            this.maxDiskUsagePercent = percent;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Active la rotation avec les valeurs par défaut recommandées
     * - Taille max par fichier: 10MB
     * - Nombre de backups: 7
     * - Taille totale: 100MB
     * - Age max: 30 jours
     * - Compression: activée
     *
     * @return Cette instance pour chaînage
     */
    public CustomLogger enableDefaultRotation() {
        lock.writeLock().lock();
        try {
            this.maxFileSize = DEFAULT_MAX_FILE_SIZE;
            this.maxBackupFiles = DEFAULT_MAX_BACKUP_FILES;
            this.totalSizeCap = DEFAULT_TOTAL_SIZE_CAP;
            this.maxAgeDays = DEFAULT_MAX_AGE_DAYS;
            this.compressionEnabled = true;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== MÉTHODES DE LOGGING ====================

    public void trace(String message) {
        log(LogLevel.TRACE, message, null);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    public void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }

    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }

    public void fatal(String message) {
        log(LogLevel.FATAL, message, null);
    }

    public void fatal(String message, Throwable throwable) {
        log(LogLevel.FATAL, message, throwable);
    }

    public boolean isLevelEnabled(LogLevel level) {
        lock.readLock().lock();
        try {
            return level.getPriority() >= currentLevel.getPriority();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Méthode principale de logging
     */
    private void log(LogLevel level, String message, Throwable throwable) {
        if (!isLevelEnabled(level)) {
            return;
        }

        lock.readLock().lock();
        try {
            String formattedMessage = formatMessage(level, message, throwable);

            if (consoleOutput) {
                writeToConsole(level, formattedMessage);
            }

            if (logFilePath != null) {
                writeToFileWithRotation(formattedMessage);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== FORMATAGE ====================

    private String formatMessage(LogLevel level, String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();

        // Timestamp
        sb.append("[").append(dateFormat.format(new Date())).append("]");

        // Niveau
        sb.append(" [").append(level.getLabel()).append("]");

        // Thread
        sb.append(" [").append(Thread.currentThread().getName()).append("]");

        // Logger name
        sb.append(" [").append(loggerName).append("]");

        // Message
        sb.append(" - ").append(message);

        // Exception si présente
        if (throwable != null) {
            sb.append("\n").append(getStackTraceAsString(throwable));
        } else if (includeStackTrace) {
            sb.append("\n").append(getCurrentStackTrace());
        }

        return sb.toString();
    }

    // ==================== SORTIE CONSOLE ====================

    private void writeToConsole(LogLevel level, String message) {
        String colorCode = getColorCode(level);
        String resetCode = "\u001B[0m";

        if (level.getPriority() >= LogLevel.ERROR.getPriority()) {
            System.err.println(colorCode + message + resetCode);
        } else {
            System.out.println(colorCode + message + resetCode);
        }
    }

    private String getColorCode(LogLevel level) {
        switch (level) {
            case TRACE:
                return "\u001B[37m"; // Blanc
            case DEBUG:
                return "\u001B[36m"; // Cyan
            case INFO:
                return "\u001B[32m"; // Vert
            case WARN:
                return "\u001B[33m"; // Jaune
            case ERROR:
                return "\u001B[31m"; // Rouge
            case FATAL:
                return "\u001B[35m"; // Magenta
            default:
                return "";
        }
    }

    // ==================== ÉCRITURE FICHIER AVEC ROTATION ====================

    /**
     * Écrit dans le fichier avec gestion de la rotation
     */
    private void writeToFileWithRotation(String message) {
        try {
            File logFile = new File(logFilePath);

            // Créer le dossier parent si nécessaire
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Vérifier si rotation nécessaire
            if (shouldRotate(logFile)) {
                rotateLogFile(logFile);
            }

            // Écrire le message
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(message);
            }

            // Nettoyer les anciens fichiers après écriture
            cleanupOldLogFiles();

        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture dans le fichier de log: " + e.getMessage());
        }
    }

    /**
     * Vérifie si une rotation est nécessaire
     */
    private boolean shouldRotate(File logFile) {
        if (!logFile.exists()) {
            return false;
        }

        // Rotation par taille
        if (maxFileSize > 0 && logFile.length() >= maxFileSize) {
            return true;
        }

        // Vérification de l'espace disque
        if (maxDiskUsagePercent > 0) {
            File partition = logFile.getParentFile();
            if (partition != null) {
                long totalSpace = partition.getTotalSpace();
                long freeSpace = partition.getFreeSpace();
                long usedSpace = totalSpace - freeSpace;
                double usagePercent = (double) usedSpace / totalSpace * 100;

                // Si on approche de la limite, forcer la rotation
                if (usagePercent > (100 - maxDiskUsagePercent)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Effectue la rotation du fichier de log
     */
    private void rotateLogFile(File currentFile) throws IOException {
        String basePath = logFilePath;
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

        // Générer le nom du fichier archivé
        int dotIndex = basePath.lastIndexOf('.');
        String nameWithoutExt = (dotIndex > 0) ? basePath.substring(0, dotIndex) : basePath;
        String extension = (dotIndex > 0) ? basePath.substring(dotIndex) : "";

        String archivedFileName = nameWithoutExt + "_" + timestamp + extension;

        // Copier le fichier actuel vers l'archive
        File archivedFile = new File(archivedFileName);
        Files.copy(currentFile.toPath(), archivedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Compresser si activé
        if (compressionEnabled) {
            compressFile(archivedFile);
            archivedFile.delete(); // Supprimer le fichier non compressé
        }

        // Vider le fichier actuel
        new PrintWriter(currentFile).close();
    }

    /**
     * Compresse un fichier en .gz
     */
    private void compressFile(File file) throws IOException {
        File gzFile = new File(file.getAbsolutePath() + ".gz");

        try (FileInputStream fis = new FileInputStream(file);
             FileOutputStream fos = new FileOutputStream(gzFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, len);
            }
        }
    }

    /**
     * Nettoie les anciens fichiers de log selon les règles configurées
     */
    private void cleanupOldLogFiles() {
        try {
            File logFile = new File(logFilePath);
            File parentDir = logFile.getParentFile();
            if (parentDir == null || !parentDir.exists()) {
                return;
            }

            String baseFileName = logFile.getName();
            int dotIndex = baseFileName.lastIndexOf('.');
            String namePattern = (dotIndex > 0) ? baseFileName.substring(0, dotIndex) : baseFileName;

            // Récupérer tous les fichiers de log archivés
            List<File> archivedFiles = Arrays.stream(parentDir.listFiles())
                .filter(f -> f.getName().startsWith(namePattern + "_"))
                .sorted(Comparator.comparingLong(File::lastModified))
                .collect(Collectors.toList());

            if (archivedFiles.isEmpty()) {
                return;
            }

            // 1. Supprimer par nombre de fichiers
            if (maxBackupFiles > 0 && archivedFiles.size() > maxBackupFiles) {
                int toDelete = archivedFiles.size() - maxBackupFiles;
                for (int i = 0; i < toDelete; i++) {
                    archivedFiles.get(i).delete();
                }
                archivedFiles = archivedFiles.subList(toDelete, archivedFiles.size());
            }

            // 2. Supprimer par âge
            if (maxAgeDays > 0) {
                long maxAgeMillis = System.currentTimeMillis() - (maxAgeDays * 24L * 60 * 60 * 1000);
                archivedFiles.removeIf(file -> {
                    if (file.lastModified() < maxAgeMillis) {
                        file.delete();
                        return true;
                    }
                    return false;
                });
            }

            // 3. Supprimer par taille totale
            if (totalSizeCap > 0) {
                long totalSize = archivedFiles.stream().mapToLong(File::length).sum();
                int index = 0;
                while (totalSize > totalSizeCap && index < archivedFiles.size()) {
                    File fileToDelete = archivedFiles.get(index);
                    totalSize -= fileToDelete.length();
                    fileToDelete.delete();
                    index++;
                }
            }

            // 4. Vérifier le pourcentage d'espace disque
            if (maxDiskUsagePercent > 0) {
                long freeSpace = parentDir.getFreeSpace();
                long totalSpace = parentDir.getTotalSpace();
                long allowedLogSpace = (long) (totalSpace * maxDiskUsagePercent / 100.0);
                long currentLogSize = archivedFiles.stream().mapToLong(File::length).sum();

                // Si on dépasse, supprimer les plus anciens
                int index = 0;
                while (currentLogSize > allowedLogSpace && index < archivedFiles.size()) {
                    File fileToDelete = archivedFiles.get(index);
                    currentLogSize -= fileToDelete.length();
                    fileToDelete.delete();
                    index++;
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du nettoyage des logs: " + e.getMessage());
        }
    }

    // ==================== UTILITAIRES ====================

    private String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage());

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\n\tat ").append(element.toString());
        }

        if (throwable.getCause() != null) {
            sb.append("\nCaused by: ").append(getStackTraceAsString(throwable.getCause()));
        }

        return sb.toString();
    }

    private String getCurrentStackTrace() {
        StringBuilder sb = new StringBuilder("Stack trace:");
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();

        for (int i = 4; i < elements.length; i++) {
            sb.append("\n\tat ").append(elements[i].toString());
        }

        return sb.toString();
    }

    // ==================== GETTERS ====================

    public LogLevel getLevel() {
        lock.readLock().lock();
        try {
            return currentLevel;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getName() {
        return loggerName;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public int getMaxBackupFiles() {
        return maxBackupFiles;
    }

    public long getTotalSizeCap() {
        return totalSizeCap;
    }

    public int getMaxAgeDays() {
        return maxAgeDays;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public double getMaxDiskUsagePercent() {
        return maxDiskUsagePercent;
    }
}
