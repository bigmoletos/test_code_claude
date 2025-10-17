package com.sync.app.repository;

import com.sync.app.entity.SyncLog;
import com.sync.app.entity.SyncTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    Page<SyncLog> findBySyncTaskOrderByStartTimeDesc(SyncTask syncTask, Pageable pageable);

    List<SyncLog> findTop10BySyncTaskOrderByStartTimeDesc(SyncTask syncTask);

    Page<SyncLog> findAllByOrderByStartTimeDesc(Pageable pageable);
}
