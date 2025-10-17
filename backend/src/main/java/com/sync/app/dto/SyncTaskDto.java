package com.sync.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la création/modification d'une tâche de synchronisation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncTaskDto {

    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotBlank(message = "Le chemin source est obligatoire")
    private String sourcePath;

    @NotBlank(message = "Le chemin de destination est obligatoire")
    private String destinationPath;

    @NotNull(message = "L'intervalle est obligatoire")
    @Min(value = 1, message = "L'intervalle doit être au moins 1 minute")
    private Long intervalMinutes;

    private Boolean active = true;

    private Boolean useChecksum = true;

    private LocalDateTime lastSyncTime;

    private LocalDateTime nextSyncTime;
}
