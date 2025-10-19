package com.sync.app.example;

import com.sync.app.annotation.Logged;
import com.sync.app.annotation.LogExecution;
import com.sync.app.util.CustomLogger;
import com.sync.app.util.CustomLogger.LogLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * EXEMPLE : Service simplifié utilisant Lombok et annotations personnalisées
 * 
 * Avantages par rapport à l'ancienne approche:
 * 
 * ❌ AVANT (verbose):
 * <pre>
 * private static final CustomLogger logger = CustomLogger.getLogger("MyService");
 * 
 * static {
 *     logger.setLevel(LogLevel.INFO)
 *           .setConsoleOutput(true)
 *           .setLogFile("./logs/my-service.log")
 *           .setMaxFileSize(10 * 1024 * 1024)
 *           .setMaxBackupFiles(5)
 *           .setCompressionEnabled(true);
 * }
 * 
 * public void myMethod(String param) {
 *     logger.info("Début de myMethod avec param: {}", param);
 *     try {
 *         // code...
 *         logger.info("Fin de myMethod");
 *     } catch (Exception e) {
 *         logger.error("Erreur dans myMethod", e);
 *         throw e;
 *     }
 * }
 * </pre>
 * 
 * ✅ APRÈS (concis):
 * <pre>
 * @Logged(level = LogLevel.INFO, logFile = "./logs/my-service.log")
 * @Service
 * @RequiredArgsConstructor
 * public class MyService {
 *     
 *     @LogExecution
 *     public void myMethod(String param) {
 *         // code...
 *     }
 * }
 * </pre>
 */
@Service
@Logged(
    level = LogLevel.DEBUG,
    logFile = "./logs/simplified-service.log",
    maxFileSize = 10 * 1024 * 1024, // 10MB
    maxBackupFiles = 5,
    compression = true
)
@RequiredArgsConstructor
public class SimplifiedService {
    
    // Lombok génère automatiquement le constructeur avec les dépendances
    // private final SomeRepository repository;
    
    /**
     * Méthode avec logging automatique complet
     * L'annotation @LogExecution génère automatiquement:
     * - Log de l'entrée avec les paramètres
     * - Log de la sortie avec le résultat
     * - Log du temps d'exécution
     * - Log des exceptions
     */
    @LogExecution(
        level = LogLevel.DEBUG,
        logParams = true,
        logResult = true,
        logTime = true,
        slowThreshold = 1000
    )
    public String processData(String input, int count) {
        // Votre logique métier ici
        // Plus besoin de logs manuels !
        return "Processed: " + input + " x" + count;
    }
    
    /**
     * Pour les cas où vous avez besoin de logs manuels spécifiques,
     * vous pouvez toujours utiliser le logger directement
     */
    private static final CustomLogger logger = CustomLogger.getLogger("SimplifiedService");
    
    public void complexMethod() {
        logger.info("Début de traitement complexe");
        
        // Logique métier avec logs spécifiques
        logger.debug("Étape 1 terminée");
        logger.debug("Étape 2 terminée");
        
        logger.info("Traitement complexe terminé");
    }
}

/**
 * COMPARAISON DES APPROCHES:
 * 
 * 📊 Réduction de code:
 * - Ancienne approche: ~30 lignes de boilerplate pour le logger
 * - Nouvelle approche: 1 ligne d'annotation
 * 
 * ✨ Avantages:
 * 1. Code plus lisible et maintenable
 * 2. Configuration centralisée via annotations
 * 3. Logging automatique des méthodes (AOP)
 * 4. Moins d'erreurs (pas d'oubli de log)
 * 5. Mesure automatique des performances
 * 6. Détection automatique des méthodes lentes
 * 
 * 🎯 Quand utiliser quoi:
 * - @Logged sur la classe → Configuration globale du logger
 * - @LogExecution sur les méthodes → Logging automatique complet
 * - Logger manuel → Logs spécifiques au métier
 */

