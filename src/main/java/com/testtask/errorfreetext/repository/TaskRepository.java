package com.testtask.errorfreetext.repository;

import com.testtask.errorfreetext.entity.Task;
import com.testtask.errorfreetext.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByStatus(TaskStatus status);

    @Modifying
    @Query(value = """
            UPDATE tasks
            SET status = 'IN_PROGRESS', updated_at = NOW()
            WHERE id IN (
                SELECT id FROM tasks
                WHERE status = 'NEW'
                ORDER BY created_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id
            """, nativeQuery = true)
    List<UUID> claimNewTaskIds(@Param("limit") int limit);
}