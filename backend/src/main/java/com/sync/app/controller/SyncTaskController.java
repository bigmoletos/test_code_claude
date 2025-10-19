package com.sync.app.controller;

import com.sync.app.dto.SyncTaskDto;
import com.sync.app.entity.SyncTask;
import com.sync.app.service.SyncTaskService;
import com.sync.app.util.CustomLogger;
import com.sync.app.util.CustomLogger.LogLevel;
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
    private static final CustomLogger logger = CustomLogger.getLogger("SyncTaskController");

    static {
        logger.setLevel(LogLevel.INFO)
              .setConsoleOutput(true)
              .setLogFile("./logs/sync-task-controller.log")
              .setMaxFileSize(10 * 1024 * 1024) // 10MB
              .setMaxBackupFiles(5)
              .setCompressionEnabled(true);
    }

    private final SyncTaskService syncTaskService;

    @GetMapping
    public ResponseEntity<List<SyncTask>> getAllTasks() {
        logger.info("API GET /api/sync-tasks - Récupération de toutes les tâches");
        List<SyncTask> tasks = syncTaskService.getAllTasks();
        logger.debug("Retour de {} tâches", tasks.size());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SyncTask> getTaskById(@PathVariable Long id) {
        logger.info("API GET /api/sync-tasks/{} - Récupération d'une tâche", id);
        SyncTask task = syncTaskService.getTaskById(id);
        logger.debug("Tâche trouvée: {}", task.getName());
        return ResponseEntity.ok(task);
    }

    @PostMapping
    public ResponseEntity<SyncTask> createTask(@Valid @RequestBody SyncTaskDto dto) {
        logger.info("API POST /api/sync-tasks - Création d'une nouvelle tâche: {}", dto.getName());
        logger.debug("Données reçues: Source={}, Destination={}", dto.getSourcePath(), dto.getDestinationPath());
        SyncTask task = syncTaskService.createTask(dto);
        logger.info("Tâche créée avec succès - ID: {}", task.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SyncTask> updateTask(@PathVariable Long id, @Valid @RequestBody SyncTaskDto dto) {
        logger.info("API PUT /api/sync-tasks/{} - Mise à jour de la tâche", id);
        SyncTask updated = syncTaskService.updateTask(id, dto);
        logger.info("Tâche ID: {} mise à jour avec succès", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        logger.warn("API DELETE /api/sync-tasks/{} - Suppression de la tâche", id);
        syncTaskService.deleteTask(id);
        logger.info("Tâche ID: {} supprimée avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<SyncTask> toggleTaskStatus(@PathVariable Long id) {
        logger.info("API POST /api/sync-tasks/{}/toggle - Basculement du statut", id);
        syncTaskService.toggleTaskStatus(id);
        SyncTask updatedTask = syncTaskService.getTaskById(id);
        logger.info("Statut de la tâche ID: {} basculé vers: {}", id, updatedTask.getActive() ? "ACTIVE" : "INACTIVE");
        return ResponseEntity.ok(updatedTask);
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<Map<String, String>> triggerSync(@PathVariable Long id) {
        logger.info("API POST /api/sync-tasks/{}/trigger - Déclenchement manuel", id);
        try {
            syncTaskService.triggerSync(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Synchronisation démarrée");
            response.put("taskId", id.toString());
            logger.info("Synchronisation démarrée avec succès pour la tâche ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors du déclenchement de la synchronisation pour la tâche ID: {}", id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("taskId", id.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Boolean>> getSyncStatus(@PathVariable Long id) {
        logger.debug("API GET /api/sync-tasks/{}/status - Vérification du statut", id);
        boolean running = syncTaskService.isSyncRunning(id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("running", running);
        logger.debug("Statut de synchronisation pour tâche ID: {} = {}", id, running ? "EN COURS" : "ARRÊTÉE");
        return ResponseEntity.ok(response);
    }
}
