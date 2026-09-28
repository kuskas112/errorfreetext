package com.testtask.errorfreetext.repository;

import com.testtask.errorfreetext.entity.Task;
import com.testtask.errorfreetext.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByStatus(TaskStatus status);
}