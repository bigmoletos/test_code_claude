package com.sync.app.controller;

import com.sync.app.dto.SyncLogDto;
import com.sync.app.entity.SyncLog;
import com.sync.app.entity.SyncTask;
import com.sync.app.repository.SyncLogRepository;
import com.sync.app.repository.SyncTaskRepository;
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

    private final SyncLogRepository syncLogRepository;
    private final SyncTaskRepository syncTaskRepository;

    @GetMapping
    public ResponseEntity<Page<SyncLogDto>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<SyncLog> logs = syncLogRepository.findAllByOrderByStartTimeDesc(pageable);

        Page<SyncLogDto> dtos = logs.map(this::convertToDto);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<Page<SyncLogDto>> getLogsByTask(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        SyncTask task = syncTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Tâche non trouvée: " + taskId));

        Pageable pageable = PageRequest.of(page, size);
        Page<SyncLog> logs = syncLogRepository.findBySyncTaskOrderByStartTimeDesc(task, pageable);

        Page<SyncLogDto> dtos = logs.map(this::convertToDto);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SyncLogDto> getLogById(@PathVariable Long id) {
        SyncLog log = syncLogRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Log non trouvé: " + id));

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
