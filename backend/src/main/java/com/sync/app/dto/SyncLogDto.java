package com.sync.app.dto;

import com.sync.app.entity.SyncLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les logs de synchronisation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncLogDto {

    private Long id;
    private Long syncTaskId;
    private String syncTaskName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SyncLog.SyncStatus status;
    private Long filesScanned;
    private Long filesCopied;
    private Long filesUpdated;
    private Long filesDeleted;
    private Long filesSkipped;
    private Long totalBytes;
    private String errorMessage;
    private String details;
}
