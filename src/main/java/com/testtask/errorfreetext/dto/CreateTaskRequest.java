package com.testtask.errorfreetext.dto;

import com.testtask.errorfreetext.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(

        @NotBlank(message = "Text must not be blank")
        @Size(min = 3, message = "Text must contain at least 3 characters")
        @Pattern(
                regexp = ".*\\p{L}.*",
                message = "Text must contain at least one letter"
        )
        String text,

        @NotNull(message = "Language must not be null")
        Language language
) {
}