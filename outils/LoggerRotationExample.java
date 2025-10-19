package outils;

import outils.CustomLogger;
import outils.CustomLogger.LogLevel;

/**
 * Exemples d'utilisation de la rotation de logs avec CustomLogger
 *
 * Démontre:
 * - Rotation par taille de fichier
 * - Suppression par nombre de fichiers
 * - Suppression par ancienneté
 * - Gestion de la taille totale
 * - Gestion du pourcentage d'espace disque
 * - Compression automatique
 */
public class LoggerRotationExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Démonstration de la rotation de logs ===\n");

        // Exemple 1: Rotation basique par taille
        exemple1_RotationParTaille();

        // Exemple 2: Configuration complète avec toutes les options
        exemple2_ConfigurationComplete();

        // Exemple 3: Utilisation des valeurs par défaut
        exemple3_ValeursParDefaut();

        // Exemple 4: Gestion du pourcentage d'espace disque
        exemple4_PourcentageEspaceDisque();

        // Exemple 5: Configuration pour production
        exemple5_ConfigurationProduction();

        // Exemple 6: Test de rotation intensive
        exemple6_TestRotationIntensive();
    }

    /**
     * Exemple 1: Rotation simple par taille de fichier
     */
    private static void exemple1_RotationParTaille() {
        System.out.println("\n--- Exemple 1: Rotation par taille (100KB) ---");

        CustomLogger logger = CustomLogger.getLogger("RotationTaille");
        logger.setLevel(LogLevel.INFO)
              .setLogFile("./logs/size-rotation.log")
              .setMaxFileSize(100 * 1024)  // 100KB
              .setMaxBackupFiles(3);        // Garder 3 backups

        logger.info("Logger configuré pour rotation à 100KB, max 3 backups");

        // Simuler beaucoup de logs pour déclencher la rotation
        for (int i = 1; i <= 50; i++) {
            logger.info("Message numéro " + i + " - " + generateLongMessage());
        }

        logger.info("Rotation terminée. Vérifiez le dossier ./logs/");
        System.out.println("Fichiers créés: size-rotation.log et jusqu'à 3 archives");
    }

    /**
     * Exemple 2: Configuration complète
     */
    private static void exemple2_ConfigurationComplete() {
        System.out.println("\n--- Exemple 2: Configuration complète ---");

        CustomLogger logger = CustomLogger.getLogger("CompletConfig");
        logger.setLevel(LogLevel.DEBUG)
              .setLogFile("./logs/complete.log")
              .setMaxFileSize(50 * 1024)          // 50KB par fichier
              .setMaxBackupFiles(5)                // Maximum 5 backups
              .setTotalSizeCap(200 * 1024)         // 200KB total
              .setMaxAgeDays(7)                    // Supprimer après 7 jours
              .setCompressionEnabled(true);        // Compresser en .gz

        logger.info("Configuration:");
        logger.info("- Taille max/fichier: 50KB");
        logger.info("- Backups max: 5");
        logger.info("- Taille totale max: 200KB");
        logger.info("- Age max: 7 jours");
        logger.info("- Compression: activée (.gz)");

        for (int i = 1; i <= 30; i++) {
            logger.debug("Log debug #" + i + " - " + generateLongMessage());
        }

        logger.info("Vérifiez ./logs/ pour les fichiers .gz compressés");
    }

    /**
     * Exemple 3: Utilisation des valeurs par défaut recommandées
     */
    private static void exemple3_ValeursParDefaut() {
        System.out.println("\n--- Exemple 3: Valeurs par défaut (recommandées) ---");

        CustomLogger logger = CustomLogger.getLogger("DefaultConfig");
        logger.setLevel(LogLevel.INFO)
              .setLogFile("./logs/default-rotation.log")
              .enableDefaultRotation();  // Active config par défaut

        logger.info("Rotation activée avec valeurs par défaut:");
        logger.info("- Taille max/fichier: 10MB");
        logger.info("- Backups max: 7");
        logger.info("- Taille totale max: 100MB");
        logger.info("- Age max: 30 jours");
        logger.info("- Compression: activée");

        for (int i = 1; i <= 20; i++) {
            logger.info("Test message " + i);
        }

        System.out.println("Configuration idéale pour la plupart des applications");
    }

    /**
     * Exemple 4: Gestion du pourcentage d'espace disque
     */
    private static void exemple4_PourcentageEspaceDisque() {
        System.out.println("\n--- Exemple 4: Gestion pourcentage d'espace disque ---");

        CustomLogger logger = CustomLogger.getLogger("DiskPercent");
        logger.setLevel(LogLevel.INFO)
              .setLogFile("./logs/disk-percent.log")
              .setMaxFileSize(30 * 1024)           // 30KB
              .setMaxDiskUsagePercent(10.0);       // Max 10% du disque

        logger.info("Logger configuré pour utiliser max 10% de l'espace disque");
        logger.info("Si l'espace disque est faible, rotation forcée");

        // Afficher les informations disque
        java.io.File logDir = new java.io.File("./logs");
        if (logDir.exists()) {
            long totalSpace = logDir.getTotalSpace();
            long freeSpace = logDir.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            double usagePercent = (double) usedSpace / totalSpace * 100;

            logger.info("Espace disque total: " + formatBytes(totalSpace));
            logger.info("Espace utilisé: " + formatBytes(usedSpace) +
                       String.format(" (%.2f%%)", usagePercent));
            logger.info("Espace libre: " + formatBytes(freeSpace));
            logger.info("Limite pour logs: " + formatBytes((long)(totalSpace * 0.10)));
        }

        for (int i = 1; i <= 15; i++) {
            logger.info("Message #" + i + " - " + generateLongMessage());
        }

        System.out.println("Les logs plus anciens seront supprimés si nécessaire");
    }

    /**
     * Exemple 5: Configuration recommandée pour production
     */
    private static void exemple5_ConfigurationProduction() {
        System.out.println("\n--- Exemple 5: Configuration Production ---");

        CustomLogger logger = CustomLogger.getLogger("ProductionApp");
        logger.setLevel(LogLevel.WARN)  // En prod: WARN ou ERROR
              .setLogFile("./logs/production.log")
              .setMaxFileSize(20 * 1024 * 1024)    // 20MB par fichier
              .setMaxBackupFiles(10)                // 10 backups
              .setTotalSizeCap(300 * 1024 * 1024)   // 300MB total
              .setMaxAgeDays(90)                    // 90 jours
              .setMaxDiskUsagePercent(5.0)          // Max 5% disque
              .setCompressionEnabled(true)          // Compression
              .setConsoleOutput(false);             // Pas de console en prod

        logger.warn("Configuration PRODUCTION activée");
        logger.warn("Niveau: WARN (logs critiques uniquement)");
        logger.error("Test erreur en production");

        try {
            // Simulation d'erreur
            int result = 10 / 0;
        } catch (Exception e) {
            logger.error("Erreur critique en production", e);
        }

        logger.fatal("Erreur système fatale simulée");

        System.out.println("Configuration optimale pour environnement de production");
        System.out.println("- Niveau WARN/ERROR uniquement");
        System.out.println("- Pas de sortie console");
        System.out.println("- Rotation et compression activées");
        System.out.println("- Rétention 90 jours");
    }

    /**
     * Exemple 6: Test de rotation intensive
     */
    private static void exemple6_TestRotationIntensive() throws InterruptedException {
        System.out.println("\n--- Exemple 6: Test rotation intensive ---");

        CustomLogger logger = CustomLogger.getLogger("IntensiveTest");
        logger.setLevel(LogLevel.DEBUG)
              .setLogFile("./logs/intensive.log")
              .setMaxFileSize(20 * 1024)            // 20KB (petit pour test)
              .setMaxBackupFiles(3)
              .setCompressionEnabled(true);

        System.out.println("Génération intensive de logs...");

        for (int i = 1; i <= 100; i++) {
            logger.debug("Iteration " + i + " - " + generateLongMessage());

            if (i % 20 == 0) {
                System.out.println("  -> " + i + " messages générés...");
                Thread.sleep(100); // Pause pour voir la progression
            }
        }

        logger.info("Test intensif terminé - 100 messages générés");
        System.out.println("Vérifiez ./logs/ pour voir les rotations");
        System.out.println("Fichiers attendus: intensive.log + 3 archives .gz");
    }

    /**
     * Exemple d'utilisation dans une classe de service
     */
    static class ServiceWithRotation {
        private static final CustomLogger logger =
            CustomLogger.getLogger(ServiceWithRotation.class);

        static {
            // Configuration au chargement de la classe
            logger.setLevel(LogLevel.INFO)
                  .setLogFile("./logs/service.log")
                  .enableDefaultRotation();
        }

        public void processData(String data) {
            logger.info("Début traitement: " + data);

            try {
                // Traitement...
                for (int i = 0; i < 10; i++) {
                    logger.debug("Étape " + i);
                }
                logger.info("Traitement réussi");

            } catch (Exception e) {
                logger.error("Erreur traitement", e);
            }
        }
    }

    /**
     * Comparaison des différentes stratégies
     */
    static class StrategieComparison {
        public static void demonstrateStrategies() {
            System.out.println("\n=== Comparaison des stratégies de rotation ===\n");

            // Stratégie 1: Par taille uniquement
            System.out.println("Stratégie 1: Par TAILLE uniquement");
            System.out.println("  Avantages: Simple, prévisible");
            System.out.println("  Usage: Applications avec volume constant");
            System.out.println("  Config: setMaxFileSize(10MB) + setMaxBackupFiles(5)");

            // Stratégie 2: Par ancienneté uniquement
            System.out.println("\nStratégie 2: Par ANCIENNETÉ uniquement");
            System.out.println("  Avantages: Conformité réglementaire");
            System.out.println("  Usage: Audits, archivage légal");
            System.out.println("  Config: setMaxAgeDays(90)");

            // Stratégie 3: Par taille totale
            System.out.println("\nStratégie 3: Par TAILLE TOTALE");
            System.out.println("  Avantages: Contrôle strict espace disque");
            System.out.println("  Usage: Serveurs avec espace limité");
            System.out.println("  Config: setTotalSizeCap(500MB)");

            // Stratégie 4: Par pourcentage disque
            System.out.println("\nStratégie 4: Par POURCENTAGE DISQUE");
            System.out.println("  Avantages: S'adapte à la taille partition");
            System.out.println("  Usage: Environnements multi-tenant");
            System.out.println("  Config: setMaxDiskUsagePercent(5.0)");

            // Stratégie 5: Combinée (recommandée)
            System.out.println("\nStratégie 5: COMBINÉE (recommandée)");
            System.out.println("  Avantages: Protection multi-niveaux");
            System.out.println("  Usage: Production");
            System.out.println("  Config: enableDefaultRotation()");
            System.out.println("    -> Taille (10MB) + Nombre (7) + Total (100MB) + Age (30j)");
        }
    }

    // ==================== UTILITAIRES ====================

    private static String generateLongMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Données de test: ");
        for (int i = 0; i < 50; i++) {
            sb.append("ABCDEFGH ");
        }
        return sb.toString();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Guide de configuration selon le type d'application
     */
    static class ConfigurationGuide {
        public static void printGuide() {
            System.out.println("\n=== Guide de configuration selon le contexte ===\n");

            System.out.println("1. DÉVELOPPEMENT:");
            System.out.println("   logger.setLevel(LogLevel.DEBUG)");
            System.out.println("         .setLogFile(\"./logs/dev.log\")");
            System.out.println("         .setMaxFileSize(5 * 1024 * 1024)  // 5MB");
            System.out.println("         .setMaxBackupFiles(3)");
            System.out.println("         .setConsoleOutput(true);");

            System.out.println("\n2. TEST/STAGING:");
            System.out.println("   logger.setLevel(LogLevel.INFO)");
            System.out.println("         .setLogFile(\"./logs/staging.log\")");
            System.out.println("         .enableDefaultRotation();");

            System.out.println("\n3. PRODUCTION:");
            System.out.println("   logger.setLevel(LogLevel.WARN)");
            System.out.println("         .setLogFile(\"/var/log/app/prod.log\")");
            System.out.println("         .setMaxFileSize(20 * 1024 * 1024)  // 20MB");
            System.out.println("         .setMaxBackupFiles(10)");
            System.out.println("         .setTotalSizeCap(500 * 1024 * 1024)  // 500MB");
            System.out.println("         .setMaxAgeDays(90)");
            System.out.println("         .setMaxDiskUsagePercent(5.0)");
            System.out.println("         .setCompressionEnabled(true)");
            System.out.println("         .setConsoleOutput(false);");

            System.out.println("\n4. MICROSERVICE:");
            System.out.println("   logger.setLevel(LogLevel.INFO)");
            System.out.println("         .setLogFile(\"./logs/microservice.log\")");
            System.out.println("         .setMaxFileSize(10 * 1024 * 1024)  // 10MB");
            System.out.println("         .setMaxBackupFiles(5)");
            System.out.println("         .setTotalSizeCap(100 * 1024 * 1024)  // 100MB");
            System.out.println("         .setCompressionEnabled(true);");

            System.out.println("\n5. APPLICATION BATCH:");
            System.out.println("   logger.setLevel(LogLevel.DEBUG)");
            System.out.println("         .setLogFile(\"./logs/batch_\" + timestamp + \".log\")");
            System.out.println("         .setMaxFileSize(50 * 1024 * 1024)  // 50MB");
            System.out.println("         .setMaxAgeDays(30)");
            System.out.println("         .setCompressionEnabled(true);");
        }
    }
}
