package outils;

import outils.CustomLogger;
import outils.CustomLogger.LogLevel;

/**
 * Exemple d'utilisation du CustomLogger
 *
 * Ce fichier démontre les différentes fonctionnalités du logger:
 * - Différents niveaux de log
 * - Configuration du niveau minimum
 * - Sortie console et fichier
 * - Gestion des exceptions
 * - Personnalisation du format
 */
public class LoggerExample {

    public static void main(String[] args) {
        System.out.println("=== Démonstration du CustomLogger ===\n");

        // Exemple 1: Utilisation basique
        exemple1_UtilisationBasique();

        // Exemple 2: Avec fichier de log
        exemple2_AvecFichier();

        // Exemple 3: Gestion des exceptions
        exemple3_GestionExceptions();

        // Exemple 4: Niveaux de log
        exemple4_NiveauxDeLog();

        // Exemple 5: Logger par classe
        exemple5_LoggerParClasse();
    }

    /**
     * Exemple 1: Utilisation basique avec sortie console
     */
    private static void exemple1_UtilisationBasique() {
        System.out.println("\n--- Exemple 1: Utilisation basique ---");

        CustomLogger logger = CustomLogger.getLogger("ExempleBasique");
        logger.setLevel(LogLevel.DEBUG);

        logger.trace("Message TRACE (ne s'affichera pas car niveau = DEBUG)");
        logger.debug("Message DEBUG");
        logger.info("Application démarrée avec succès");
        logger.warn("Ceci est un avertissement");
        logger.error("Simulation d'une erreur");
        logger.fatal("Erreur critique système");
    }

    /**
     * Exemple 2: Logger avec sortie dans un fichier
     */
    private static void exemple2_AvecFichier() {
        System.out.println("\n--- Exemple 2: Log vers fichier ---");

        CustomLogger logger = CustomLogger.getLogger("ExempleFichier");
        logger.setLevel(LogLevel.INFO)
              .setLogFile("./logs/application.log")
              .setDateFormat("dd/MM/yyyy HH:mm:ss");

        logger.info("Log écrit dans la console ET dans le fichier");
        logger.warn("Attention: mémoire élevée");

        // Logger uniquement vers fichier (sans console)
        logger.setConsoleOutput(false);
        logger.info("Ce message n'apparaît que dans le fichier");
    }

    /**
     * Exemple 3: Gestion des exceptions
     */
    private static void exemple3_GestionExceptions() {
        System.out.println("\n--- Exemple 3: Gestion des exceptions ---");

        CustomLogger logger = CustomLogger.getLogger("ExempleException");
        logger.setLevel(LogLevel.DEBUG);

        try {
            // Simulation d'une erreur
            int result = 10 / 0;
        } catch (Exception e) {
            logger.error("Erreur lors du calcul", e);
        }

        try {
            // Simulation d'une autre erreur
            String test = null;
            test.length();
        } catch (Exception e) {
            logger.fatal("Erreur critique NullPointerException", e);
        }
    }

    /**
     * Exemple 4: Démonstration des différents niveaux
     */
    private static void exemple4_NiveauxDeLog() {
        System.out.println("\n--- Exemple 4: Niveaux de log ---");

        CustomLogger logger = CustomLogger.getLogger("ExempleNiveaux");

        // Test avec niveau WARN
        System.out.println("\nNiveau configuré: WARN");
        logger.setLevel(LogLevel.WARN);

        logger.trace("TRACE - ne s'affiche pas");
        logger.debug("DEBUG - ne s'affiche pas");
        logger.info("INFO - ne s'affiche pas");
        logger.warn("WARN - s'affiche");
        logger.error("ERROR - s'affiche");
        logger.fatal("FATAL - s'affiche");

        // Test avec niveau TRACE (tous les messages)
        System.out.println("\nNiveau configuré: TRACE");
        logger.setLevel(LogLevel.TRACE);

        logger.trace("TRACE - s'affiche");
        logger.debug("DEBUG - s'affiche");
        logger.info("INFO - s'affiche");
    }

    /**
     * Exemple 5: Logger par classe
     */
    private static void exemple5_LoggerParClasse() {
        System.out.println("\n--- Exemple 5: Logger par classe ---");

        // Logger avec nom de classe
        CustomLogger logger = CustomLogger.getLogger(LoggerExample.class);
        logger.setLevel(LogLevel.INFO);

        logger.info("Log depuis la classe " + LoggerExample.class.getSimpleName());

        // Vérifier si un niveau est activé
        if (logger.isLevelEnabled(LogLevel.DEBUG)) {
            logger.debug("Le niveau DEBUG est activé");
        } else {
            logger.info("Le niveau DEBUG n'est PAS activé (niveau actuel: " +
                       logger.getLevel().getLabel() + ")");
        }
    }

    /**
     * Exemple d'utilisation dans une classe métier
     */
    static class ServiceMetier {
        private static final CustomLogger logger = CustomLogger.getLogger(ServiceMetier.class);

        static {
            logger.setLevel(LogLevel.DEBUG)
                  .setLogFile("./logs/service.log");
        }

        public void traiterDonnees(String data) {
            logger.debug("Début du traitement des données: " + data);

            try {
                // Simulation du traitement
                if (data == null || data.isEmpty()) {
                    logger.warn("Données vides reçues");
                    return;
                }

                // Traitement...
                logger.info("Traitement réussi pour: " + data);

            } catch (Exception e) {
                logger.error("Erreur lors du traitement", e);
                throw e;
            } finally {
                logger.debug("Fin du traitement");
            }
        }
    }
}
