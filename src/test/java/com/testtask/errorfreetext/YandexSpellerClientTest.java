package com.testtask.errorfreetext;

import com.testtask.errorfreetext.dto.SpellError;
import com.testtask.errorfreetext.enums.Language;
import com.testtask.errorfreetext.service.YandexSpellerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YandexSpellerClientTest {

    private static final String BASE_URL = "https://speller.yandex.net";

    private MockRestServiceServer server;
    private YandexSpellerClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new YandexSpellerClient(builder);
    }

    @Test
    void sendsFormUrlEncodedPostRequest() {
        server.expect(requestTo(BASE_URL + "/services/spellservice.json/checkTexts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("lang=ru")))
                .andExpect(content().string(containsString("options=0")))
                .andRespond(withSuccess("[[],[]]", MediaType.APPLICATION_JSON));

        client.checkTexts(List.of("Привет", "мир"), Language.RU, 0);

        server.verify();
    }

    @Test
    void sendsMultipleTextsAsRepeatedParams() {
        server.expect(requestTo(containsString("/checkTexts")))
                .andExpect(content().string(containsString("text=one")))
                .andExpect(content().string(containsString("text=two")))
                .andRespond(withSuccess("[[],[]]", MediaType.APPLICATION_JSON));

        client.checkTexts(List.of("one", "two"), Language.EN, 0);

        server.verify();
    }

    @Test
    void sendsOptionsAndUppercaseLanguageAsLowercase() {
        server.expect(requestTo(containsString("/checkTexts")))
                .andExpect(content().string(containsString("lang=ru")))
                .andExpect(content().string(containsString("options=10")))
                .andRespond(withSuccess("[[],[]]", MediaType.APPLICATION_JSON));

        client.checkTexts(List.of("100 https://example.com"), Language.RU, 10);

        server.verify();
    }

    @Test
    void parsesSpellErrorsFromResponse() {
        String json = """
                [[
                  {"code":1,"pos":0,"row":0,"col":0,"len":6,
                   "word":"Привкт","s":["Привет"]},
                  {"code":1,"pos":7,"row":0,"col":7,"len":4,
                   "word":"менч","s":["меня","меняю"]}
                ]]
                """;

        server.expect(requestTo(containsString("/checkTexts")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<List<SpellError>> result = client.checkTexts(
                List.of("Привкт менч зовут Иван"), Language.RU, 0);

        assertThat(result).hasSize(1);

        List<SpellError> errors = result.getFirst();
        assertThat(errors).hasSize(2);

        SpellError first = errors.getFirst();
        assertThat(first.word()).isEqualTo("Привкт");
        assertThat(first.pos()).isZero();
        assertThat(first.len()).isEqualTo(6);
        assertThat(first.suggestions()).containsExactly("Привет");

        SpellError second = errors.get(1);
        assertThat(second.word()).isEqualTo("менч");
        assertThat(second.pos()).isEqualTo(7);
        assertThat(second.suggestions()).containsExactly("меня", "меняю");
    }

    @Test
    void parsesTwoTextsResponseAsNestedList() {
        String json = """
                [
                  [{"code":1,"pos":0,"row":0,"col":0,"len":6,
                    "word":"Привкт","s":["Привет"]}],
                  []
                ]
                """;

        server.expect(requestTo(containsString("/checkTexts")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<List<SpellError>> result = client.checkTexts(
                List.of("Привкт", "мир"), Language.RU, 0);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst()).hasSize(1);
        assertThat(result.getFirst().getFirst().word()).isEqualTo("Привкт");
        assertThat(result.get(1)).isEmpty();
    }

    @Test
    void returnsEmptyListWhenResponseIsNull() {
        server.expect(requestTo(containsString("/checkTexts")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<List<SpellError>> result = client.checkTexts(
                List.of("Привет"), Language.RU, 0);

        assertThat(result).isEmpty();
    }

    @Test
    void parsesErrorWithEmptySuggestions() {
        String json = """
                [[
                  {"code":1,"pos":0,"row":0,"col":0,"len":5,
                   "word":"фывап","s":[]}
                ]]
                """;

        server.expect(requestTo(containsString("/checkTexts")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<List<SpellError>> result = client.checkTexts(
                List.of("фывап"), Language.RU, 0);

        assertThat(result.getFirst().getFirst().suggestions()).isEmpty();
    }

    @Test
    void propagatesServerError() {
        server.expect(requestTo(containsString("/checkTexts")))
                .andRespond(withServerError());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        client.checkTexts(List.of("Привет"), Language.RU, 0))
                .isInstanceOf(org.springframework.web.client.RestClientException.class);
    }
}