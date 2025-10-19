package com.sync.app.annotation;

import com.sync.app.util.CustomLogger.LogLevel;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour logger automatiquement l'exécution d'une méthode.
 * 
 * Exemple:
 * <pre>
 * @LogExecution(level = LogLevel.DEBUG, logParams = true, logResult = true)
 * public String myMethod(String param) {
 *     return "result";
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
    
    /**
     * Niveau de log
     */
    LogLevel level() default LogLevel.DEBUG;
    
    /**
     * Logger les paramètres d'entrée
     */
    boolean logParams() default true;
    
    /**
     * Logger le résultat de retour
     */
    boolean logResult() default true;
    
    /**
     * Logger le temps d'exécution
     */
    boolean logTime() default true;
    
    /**
     * Seuil d'avertissement pour le temps d'exécution (en ms)
     * Si le temps dépasse ce seuil, un warning est émis
     */
    long slowThreshold() default 1000;
}

