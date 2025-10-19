package com.sync.app.service;

import com.sync.app.entity.SyncTask;
import com.sync.app.repository.SyncTaskRepository;
import com.sync.app.util.CustomLogger;
import com.sync.app.util.CustomLogger.LogLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour gérer la planification automatique des synchronisations.
 */
@Service
@RequiredArgsConstructor
public class SyncSchedulerService {
    private static final CustomLogger logger = CustomLogger.getLogger("SyncSchedulerService");

    static {
        logger.setLevel(LogLevel.INFO)
              .setConsoleOutput(true)
              .setLogFile("./logs/sync-scheduler.log")
              .setMaxFileSize(10 * 1024 * 1024) // 10MB
              .setMaxBackupFiles(5)
              .setCompressionEnabled(true);
    }

    private final SyncTaskRepository syncTaskRepository;
    private final FileSyncService fileSyncService;

    /**
     * Vérifie toutes les minutes si des tâches doivent être exécutées.
     */
    @Scheduled(fixedRate = 60000) // Toutes les minutes
    @Transactional
    public void checkAndExecutePendingTasks() {
        logger.debug("Vérification des tâches de synchronisation planifiées...");

        List<SyncTask> tasks = syncTaskRepository
            .findByActiveTrueAndNextSyncTimeBefore(LocalDateTime.now());

        if (!tasks.isEmpty()) {
            logger.info("Trouvé {} tâche(s) en attente d'exécution", tasks.size());
        }

        for (SyncTask task : tasks) {
            if (!fileSyncService.isSyncRunning(task.getId())) {
                logger.info("Démarrage de la synchronisation planifiée pour: {} (ID: {})", task.getName(), task.getId());

                // Exécution asynchrone pour ne pas bloquer le scheduler
                executeTaskAsync(task);
            } else {
                logger.warn("Synchronisation déjà en cours pour: {} - tâche ignorée", task.getName());
            }
        }
    }

    /**
     * Exécute une tâche de manière asynchrone et met à jour les timestamps.
     */
    private void executeTaskAsync(SyncTask task) {
        final Long taskId = task.getId();
        final String taskName = task.getName();
        final Long intervalMinutes = task.getIntervalMinutes();

        logger.debug("Création thread asynchrone pour tâche: {}", taskName);

        new Thread(() -> {
            try {
                logger.info("Thread de synchronisation démarré pour: {}", taskName);

                // Récupérer à nouveau la tâche dans le contexte du nouveau thread
                SyncTask taskInThread = syncTaskRepository.findById(taskId).orElse(null);
                if (taskInThread == null) {
                    logger.error("Tâche ID {} introuvable dans le thread d'exécution", taskId);
                    return;
                }

                fileSyncService.executeSync(taskInThread);

                // Mise à jour des timestamps
                LocalDateTime now = LocalDateTime.now();
                taskInThread.setLastSyncTime(now);
                LocalDateTime nextSync = now.plusMinutes(intervalMinutes);
                taskInThread.setNextSyncTime(nextSync);
                syncTaskRepository.save(taskInThread);

                logger.info("Synchronisation terminée avec succès pour: {} - Prochaine exécution: {}",
                    taskName, nextSync);
            } catch (Exception e) {
                logger.error("Erreur lors de l'exécution asynchrone de la tâche: {}", taskName, e);
            }
        }).start();
    }
}
