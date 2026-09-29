package com.testtask.errorfreetext.service;

import com.testtask.errorfreetext.dto.CreateTaskRequest;
import com.testtask.errorfreetext.dto.TaskStatusResponse;
import com.testtask.errorfreetext.entity.Task;
import com.testtask.errorfreetext.enums.TaskStatus;
import com.testtask.errorfreetext.exception.TaskNotFoundException;
import com.testtask.errorfreetext.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public UUID createTask(CreateTaskRequest request) {
        Task task = new Task(request.text(), request.language());
        Task saved = taskRepository.save(task);
        log.info("Created task id={}, language={}", saved.getId(), saved.getLanguage());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public TaskStatusResponse getTask(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        return new TaskStatusResponse(
                task.getStatus(),
                task.getCorrectedText(),
                task.getErrorMessage()
        );
    }

    @Transactional
    public List<Task> claimNewTasks(int batchSize) {
        List<UUID> ids = taskRepository.claimNewTaskIds(batchSize);
        if (ids.isEmpty()) {
            return List.of();
        }
        return taskRepository.findAllById(ids);
    }

    @Transactional
    public void markDone(UUID taskId, String correctedText) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        task.setCorrectedText(correctedText);
        task.setStatus(TaskStatus.DONE);
        task.setErrorMessage(null);
        log.info("Task {} completed", taskId);
    }

    @Transactional
    public void markError(UUID taskId, String errorMessage) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        task.setStatus(TaskStatus.ERROR);
        task.setErrorMessage(errorMessage);
        log.warn("Task {} failed: {}", taskId, errorMessage);
    }
}