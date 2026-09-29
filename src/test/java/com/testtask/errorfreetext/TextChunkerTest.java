package com.testtask.errorfreetext;

import com.testtask.errorfreetext.service.TextChunker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void returnsSingleChunkForShortText() {
        List<String> chunks = chunker.chunk("Привет мир");
        assertThat(chunks).containsExactly("Привет мир");
    }

    @Test
    void returnsEmptyListForEmptyText() {
        assertThat(chunker.chunk("")).isEmpty();
        assertThat(chunker.chunk(null)).isEmpty();
    }

    @Test
    void splitsLongTextWithoutLosingCharacters() {
        String text = "a".repeat(25_000);
        List<String> chunks = chunker.chunk(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(String.join("", chunks)).isEqualTo(text);
        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(10_000));
    }

    @Test
    void prefersBreakpointsOverHardCut() {
        // 10_500 символов, пробел на позиции 9_950
        String text = "a".repeat(9_950) + " " + "b".repeat(549);
        List<String> chunks = chunker.chunk(text);

        assertThat(chunks.getFirst()).endsWith(" ");
        assertThat(chunks.getFirst()).hasSize(9_951);
    }

    @Test
    void hardCutsWhenNoBreakpointInLookback() {
        String text = "a".repeat(15_000);
        List<String> chunks = chunker.chunk(text);

        assertThat(chunks.getFirst()).hasSize(10_000);
        assertThat(String.join("", chunks)).isEqualTo(text);
    }

    @Test
    void noChunkIsShorterThanMinimum() {
        // breakpoints частые — без MIN_CHUNK_SIZE могли бы получиться крошечные чанки
        String text = "a ".repeat(5_000) + "b".repeat(100);
        List<String> chunks = chunker.chunk(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c ->
                assertThat(c.length()).isGreaterThanOrEqualTo(TextChunker.MIN_CHUNK_SIZE)
        );
    }

    @Test
    void ignoresBreakpointTooCloseToStart() {
        // пробел на позиции 50 — ближе MIN_CHUNK_SIZE к start
        // lowerBound = max(0 + 100, 10000 - 500) = 500, пробел не находится
        String text = "a".repeat(50) + " " + "b".repeat(10_000);
        List<String> chunks = chunker.chunk(text);

        assertThat(chunks.getFirst()).hasSize(10_000);
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(String.join("", chunks)).isEqualTo(text);
    }

    @Test
    void isBreakCharHandlesUnicodeWhitespace() {
        assertThat(TextChunker.isBreakChar(' ')).isTrue();
        assertThat(TextChunker.isBreakChar('\n')).isTrue();
        assertThat(TextChunker.isBreakChar('\u00A0')).isTrue();
        assertThat(TextChunker.isBreakChar('.')).isTrue();
        assertThat(TextChunker.isBreakChar('-')).isFalse();
        assertThat(TextChunker.isBreakChar('\'')).isFalse();
        assertThat(TextChunker.isBreakChar('a')).isFalse();
    }
}