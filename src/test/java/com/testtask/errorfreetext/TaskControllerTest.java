package com.testtask.errorfreetext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testtask.errorfreetext.controller.TaskController;
import com.testtask.errorfreetext.dto.CreateTaskRequest;
import com.testtask.errorfreetext.dto.TaskStatusResponse;
import com.testtask.errorfreetext.enums.Language;
import com.testtask.errorfreetext.enums.TaskStatus;
import com.testtask.errorfreetext.exception.TaskNotFoundException;
import com.testtask.errorfreetext.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    void createTaskReturns201WithId() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.createTask(any())).thenReturn(id);

        var request = new CreateTaskRequest("Привкт мир", Language.RU);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void createTaskRejectsTextShorterThan3() throws Exception {
        var request = new CreateTaskRequest("ab", Language.RU);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(40001))
                .andExpect(jsonPath("$.errorMessage").exists())
                .andExpect(jsonPath("$.path").value("/tasks"));
    }

    @Test
    void createTaskRejectsTextWithoutLetters() throws Exception {
        var request = new CreateTaskRequest("123!!!", Language.RU);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(40001));
    }

    @Test
    void createTaskRejectsBlankText() throws Exception {
        var request = new CreateTaskRequest("   ", Language.RU);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTaskRejectsMissingLanguage() throws Exception {
        String body = """
                {"text": "Привет мир"}
                """;

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTaskRejectsInvalidLanguage() throws Exception {
        String body = """
                {"text": "Привет мир", "language": "DE"}
                """;

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(40002));
    }

    @Test
    void getTaskReturnsStatusForNewTask() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTask(id))
                .thenReturn(new TaskStatusResponse(TaskStatus.NEW, null, null));

        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.correctedText").doesNotExist())
                .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    @Test
    void getTaskReturnsCorrectedTextForDoneTask() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTask(id))
                .thenReturn(new TaskStatusResponse(TaskStatus.DONE, "Привет мир", null));

        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.correctedText").value("Привет мир"))
                .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    @Test
    void getTaskReturnsErrorMessageForFailedTask() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTask(id))
                .thenReturn(new TaskStatusResponse(TaskStatus.ERROR, null, "API failed"));

        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorMessage").value("API failed"))
                .andExpect(jsonPath("$.correctedText").doesNotExist());
    }

    @Test
    void getTaskReturns404WithErrorBodyWhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTask(id)).thenThrow(new TaskNotFoundException(id));

        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(40401))
                .andExpect(jsonPath("$.errorMessage").value(
                        "Task with id: " + id + " not found"))
                .andExpect(jsonPath("$.path").value("/tasks/" + id))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}