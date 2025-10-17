package com.sync.app.repository;

import com.sync.app.entity.FileMetadata;
import com.sync.app.entity.SyncTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    Optional<FileMetadata> findBySyncTaskAndFilePath(SyncTask syncTask, String filePath);

    List<FileMetadata> findBySyncTask(SyncTask syncTask);

    void deleteBySyncTask(SyncTask syncTask);
}
