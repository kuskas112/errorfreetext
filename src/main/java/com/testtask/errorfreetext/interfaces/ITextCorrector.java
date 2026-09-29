package com.testtask.errorfreetext.interfaces;

import com.testtask.errorfreetext.enums.Language;

public interface ITextCorrector {
    public String correct(String originalText, Language lang);
}
