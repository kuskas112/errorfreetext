package com.testtask.errorfreetext;

import com.testtask.errorfreetext.dto.CreateTaskRequest;
import com.testtask.errorfreetext.dto.TaskStatusResponse;
import com.testtask.errorfreetext.entity.Task;
import com.testtask.errorfreetext.enums.Language;
import com.testtask.errorfreetext.enums.TaskStatus;
import com.testtask.errorfreetext.exception.TaskNotFoundException;
import com.testtask.errorfreetext.repository.TaskRepository;
import com.testtask.errorfreetext.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
    }

    @Test
    void createTaskSavesWithNewStatusAndReturnsId() {
        UUID expectedId = UUID.randomUUID();
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(expectedId);
            return t;
        });

        UUID id = taskService.createTask(new CreateTaskRequest("Привет мир", Language.RU));

        assertThat(id).isEqualTo(expectedId);

        var captor = org.mockito.ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();

        assertThat(saved.getOriginalText()).isEqualTo("Привет мир");
        assertThat(saved.getLanguage()).isEqualTo(Language.RU);
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.NEW);
    }

    @Test
    void getTaskReturnsStatusResponseForNewTask() {
        UUID id = UUID.randomUUID();
        Task task = new Task("Привет", Language.RU);
        task.setId(id);
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));

        TaskStatusResponse response = taskService.getTask(id);

        assertThat(response.status()).isEqualTo(TaskStatus.NEW);
        assertThat(response.correctedText()).isNull();
        assertThat(response.errorMessage()).isNull();
    }

    @Test
    void getTaskReturnsCorrectedTextForDoneTask() {
        UUID id = UUID.randomUUID();
        Task task = new Task("Привкт", Language.RU);
        task.setId(id);
        task.setStatus(TaskStatus.DONE);
        task.setCorrectedText("Привет");
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));

        TaskStatusResponse response = taskService.getTask(id);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.correctedText()).isEqualTo("Привет");
    }

    @Test
    void getTaskReturnsErrorMessageForFailedTask() {
        UUID id = UUID.randomUUID();
        Task task = new Task("Привет", Language.RU);
        task.setId(id);
        task.setStatus(TaskStatus.ERROR);
        task.setErrorMessage("Yandex API unavailable");
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));

        TaskStatusResponse response = taskService.getTask(id);

        assertThat(response.status()).isEqualTo(TaskStatus.ERROR);
        assertThat(response.errorMessage()).isEqualTo("Yandex API unavailable");
    }

    @Test
    void getTaskThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(id))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void markDoneUpdatesStatusAndText() {
        UUID id = UUID.randomUUID();
        Task task = new Task("Привкт", Language.RU);
        task.setId(id);
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));

        taskService.markDone(id, "Привет");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getCorrectedText()).isEqualTo("Привет");
        assertThat(task.getErrorMessage()).isNull();
    }

    @Test
    void markErrorUpdatesStatusAndMessage() {
        UUID id = UUID.randomUUID();
        Task task = new Task("Привет", Language.RU);
        task.setId(id);
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));

        taskService.markError(id, "API failed");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.ERROR);
        assertThat(task.getErrorMessage()).isEqualTo("API failed");
    }

    @Test
    void claimNewTasksReturnsEmptyListWhenNoIdsClaimed() {
        when(taskRepository.claimNewTaskIds(10)).thenReturn(java.util.List.of());

        var result = taskService.claimNewTasks(10);

        assertThat(result).isEmpty();
        verify(taskRepository, never()).findAllById(any());
    }

    @Test
    void claimNewTasksLoadsEntitiesByIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Task t1 = new Task("a", Language.RU);
        t1.setId(id1);
        Task t2 = new Task("b", Language.RU);
        t2.setId(id2);

        when(taskRepository.claimNewTaskIds(10)).thenReturn(java.util.List.of(id1, id2));
        when(taskRepository.findAllById(java.util.List.of(id1, id2)))
                .thenReturn(java.util.List.of(t1, t2));

        var result = taskService.claimNewTasks(10);

        assertThat(result).containsExactly(t1, t2);
    }
}