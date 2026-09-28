package com.testtask.errorfreetext.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.testtask.errorfreetext.enums.TaskStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskStatusResponse(
        TaskStatus status,
        String correctedText,
        String errorMessage
) {
}