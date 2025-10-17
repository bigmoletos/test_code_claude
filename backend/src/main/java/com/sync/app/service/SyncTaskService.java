package com.sync.app.service;

import com.sync.app.dto.SyncTaskDto;
import com.sync.app.entity.SyncTask;
import com.sync.app.repository.FileMetadataRepository;
import com.sync.app.repository.SyncTaskRepository;
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

    private final SyncTaskRepository syncTaskRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileSyncService fileSyncService;

    public List<SyncTask> getAllTasks() {
        return syncTaskRepository.findAll();
    }

    public SyncTask getTaskById(Long id) {
        return syncTaskRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tâche non trouvée: " + id));
    }

    @Transactional
    public SyncTask createTask(SyncTaskDto dto) {
        SyncTask task = new SyncTask();
        task.setName(dto.getName());
        task.setSourcePath(dto.getSourcePath());
        task.setDestinationPath(dto.getDestinationPath());
        task.setIntervalMinutes(dto.getIntervalMinutes());
        task.setActive(dto.getActive() != null ? dto.getActive() : true);
        task.setUseChecksum(dto.getUseChecksum() != null ? dto.getUseChecksum() : true);
        task.setNextSyncTime(LocalDateTime.now());

        return syncTaskRepository.save(task);
    }

    @Transactional
    public SyncTask updateTask(Long id, SyncTaskDto dto) {
        SyncTask task = getTaskById(id);

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

        return syncTaskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        SyncTask task = getTaskById(id);
        fileMetadataRepository.deleteBySyncTask(task);
        syncTaskRepository.delete(task);
    }

    @Transactional
    public void toggleTaskStatus(Long id) {
        SyncTask task = getTaskById(id);
        task.setActive(!task.getActive());
        syncTaskRepository.save(task);
    }

    /**
     * Déclenche manuellement une synchronisation.
     */
    public void triggerSync(Long id) {
        SyncTask task = getTaskById(id);

        if (fileSyncService.isSyncRunning(id)) {
            throw new RuntimeException("Une synchronisation est déjà en cours pour cette tâche");
        }

        // Exécution en arrière-plan
        new Thread(() -> {
            fileSyncService.executeSync(task);
            task.setLastSyncTime(LocalDateTime.now());
            task.setNextSyncTime(LocalDateTime.now().plusMinutes(task.getIntervalMinutes()));
            syncTaskRepository.save(task);
        }).start();
    }

    /**
     * Vérifie le statut d'une synchronisation.
     */
    public boolean isSyncRunning(Long id) {
        return fileSyncService.isSyncRunning(id);
    }
}
