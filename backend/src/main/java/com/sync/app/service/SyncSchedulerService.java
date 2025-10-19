package com.sync.app.service;

import com.sync.app.entity.SyncTask;
import com.sync.app.repository.SyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour gérer la planification automatique des synchronisations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SyncSchedulerService {

    private final SyncTaskRepository syncTaskRepository;
    private final FileSyncService fileSyncService;

    /**
     * Vérifie toutes les minutes si des tâches doivent être exécutées.
     */
    @Scheduled(fixedRate = 60000) // Toutes les minutes
    @Transactional
    public void checkAndExecutePendingTasks() {
        log.debug("Vérification des tâches de synchronisation...");

        List<SyncTask> tasks = syncTaskRepository
            .findByActiveTrueAndNextSyncTimeBefore(LocalDateTime.now());

        for (SyncTask task : tasks) {
            if (!fileSyncService.isSyncRunning(task.getId())) {
                log.info("Démarrage de la synchronisation pour: {}", task.getName());

                // Exécution asynchrone pour ne pas bloquer le scheduler
                executeTaskAsync(task);
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

        new Thread(() -> {
            try {
                // Récupérer à nouveau la tâche dans le contexte du nouveau thread
                SyncTask taskInThread = syncTaskRepository.findById(taskId).orElse(null);
                if (taskInThread == null) {
                    log.error("Tâche {} introuvable dans le thread d'exécution", taskId);
                    return;
                }

                fileSyncService.executeSync(taskInThread);

                // Mise à jour des timestamps
                taskInThread.setLastSyncTime(LocalDateTime.now());
                taskInThread.setNextSyncTime(LocalDateTime.now().plusMinutes(intervalMinutes));
                syncTaskRepository.save(taskInThread);

                log.info("Synchronisation terminée pour: {}", taskName);
            } catch (Exception e) {
                log.error("Erreur lors de l'exécution de la tâche: {}", taskName, e);
            }
        }).start();
    }
}
