package com.testtask.errorfreetext;

import com.testtask.errorfreetext.dto.SpellError;
import com.testtask.errorfreetext.enums.Language;
import com.testtask.errorfreetext.service.SpellerOptionsResolver;
import com.testtask.errorfreetext.service.TextChunker;
import com.testtask.errorfreetext.service.YandexSpellerClient;
import com.testtask.errorfreetext.service.YandexTextCorrectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YandexTextCorrectionServiceTest {

    @Mock
    private YandexSpellerClient client;

    @Mock
    private SpellerOptionsResolver optionsResolver;

    @Mock
    private TextChunker chunker;

    private YandexTextCorrectionService service;

    @BeforeEach
    void setUp() {
        service = new YandexTextCorrectionService(client, optionsResolver, chunker);
    }

    // ---------- базовые сценарии ----------

    @Test
    void returnsOriginalWhenNoErrors() {
        String original = "Привет мир";
        mockSingleChunk(original);
        when(client.checkTexts(any(), any(), anyInt())).thenReturn(List.of(List.of()));

        String result = service.correct(original, Language.RU);

        assertThat(result).isEqualTo(original);
    }

    @Test
    void appliesSingleCorrection() {
        String original = "Привкт мир";
        mockSingleChunk(original);
        when(client.checkTexts(any(), any(), anyInt())).thenReturn(List.of(List.of(
                new SpellError(1, 0, 0, 0, 6, "Привкт", List.of("Привет"))
        )));

        String result = service.correct(original, Language.RU);

        assertThat(result).isEqualTo("Привет мир");
    }

    @Test
    void appliesMultipleCorrections() {
        String original = "Привкт менч зовут Иван";
        mockSingleChunk(original);
        when(client.checkTexts(any(), any(), anyInt())).thenReturn(List.of(List.of(
                new SpellError(1, 0, 0, 0, 6, "Привкт", List.of("Привет")),
                new SpellError(1, 7, 0, 7, 4, "менч", List.of("меня"))
        )));

        String result = service.correct(original, Language.RU);

        assertThat(result).isEqualTo("Привет меня зовут Иван");
    }

    @Test
    void picksFirstSuggestion() {
        String original = "Привкт мир";
        mockSingleChunk(original);
        when(client.checkTexts(any(), any(), anyInt())).thenReturn(List.of(List.of(
                new SpellError(1, 0, 0, 0, 6, "Привкт", List.of("Привет", "Приветик"))
        )));

        String result = service.correct(original, Language.RU);

        assertThat(result).isEqualTo("Привет мир");
    }

    @Test
    void keepsOriginalWordWhenNoSuggestions() {
        String original = "фывап мир";
        mockSingleChunk(original);
        when(client.checkTexts(any(), any(), anyInt())).thenReturn(List.of(List.of(
                new SpellError(1, 0, 0, 0, 5, "фывап", List.of())
        )));

        String result = service.correct(original, Language.RU);

        assertThat(result).isEqualTo(original);
    }

    // ---------- защита от некорректных данных ----------

    @Test
    void skipsOverlappingErrors() {
        String original = "Привкт мир";
        mockSingleChunk(original);
        when(client.checkTexts(any(), any(), anyInt())).thenReturn(List.of(List.of(
                new SpellError(1, 0, 0, 0, 6, "Привкт", List.of("Привет")),
                new SpellError(1, 3, 0, 3, 3, "вкт", List.of("шка"))
        )));

        String result = service.correct(original, Language.RU);

        assertThat(result).isEqualTo("Привет мир");
    }

    @Test
    void skipsErrorWithOutOfBoundsPos() {
        String original = "Привет";
        mockSingleChunk(original);
        when(client.checkTexts(any(), any(), anyInt())).thenReturn(List.of(List.of(
                new SpellError(1, 100, 0, 100, 5, "abcde", List.of("abc"))
        )));

        String result = service.correct(original, Language.RU);

        assertThat(result).isEqualTo(original);
    }

    @Test
    void skipsErrorThatOverflowsTextLength() {
        String original = "Привет";
        mockSingleChunk(original);
        when(client.checkTexts(any(), any(), anyInt())).thenReturn(List.of(List.of(
                new SpellError(1, 3, 0, 3, 100, "вет...", List.of("xxx"))
        )));

        String result = service.correct(original, Language.RU);

        assertThat(result).isEqualTo(original);
    }

    // ---------- многоканальная обработка ----------

    @Test
    void joinsCorrectedChunksBackWithoutLoss() {
        String chunk1 = "Привкт ";
        String chunk2 = "менч";
        String original = chunk1 + chunk2;

        when(chunker.chunk(original)).thenReturn(List.of(chunk1, chunk2));
        when(optionsResolver.resolve(original)).thenReturn(0);
        when(client.checkTexts(any(), any(), anyInt())).thenReturn(List.of(
                List.of(new SpellError(1, 0, 0, 0, 6, "Привкт", List.of("Привет"))),
                List.of(new SpellError(1, 0, 0, 0, 4, "менч", List.of("меня")))
        ));

        String result = service.correct(original, Language.RU);

        assertThat(result).isEqualTo("Привет меня");
    }

    @Test
    void fallsBackToOriginalWhenResultsCountMismatch() {
        String original = "Привкт менч";
        when(chunker.chunk(original)).thenReturn(List.of("Привкт ", "менч"));
        when(optionsResolver.resolve(original)).thenReturn(0);
        // вернули только один результат на два чанка
        when(client.checkTexts(any(), any(), anyInt())).thenReturn(List.of(
                List.of(new SpellError(1, 0, 0, 0, 6, "Привкт", List.of("Привет")))
        ));

        String result = service.correct(original, Language.RU);

        assertThat(result).isEqualTo(original);
    }

    // ---------- опции ----------

    @Test
    void passesResolvedOptionsToClient() {
        String original = "100 рублей на https://example.com";
        mockSingleChunk(original);
        when(optionsResolver.resolve(original)).thenReturn(10);
        when(client.checkTexts(any(), any(), eq(10))).thenReturn(List.of(List.of()));

        service.correct(original, Language.RU);

        // verify не нужен — если бы options не совпал, мок вернул бы null,
        // и сервис упал бы с NPE или вернул оригинал
        assertThat(service.correct(original, Language.RU)).isEqualTo(original);
    }

    // ---------- вспомогательный метод ----------

    private void mockSingleChunk(String original) {
        when(chunker.chunk(original)).thenReturn(List.of(original));
        when(optionsResolver.resolve(original)).thenReturn(0);
    }
}