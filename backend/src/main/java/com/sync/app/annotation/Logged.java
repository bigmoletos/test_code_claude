package com.sync.app.annotation;

import com.sync.app.util.CustomLogger.LogLevel;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour activer le logging automatique sur une classe.
 *
 * Exemple d'utilisation:
 * <pre>
 * @Logged(level = LogLevel.DEBUG, logFile = "./logs/my-service.log")
 * public class MyService {
 *     // Le logger sera automatiquement injecté
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Logged {

    /**
     * Niveau de log par défaut
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * Chemin du fichier de log
     */
    String logFile() default "./logs/application.log";

    /**
     * Taille maximale du fichier en octets (0 = pas de limite)
     */
    long maxFileSize() default 10485760; // 10MB

    /**
     * Nombre maximal de fichiers de backup
     */
    int maxBackupFiles() default 5;

    /**
     * Activer la compression GZIP
     */
    boolean compression() default true;

    /**
     * Activer la sortie console
     */
    boolean console() default true;
}

