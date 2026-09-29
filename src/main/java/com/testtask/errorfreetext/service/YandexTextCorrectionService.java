package com.testtask.errorfreetext.service;

import com.testtask.errorfreetext.dto.SpellError;
import com.testtask.errorfreetext.enums.Language;
import com.testtask.errorfreetext.interfaces.ITextCorrector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class YandexTextCorrectionService implements ITextCorrector {

    private final YandexSpellerClient client;
    private final SpellerOptionsResolver optionsResolver;
    private final TextChunker chunker;

    @Override
    public String correct(String text, Language lang) {
        List<String> chunks = chunker.chunk(text);
        int options = optionsResolver.resolve(text);

        log.info("Correcting text: {} chars, {} chunk(s), lang={}, options={}",
                text.length(), chunks.size(), lang, options);

        List<List<SpellError>> results = client.checkTexts(chunks, lang, options);

        if (results.size() != chunks.size()) {
            log.warn("Yandex returned {} results for {} chunks — falling back to original",
                    results.size(), chunks.size());
            return text;
        }

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < chunks.size(); i++) {
            String correctedChunk = applyCorrections(chunks.get(i), results.get(i));
            sb.append(correctedChunk);
        }
        return sb.toString();
    }

    private String applyCorrections(String original, List<SpellError> errors) {
        if (errors == null || errors.isEmpty()) {
            return original;
        }

        StringBuilder sb = new StringBuilder();
        int cursor = 0;

        for (SpellError error : errors) {
            if (error.pos() < cursor || error.pos() + error.len() > original.length()) {
                log.warn("Skipping suspicious error at pos={}, len={}", error.pos(), error.len());
                continue;
            }

            sb.append(original, cursor, error.pos());

            String replacement = error.suggestions().isEmpty()
                    ? original.substring(error.pos(), error.pos() + error.len())
                    : error.suggestions().getFirst();

            sb.append(replacement);
            cursor = error.pos() + error.len();
        }

        sb.append(original, cursor, original.length());
        return sb.toString();
    }
}