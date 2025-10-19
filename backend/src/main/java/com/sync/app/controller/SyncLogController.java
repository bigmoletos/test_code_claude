package com.sync.app.controller;

import com.sync.app.dto.SyncLogDto;
import com.sync.app.entity.SyncLog;
import com.sync.app.entity.SyncTask;
import com.sync.app.repository.SyncLogRepository;
import com.sync.app.repository.SyncTaskRepository;
import com.sync.app.util.CustomLogger;
import com.sync.app.util.CustomLogger.LogLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour gérer les logs de synchronisation.
 */
@RestController
@RequestMapping("/api/sync-logs")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class SyncLogController {
    private static final CustomLogger logger = CustomLogger.getLogger("SyncLogController");

    static {
        logger.setLevel(LogLevel.INFO)
              .setConsoleOutput(true)
              .setLogFile("./logs/sync-log-controller.log")
              .setMaxFileSize(10 * 1024 * 1024) // 10MB
              .setMaxBackupFiles(5)
              .setCompressionEnabled(true);
    }

    private final SyncLogRepository syncLogRepository;
    private final SyncTaskRepository syncTaskRepository;

    @GetMapping
    public ResponseEntity<Page<SyncLogDto>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("API GET /api/sync-logs - Récupération des logs (page={}, size={})", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<SyncLog> logs = syncLogRepository.findAllByOrderByStartTimeDesc(pageable);

        Page<SyncLogDto> dtos = logs.map(this::convertToDto);
        logger.debug("Retour de {} logs sur {} total", dtos.getNumberOfElements(), dtos.getTotalElements());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<Page<SyncLogDto>> getLogsByTask(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("API GET /api/sync-logs/task/{} - Récupération des logs pour la tâche (page={}, size={})",
            taskId, page, size);

        SyncTask task = syncTaskRepository.findById(taskId)
            .orElseThrow(() -> {
                logger.error("Tâche ID {} non trouvée lors de la récupération des logs", taskId);
                return new RuntimeException("Tâche non trouvée: " + taskId);
            });

        Pageable pageable = PageRequest.of(page, size);
        Page<SyncLog> logs = syncLogRepository.findBySyncTaskOrderByStartTimeDesc(task, pageable);

        Page<SyncLogDto> dtos = logs.map(this::convertToDto);
        logger.debug("Retour de {} logs pour la tâche '{}' sur {} total",
            dtos.getNumberOfElements(), task.getName(), dtos.getTotalElements());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SyncLogDto> getLogById(@PathVariable Long id) {
        logger.info("API GET /api/sync-logs/{} - Récupération d'un log spécifique", id);
        SyncLog log = syncLogRepository.findById(id)
            .orElseThrow(() -> {
                logger.error("Log ID {} non trouvé", id);
                return new RuntimeException("Log non trouvé: " + id);
            });

        logger.debug("Log trouvé - Tâche: {}, Statut: {}", log.getSyncTask().getName(), log.getStatus());
        return ResponseEntity.ok(convertToDto(log));
    }

    private SyncLogDto convertToDto(SyncLog log) {
        SyncLogDto dto = new SyncLogDto();
        dto.setId(log.getId());
        dto.setSyncTaskId(log.getSyncTask().getId());
        dto.setSyncTaskName(log.getSyncTask().getName());
        dto.setStartTime(log.getStartTime());
        dto.setEndTime(log.getEndTime());
        dto.setStatus(log.getStatus());
        dto.setFilesScanned(log.getFilesScanned());
        dto.setFilesCopied(log.getFilesCopied());
        dto.setFilesUpdated(log.getFilesUpdated());
        dto.setFilesDeleted(log.getFilesDeleted());
        dto.setFilesSkipped(log.getFilesSkipped());
        dto.setTotalBytes(log.getTotalBytes());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setDetails(log.getDetails());
        return dto;
    }
}
