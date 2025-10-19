package com.sync.app.aspect;

import com.sync.app.annotation.Logged;
import com.sync.app.util.CustomLogger;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Aspect AOP pour le logging automatique des méthodes.
 *
 * Fonctionnalités:
 * - Log automatique de l'entrée/sortie des méthodes
 * - Log des paramètres et valeurs de retour
 * - Log des exceptions
 * - Mesure du temps d'exécution
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /**
     * Intercepte toutes les méthodes des classes annotées avec @Logged
     */
    @Around("@within(logged)")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint, Logged logged) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        CustomLogger logger = CustomLogger.getLogger(className);

        // Configuration du logger selon l'annotation
        logger.setLevel(logged.level())
              .setConsoleOutput(logged.console())
              .setLogFile(logged.logFile())
              .setMaxFileSize(logged.maxFileSize())
              .setMaxBackupFiles(logged.maxBackupFiles())
              .setCompressionEnabled(logged.compression());

        // Log des paramètres d'entrée
        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            logger.debug("→ Entrée {}.{}() avec paramètres: {}",
                className, methodName, Arrays.toString(args));
        } else {
            logger.debug("→ Entrée {}.{}()", className, methodName);
        }

        long startTime = System.currentTimeMillis();

        try {
            // Exécution de la méthode
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            // Log de la sortie
            if (result != null) {
                logger.debug("← Sortie {}.{}() = {} ({}ms)",
                    className, methodName, result, executionTime);
            } else {
                logger.debug("← Sortie {}.{}() ({}ms)",
                    className, methodName, executionTime);
            }

            // Avertissement si l'exécution est lente
            if (executionTime > 5000) {
                logger.warn("Méthode lente détectée: {}.{}() a pris {}ms",
                    className, methodName, executionTime);
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("✗ Exception dans {}.{}() après {}ms: {}",
                className, methodName, executionTime, e.getMessage(), e);
            throw e;
        }
    }
}

