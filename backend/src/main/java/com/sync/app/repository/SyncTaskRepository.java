package com.sync.app.repository;

import com.sync.app.entity.SyncTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SyncTaskRepository extends JpaRepository<SyncTask, Long> {

    List<SyncTask> findByActiveTrue();

    List<SyncTask> findByActiveTrueAndNextSyncTimeBefore(LocalDateTime dateTime);
}
