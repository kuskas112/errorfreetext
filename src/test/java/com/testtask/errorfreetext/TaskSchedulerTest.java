package com.testtask.errorfreetext;

import com.testtask.errorfreetext.entity.Task;
import com.testtask.errorfreetext.enums.Language;
import com.testtask.errorfreetext.interfaces.ITextCorrector;
import com.testtask.errorfreetext.scheduler.TaskProcessingScheduler;
import com.testtask.errorfreetext.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TextTaskSchedulerTest {

    @Mock private TaskService taskService;
    @Mock private ITextCorrector textCorrector;

    @InjectMocks
    private TaskProcessingScheduler scheduler;

    @Test
    void processesClaimedTasksAndMarksDone() {
        Task t = new Task("Привкт", Language.RU);
        t.setId(UUID.randomUUID());

        when(taskService.claimNewTasks(anyInt())).thenReturn(List.of(t));
        when(textCorrector.correct("Привкт", Language.RU)).thenReturn("Привет");

        scheduler.processNewTasks();

        verify(taskService).markDone(t.getId(), "Привет");
        verify(taskService, never()).markError(any(), any());
    }

    @Test
    void marksTaskAsErrorWhenCorrectionFails() {
        Task t = new Task("Привкт", Language.RU);
        t.setId(UUID.randomUUID());

        when(taskService.claimNewTasks(anyInt())).thenReturn(List.of(t));
        when(textCorrector.correct(any(), any()))
                .thenThrow(new RuntimeException("API down"));

        scheduler.processNewTasks();

        verify(taskService).markError(t.getId(), "API down");
        verify(taskService, never()).markDone(any(), any());
    }

    @Test
    void doesNothingWhenNoTasksClaimed() {
        when(taskService.claimNewTasks(anyInt())).thenReturn(List.of());

        scheduler.processNewTasks();

        verifyNoInteractions(textCorrector);
        verify(taskService, never()).markDone(any(), any());
    }

    @Test
    void continuesProcessingAfterOneTaskFails() {
        Task ok = new Task("a", Language.RU);
        ok.setId(UUID.randomUUID());
        Task bad = new Task("b", Language.RU);
        bad.setId(UUID.randomUUID());

        when(taskService.claimNewTasks(anyInt())).thenReturn(List.of(bad, ok));
        when(textCorrector.correct("b", Language.RU)).thenThrow(new RuntimeException("fail"));
        when(textCorrector.correct("a", Language.RU)).thenReturn("A");

        scheduler.processNewTasks();

        verify(taskService).markError(bad.getId(), "fail");
        verify(taskService).markDone(ok.getId(), "A");
    }
}