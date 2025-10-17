package com.sync.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité pour stocker les métadonnées des fichiers synchronisés.
 * Permet de détecter les changements lors des synchros incrémentales.
 */
@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_sync_task_path", columnList = "sync_task_id,file_path")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sync_task_id", nullable = false)
    private SyncTask syncTask;

    @Column(nullable = false, length = 1000)
    private String filePath; // Chemin relatif par rapport à la source

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private LocalDateTime lastModified;

    @Column(length = 64)
    private String checksum; // SHA-256 ou MD5

    @Column
    private LocalDateTime lastSynced;

    @Column
    private Boolean isDirectory = false;
}
