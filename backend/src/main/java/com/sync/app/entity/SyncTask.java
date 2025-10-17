package com.sync.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité représentant une tâche de synchronisation configurée par l'utilisateur.
 */
@Entity
@Table(name = "sync_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sourcePath;

    @Column(nullable = false)
    private String destinationPath;

    @Column(nullable = false)
    private Long intervalMinutes; // Intervalle en minutes entre chaque synchro

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean useChecksum = true; // Utiliser checksum pour détecter les changements

    @Column
    private LocalDateTime lastSyncTime;

    @Column
    private LocalDateTime nextSyncTime;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (nextSyncTime == null) {
            nextSyncTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
