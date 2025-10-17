package com.sync.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité représentant un log d'exécution de synchronisation.
 */
@Entity
@Table(name = "sync_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sync_task_id", nullable = false)
    private SyncTask syncTask;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus status;

    @Column
    private Long filesScanned = 0L;

    @Column
    private Long filesCopied = 0L;

    @Column
    private Long filesUpdated = 0L;

    @Column
    private Long filesDeleted = 0L;

    @Column
    private Long filesSkipped = 0L;

    @Column
    private Long totalBytes = 0L;

    @Column(length = 2000)
    private String errorMessage;

    @Column(length = 5000)
    private String details;

    public enum SyncStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
