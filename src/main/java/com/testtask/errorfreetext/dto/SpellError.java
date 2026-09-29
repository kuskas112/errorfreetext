package com.testtask.errorfreetext.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SpellError(
        @JsonProperty("code") int code,
        @JsonProperty("pos") int pos,
        @JsonProperty("row") int row,
        @JsonProperty("col") int col,
        @JsonProperty("len") int len,
        @JsonProperty("word") String word,
        @JsonProperty("s") List<String> suggestions
) {
}