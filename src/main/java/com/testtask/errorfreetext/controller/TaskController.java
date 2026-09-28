package com.testtask.errorfreetext.controller;

import com.testtask.errorfreetext.dto.CreateTaskRequest;
import com.testtask.errorfreetext.dto.CreateTaskResponse;
import com.testtask.errorfreetext.dto.TaskStatusResponse;
import com.testtask.errorfreetext.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTaskResponse createTask(@Valid @RequestBody CreateTaskRequest request) {
        UUID id = taskService.createTask(request);
        return new CreateTaskResponse(id);
    }

    @GetMapping("/{id}")
    public TaskStatusResponse getTask(@PathVariable UUID id) {
        return taskService.getTask(id);
    }
}