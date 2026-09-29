package com.testtask.errorfreetext.scheduler;

import com.testtask.errorfreetext.entity.Task;
import com.testtask.errorfreetext.interfaces.ITextCorrector;
import com.testtask.errorfreetext.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskProcessingScheduler {

    private final TaskService taskService;
    private final ITextCorrector textCorrector;

    @Value("${app.scheduler.batch-size:10}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.scheduler.delay-ms:5000}")
    public void processNewTasks() {
        List<Task> tasks = taskService.claimNewTasks(batchSize);
        if (tasks.isEmpty()) {
            log.info("No tasks for processing");
            return;
        }
        log.info("Claimed {} tasks for processing", tasks.size());

        for (Task task : tasks) {
            try {
                String corrected = textCorrector.correct(task.getOriginalText(), task.getLanguage());
                taskService.markDone(task.getId(), corrected);
            } catch (Exception e) {
                log.error("Failed to process task {}", task.getId(), e);
                taskService.markError(task.getId(), e.getMessage());
            }
        }
    }
}