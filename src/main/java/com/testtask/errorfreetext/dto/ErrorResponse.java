package com.testtask.errorfreetext.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        String errorMessage,
        int errorCode,
        LocalDateTime timestamp,
        String path
) {
}