package com.sync.app.service;

import com.sync.app.dto.SyncTaskDto;
import com.sync.app.entity.SyncTask;
import com.sync.app.repository.FileMetadataRepository;
import com.sync.app.repository.SyncLogRepository;
import com.sync.app.repository.SyncTaskRepository;
import com.sync.app.util.CustomLogger;
import com.sync.app.util.CustomLogger.LogLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour gérer les opérations CRUD sur les tâches de synchronisation.
 */
@Service
@RequiredArgsConstructor
public class SyncTaskService {
    private static final CustomLogger logger = CustomLogger.getLogger("SyncTaskService");

    static {
        logger.setLevel(LogLevel.INFO)
              .setConsoleOutput(true)
              .setLogFile("./logs/sync-task-service.log")
              .setMaxFileSize(10 * 1024 * 1024) // 10MB
              .setMaxBackupFiles(5)
              .setCompressionEnabled(true);
    }

    private final SyncTaskRepository syncTaskRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final SyncLogRepository syncLogRepository;
    private final FileSyncService fileSyncService;

    public List<SyncTask> getAllTasks() {
        logger.debug("Récupération de toutes les tâches de synchronisation");
        List<SyncTask> tasks = syncTaskRepository.findAll();
        logger.info("Trouvé {} tâche(s) de synchronisation", tasks.size());
        return tasks;
    }

    public SyncTask getTaskById(Long id) {
        logger.debug("Recherche de la tâche ID: {}", id);
        return syncTaskRepository.findById(id)
            .orElseThrow(() -> {
                logger.error("Tâche non trouvée avec l'ID: {}", id);
                return new RuntimeException("Tâche non trouvée: " + id);
            });
    }

    @Transactional
    public SyncTask createTask(SyncTaskDto dto) {
        logger.info("Création d'une nouvelle tâche de synchronisation: {}", dto.getName());
        logger.debug("Source: {} -> Destination: {}", dto.getSourcePath(), dto.getDestinationPath());
        logger.debug("Intervalle: {} minutes, Checksum: {}", dto.getIntervalMinutes(), dto.getUseChecksum());

        SyncTask task = new SyncTask();
        task.setName(dto.getName());
        task.setSourcePath(dto.getSourcePath());
        task.setDestinationPath(dto.getDestinationPath());
        task.setIntervalMinutes(dto.getIntervalMinutes());
        task.setActive(dto.getActive() != null ? dto.getActive() : true);
        task.setUseChecksum(dto.getUseChecksum() != null ? dto.getUseChecksum() : true);
        task.setNextSyncTime(LocalDateTime.now());

        SyncTask savedTask = syncTaskRepository.save(task);
        logger.info("Tâche créée avec succès - ID: {}, Active: {}", savedTask.getId(), savedTask.getActive());
        return savedTask;
    }

    @Transactional
    public SyncTask updateTask(Long id, SyncTaskDto dto) {
        logger.info("Mise à jour de la tâche ID: {} avec le nom: {}", id, dto.getName());
        SyncTask task = getTaskById(id);

        logger.debug("Ancienne source: {} -> Nouvelle: {}", task.getSourcePath(), dto.getSourcePath());
        logger.debug("Ancienne destination: {} -> Nouvelle: {}", task.getDestinationPath(), dto.getDestinationPath());

        task.setName(dto.getName());
        task.setSourcePath(dto.getSourcePath());
        task.setDestinationPath(dto.getDestinationPath());
        task.setIntervalMinutes(dto.getIntervalMinutes());

        if (dto.getActive() != null) {
            task.setActive(dto.getActive());
        }
        if (dto.getUseChecksum() != null) {
            task.setUseChecksum(dto.getUseChecksum());
        }

        SyncTask updatedTask = syncTaskRepository.save(task);
        logger.info("Tâche ID: {} mise à jour avec succès", id);
        return updatedTask;
    }

    @Transactional
    public void deleteTask(Long id) {
        logger.warn("Suppression de la tâche ID: {}", id);
        SyncTask task = getTaskById(id);
        logger.info("Suppression de la tâche: {} (Source: {})", task.getName(), task.getSourcePath());

        // Supprimer d'abord les logs, puis les métadonnées, puis la tâche
        logger.debug("Suppression des logs associés à la tâche ID: {}", id);
        syncLogRepository.deleteBySyncTask(task);

        logger.debug("Suppression des métadonnées de fichiers associées à la tâche ID: {}", id);
        fileMetadataRepository.deleteBySyncTask(task);

        logger.debug("Suppression de la tâche elle-même ID: {}", id);
        syncTaskRepository.delete(task);

        logger.info("Tâche ID: {} supprimée avec succès", id);
    }

    @Transactional
    public void toggleTaskStatus(Long id) {
        logger.info("Basculement du statut de la tâche ID: {}", id);
        SyncTask task = getTaskById(id);
        boolean newStatus = !task.getActive();
        task.setActive(newStatus);
        syncTaskRepository.save(task);
        logger.info("Tâche '{}' (ID: {}) maintenant: {}", task.getName(), id, newStatus ? "ACTIVE" : "INACTIVE");
    }

    /**
     * Déclenche manuellement une synchronisation.
     */
    public void triggerSync(Long id) {
        logger.info("Déclenchement manuel de la synchronisation pour la tâche ID: {}", id);
        SyncTask task = getTaskById(id);

        if (fileSyncService.isSyncRunning(id)) {
            logger.warn("Synchronisation déjà en cours pour la tâche '{}' (ID: {}) - demande rejetée", task.getName(), id);
            throw new RuntimeException("Une synchronisation est déjà en cours pour cette tâche");
        }

        logger.info("Lancement de la synchronisation manuelle pour: {}", task.getName());

        // Exécution en arrière-plan
        final Long taskId = id;
        final String taskName = task.getName();
        new Thread(() -> {
            try {
                logger.info("Thread manuel démarré pour tâche: {}", taskName);

                // Récupérer à nouveau la tâche dans le contexte du nouveau thread
                SyncTask taskInThread = syncTaskRepository.findById(taskId).orElse(null);
                if (taskInThread != null) {
                    fileSyncService.executeSync(taskInThread);
                    taskInThread.setLastSyncTime(LocalDateTime.now());
                    taskInThread.setNextSyncTime(LocalDateTime.now().plusMinutes(taskInThread.getIntervalMinutes()));
                    syncTaskRepository.save(taskInThread);
                    logger.info("Synchronisation manuelle terminée pour: {}", taskName);
                } else {
                    logger.error("Tâche ID {} non trouvée dans le thread manuel", taskId);
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la synchronisation manuelle de: {}", taskName, e);
            }
        }).start();
    }

    /**
     * Vérifie le statut d'une synchronisation.
     */
    public boolean isSyncRunning(Long id) {
        logger.debug("Vérification du statut de synchronisation pour la tâche ID: {}", id);
        return fileSyncService.isSyncRunning(id);
    }
}
