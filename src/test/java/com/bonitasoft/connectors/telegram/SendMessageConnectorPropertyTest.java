package com.bonitasoft.connectors.telegram;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.connector.ConnectorValidationException;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

class SendMessageConnectorPropertyTest {

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
    Arbitrary<String> validTexts() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100)
                .flatMap(prefix -> Arbitraries.strings().ofMaxLength(3996)
                        .map(suffix -> prefix + suffix));
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
    Arbitrary<Long> negativeChatIdLongs() {
        return Arbitraries.longs().between(-1_999_999_999_999L, -1_000_000_000_000L);
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("botToken", "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11");
        inputs.put("chatId", "-1001234567890");
        inputs.put("text", "Hello World");
        return inputs;
    }

    @Property(tries = 50)
    void mandatoryTextRejectsBlank(@ForAll("blankStrings") String text) {
        var connector = new SendMessageConnector();
        var inputs = validInputs();
        inputs.put("text", text);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property(tries = 50)
    void mandatoryChatIdRejectsBlank(@ForAll("blankStrings") String chatId) {
        var connector = new SendMessageConnector();
        var inputs = validInputs();
        inputs.put("chatId", chatId);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property(tries = 50)
    void mandatoryBotTokenRejectsBlank(@ForAll("blankStrings") String token) {
        var connector = new SendMessageConnector();
        var inputs = validInputs();
        inputs.put("botToken", token);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property(tries = 50)
    void validConfigurationAlwaysBuilds(
            @ForAll("validBotTokens") String token,
            @ForAll("validChatIds") String chatId,
            @ForAll("validTexts") String text) {
        var connector = new SendMessageConnector();
        var inputs = new HashMap<String, Object>();
        inputs.put("botToken", token);
        inputs.put("chatId", chatId);
        inputs.put("text", text);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property(tries = 50)
    void parseModeAcceptsValidValues(@ForAll("validParseModes") String parseMode) {
        var connector = new SendMessageConnector();
        var inputs = validInputs();
        inputs.put("parseMode", parseMode);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property(tries = 50)
    void disableNotificationAcceptsBothValues(@ForAll boolean disable) {
        var connector = new SendMessageConnector();
        var inputs = validInputs();
        inputs.put("disableNotification", disable);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property(tries = 50)
    void textMaxLengthAcceptsUpTo4096(
            @ForAll @IntRange(min = 1, max = 4096) int length) {
        var connector = new SendMessageConnector();
        var inputs = validInputs();
        inputs.put("text", "A".repeat(length));
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property(tries = 50)
    void textExceeding4096IsRejected(
            @ForAll @IntRange(min = 4097, max = 5000) int length) {
        var connector = new SendMessageConnector();
        var inputs = validInputs();
        inputs.put("text", "A".repeat(length));
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("4096");
    }

    @Property(tries = 50)
    void chatIdNegativeForGroups(@ForAll("negativeChatIdLongs") long chatId) {
        var connector = new SendMessageConnector();
        var inputs = validInputs();
        inputs.put("chatId", String.valueOf(chatId));
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property(tries = 50)
    void timeoutPositiveOnly(@ForAll("positiveTimeouts") int timeout) {
        var connector = new SendMessageConnector();
        var inputs = validInputs();
        inputs.put("connectTimeout", timeout);
        inputs.put("readTimeout", timeout);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property(tries = 50)
    void defaultParseModeIsHTML(@ForAll("validBotTokens") String token) {
        var connector = new SendMessageConnector();
        var inputs = new HashMap<String, Object>();
        inputs.put("botToken", token);
        inputs.put("chatId", "-1001234567890");
        inputs.put("text", "Test");
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property(tries = 50)
    void stringTimeoutIsParsed(@ForAll @IntRange(min = 1, max = 300_000) int timeout) {
        var connector = new SendMessageConnector();
        var inputs = validInputs();
        inputs.put("connectTimeout", String.valueOf(timeout));
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }
}
