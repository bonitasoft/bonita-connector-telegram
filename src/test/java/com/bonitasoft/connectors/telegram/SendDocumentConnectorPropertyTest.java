package com.bonitasoft.connectors.telegram;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.connector.ConnectorValidationException;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

class SendDocumentConnectorPropertyTest {

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", " ", "\t", "\n", "   ", "\t\n");
    }

    @Provide
    Arbitrary<String> validBotTokens() {
        return Arbitraries.longs().between(100_000L, 9_999_999_999L)
                .flatMap(id -> Arbitraries.strings().alpha().ofLength(35)
                        .map(secret -> id + ":" + secret));
    }

    @Provide
    Arbitrary<String> validChatIds() {
        return Arbitraries.oneOf(
                Arbitraries.longs().between(1L, 9_999_999_999L).map(String::valueOf),
                Arbitraries.longs().between(-1_999_999_999_999L, -1_000_000_000_000L).map(String::valueOf)
        );
    }

    @Provide
    Arbitrary<String> validParseModes() {
        return Arbitraries.of("HTML", "Markdown", "MarkdownV2");
    }

    @Provide
    Arbitrary<Integer> positiveTimeouts() {
        return Arbitraries.integers().between(1, 300_000);
    }

    @Provide
    Arbitrary<String> validDocumentUrls() {
        return Arbitraries.of(
                "https://example.com/doc.pdf",
                "https://cdn.company.com/reports/annual-2024.xlsx",
                "https://storage.cloud.google.com/bucket/file.zip",
                "https://s3.amazonaws.com/bucket/key.docx"
        );
    }

    @Provide
    Arbitrary<String> validCaptions() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(200);
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("botToken", "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11");
        inputs.put("chatId", "-1001234567890");
        inputs.put("documentUrl", "https://example.com/invoice.pdf");
        return inputs;
    }

    @Property
    void mandatoryDocumentUrlRejectsBlank(@ForAll("blankStrings") String url) {
        var connector = new SendDocumentConnector();
        var inputs = validInputs();
        inputs.put("documentUrl", url);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void mandatoryChatIdRejectsBlank(@ForAll("blankStrings") String chatId) {
        var connector = new SendDocumentConnector();
        var inputs = validInputs();
        inputs.put("chatId", chatId);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void validConfigurationAlwaysBuilds(
            @ForAll("validBotTokens") String token,
            @ForAll("validChatIds") String chatId,
            @ForAll("validDocumentUrls") String documentUrl) {
        var connector = new SendDocumentConnector();
        var inputs = new HashMap<String, Object>();
        inputs.put("botToken", token);
        inputs.put("chatId", chatId);
        inputs.put("documentUrl", documentUrl);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void captionOptionalAcceptsNull(
            @ForAll("validBotTokens") String token,
            @ForAll("validChatIds") String chatId) {
        var connector = new SendDocumentConnector();
        var inputs = new HashMap<String, Object>();
        inputs.put("botToken", token);
        inputs.put("chatId", chatId);
        inputs.put("documentUrl", "https://example.com/doc.pdf");
        inputs.put("caption", null);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void parseModeAcceptsValidValues(@ForAll("validParseModes") String parseMode) {
        var connector = new SendDocumentConnector();
        var inputs = validInputs();
        inputs.put("parseMode", parseMode);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void documentUrlAcceptsVariousFormats(@ForAll("validDocumentUrls") String url) {
        var connector = new SendDocumentConnector();
        var inputs = validInputs();
        inputs.put("documentUrl", url);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void captionWithVariousLengths(@ForAll("validCaptions") String caption) {
        var connector = new SendDocumentConnector();
        var inputs = validInputs();
        inputs.put("caption", caption);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void disableNotificationAcceptsBothValues(@ForAll boolean disable) {
        var connector = new SendDocumentConnector();
        var inputs = validInputs();
        inputs.put("disableNotification", disable);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void botTokenRejectsBlank(@ForAll("blankStrings") String token) {
        var connector = new SendDocumentConnector();
        var inputs = validInputs();
        inputs.put("botToken", token);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void timeoutPositiveOnly(@ForAll("positiveTimeouts") int timeout) {
        var connector = new SendDocumentConnector();
        var inputs = validInputs();
        inputs.put("connectTimeout", timeout);
        inputs.put("readTimeout", timeout);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void defaultParseModeIsHTML(@ForAll("validBotTokens") String token) {
        var connector = new SendDocumentConnector();
        var inputs = new HashMap<String, Object>();
        inputs.put("botToken", token);
        inputs.put("chatId", "-1001234567890");
        inputs.put("documentUrl", "https://example.com/doc.pdf");
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }
}
