package com.testtask.errorfreetext.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TextChunker {

    /** Максимальный размер чанка — ограничение API Яндекс.Спеллера. */
    public final int MAX_CHUNK_SIZE = 10_000;

    /** Насколько далеко назад от границы искать breakpoint. */
    static final int MAX_LOOKBACK = 500;

    /** Минимальный размер чанка — защита от схлопывания в 1–2 символа. */
    public static final int MIN_CHUNK_SIZE = 100;

    public List<String> chunk(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        if (text.length() <= MAX_CHUNK_SIZE) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, text.length());

            if (end < text.length()) {
                int lowerBound = Math.max(start + MIN_CHUNK_SIZE, end - MAX_LOOKBACK);
                int breakPoint = findBreakPoint(text, lowerBound, end);
                if (breakPoint > lowerBound) {
                    end = breakPoint;
                } else {
                    log.debug("No suitable breakpoint in [{}, {}), cutting at {}",
                            lowerBound, end, end);
                }
            }

            chunks.add(text.substring(start, end));
            start = end;
        }

        log.info("Chunked text of {} chars into {} chunk(s)", text.length(), chunks.size());
        return chunks;
    }

    /**
     * Ищет последний breakpoint в диапазоне {@code (lowerBound, end)}.
     * Возвращает позицию сразу после разделителя или {@code -1}, если не нашёл.
     */
    private int findBreakPoint(String text, int lowerBound, int end) {
        for (int i = end - 1; i > lowerBound; i--) {
            if (isBreakChar(text.charAt(i))) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Разделитель, по которому безопасно рвать текст:
     * пробельные символы (включая \n, \t, \u00A0) и знаки пунктуации.
     * Дефис и апостроф НЕ включены — они часто часть слова.
     */
    public static boolean isBreakChar(char c) {
        return Character.isWhitespace(c)
                || Character.isSpaceChar(c)
                || c == '.'
                || c == ','
                || c == '!'
                || c == '?'
                || c == ';'
                || c == ':';
    }
}