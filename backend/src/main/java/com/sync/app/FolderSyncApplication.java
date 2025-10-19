package com.sync.app;

import com.sync.app.util.CustomLogger;
import com.sync.app.util.CustomLogger.LogLevel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FolderSyncApplication {
    private static final CustomLogger logger = CustomLogger.getLogger("FolderSyncApplication");

    static {
        // Configuration du logger
        logger.setLevel(LogLevel.INFO)
              .setConsoleOutput(true)
              .setLogFile("./logs/folder-sync-app.log")
              .setMaxFileSize(10 * 1024 * 1024) // 10MB
              .setMaxBackupFiles(5)
              .setCompressionEnabled(true);
    }

    public static void main(String[] args) {
        logger.info("===========================================");
        logger.info("Démarrage de l'application Folder Sync");
        logger.info("Version: 1.0.0");
        logger.info("Java Version: " + System.getProperty("java.version"));
        logger.info("===========================================");

        try {
            SpringApplication.run(FolderSyncApplication.class, args);
            logger.info("Application démarrée avec succès");
        } catch (Exception e) {
            logger.fatal("Erreur fatale lors du démarrage de l'application", e);
            System.exit(1);
        }
    }
}
