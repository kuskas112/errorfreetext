package com.testtask.errorfreetext.service;

import com.testtask.errorfreetext.enums.Language;
import com.testtask.errorfreetext.interfaces.ITextCorrector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class YandexTextCorrectionService implements ITextCorrector {

    @Override
    public String correct(String text, Language lang) {
        // ЗАГЛУШКА — вернём текст как есть
        log.info("STUB: correcting text in {} language", lang);
        return text + " [corrected]";
    }
}
