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
        new Thread(() -> {
            try {
                fileSyncService.executeSync(task);

                // Mise à jour des timestamps
                task.setLastSyncTime(LocalDateTime.now());
                task.setNextSyncTime(LocalDateTime.now().plusMinutes(task.getIntervalMinutes()));
                syncTaskRepository.save(task);

                log.info("Synchronisation terminée pour: {}", task.getName());
            } catch (Exception e) {
                log.error("Erreur lors de l'exécution de la tâche: {}", task.getName(), e);
            }
        }).start();
    }
}
