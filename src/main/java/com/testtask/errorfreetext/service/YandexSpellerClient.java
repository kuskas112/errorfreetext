package com.testtask.errorfreetext.service;

import com.testtask.errorfreetext.dto.SpellError;
import com.testtask.errorfreetext.enums.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Slf4j
public class YandexSpellerClient {

    private static final String CHECK_TEXTS_PATH = "/services/spellservice.json/checkTexts";

    private final RestClient restClient;

    public YandexSpellerClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://speller.yandex.net")
                .build();
    }

    public List<List<SpellError>> checkTexts(List<String> texts, Language language, int options) {
        // 1. Создаем MultiValueMap для повторяющегося параметра 'text'
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        texts.forEach(t -> formData.add("text", t));
        formData.add("lang", language.name().toLowerCase());
        formData.add("options", String.valueOf(options));

        log.debug("Calling Yandex Speller: texts={}, lang={}, options={}", texts.size(), language, options);

        // 2. Отправляем POST. Важно: MultiValueMap в body(), Content-Type через contentType()
        List<List<SpellError>> response = restClient.post()
                .uri(CHECK_TEXTS_PATH)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                // 3. ParameterizedTypeReference обязателен для сохранения generic-типа
                .body(new ParameterizedTypeReference<>() {});

        log.debug("Yandex Speller returned {} result(s)", response == null ? 0 : response.size());
        return response == null ? List.of() : response;
    }
}