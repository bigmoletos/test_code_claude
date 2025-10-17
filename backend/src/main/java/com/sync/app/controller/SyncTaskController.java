package com.sync.app.controller;

import com.sync.app.dto.SyncTaskDto;
import com.sync.app.entity.SyncTask;
import com.sync.app.service.SyncTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour gérer les tâches de synchronisation.
 */
@RestController
@RequestMapping("/api/sync-tasks")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class SyncTaskController {

    private final SyncTaskService syncTaskService;

    @GetMapping
    public ResponseEntity<List<SyncTask>> getAllTasks() {
        return ResponseEntity.ok(syncTaskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SyncTask> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(syncTaskService.getTaskById(id));
    }

    @PostMapping
    public ResponseEntity<SyncTask> createTask(@Valid @RequestBody SyncTaskDto dto) {
        SyncTask task = syncTaskService.createTask(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SyncTask> updateTask(@PathVariable Long id, @Valid @RequestBody SyncTaskDto dto) {
        return ResponseEntity.ok(syncTaskService.updateTask(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        syncTaskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<SyncTask> toggleTaskStatus(@PathVariable Long id) {
        syncTaskService.toggleTaskStatus(id);
        return ResponseEntity.ok(syncTaskService.getTaskById(id));
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<Map<String, String>> triggerSync(@PathVariable Long id) {
        syncTaskService.triggerSync(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Synchronisation démarrée");
        response.put("taskId", id.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Boolean>> getSyncStatus(@PathVariable Long id) {
        boolean running = syncTaskService.isSyncRunning(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("running", running);
        return ResponseEntity.ok(response);
    }
}
