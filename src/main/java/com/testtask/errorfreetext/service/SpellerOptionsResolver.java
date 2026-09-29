package com.testtask.errorfreetext.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SpellerOptionsResolver {

    // Битовая маска из документации Яндекс.Спеллера
    private static final int IGNORE_DIGITS = 2;
    private static final int IGNORE_URLS = 8;

    private static final Pattern DIGITS = Pattern.compile("\\d");
    private static final Pattern URL = Pattern.compile(
            "(https?://|www\\.)\\S+",
            Pattern.CASE_INSENSITIVE
    );

    public int resolve(String text) {
        int options = 0;
        if (DIGITS.matcher(text).find()) {
            options |= IGNORE_DIGITS;
        }
        if (URL.matcher(text).find()) {
            options |= IGNORE_URLS;
        }
        return options;
    }
}