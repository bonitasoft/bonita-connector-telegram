package com.bonitasoft.connectors.telegram;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.connector.ConnectorValidationException;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

class PinMessageConnectorPropertyTest {

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
        inputs.put("messageId", 42L);
        return inputs;
    }

    @Property
    void mandatoryMessageIdRejectsNull() {
        var connector = new PinMessageConnector();
        var inputs = validInputs();
        inputs.remove("messageId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("messageId");
    }

    @Property
    void mandatoryChatIdRejectsBlank(@ForAll("blankStrings") String chatId) {
        var connector = new PinMessageConnector();
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
            @ForAll @LongRange(min = 1, max = 999_999_999L) long messageId) {
        var connector = new PinMessageConnector();
        var inputs = new HashMap<String, Object>();
        inputs.put("botToken", token);
        inputs.put("chatId", chatId);
        inputs.put("messageId", messageId);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void messageIdPositiveOnly(@ForAll @LongRange(min = 1, max = Long.MAX_VALUE) long messageId) {
        var connector = new PinMessageConnector();
        var inputs = validInputs();
        inputs.put("messageId", messageId);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void disableNotificationAcceptsBothValues(@ForAll boolean disable) {
        var connector = new PinMessageConnector();
        var inputs = validInputs();
        inputs.put("disableNotification", disable);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void botTokenRejectsBlank(@ForAll("blankStrings") String token) {
        var connector = new PinMessageConnector();
        var inputs = validInputs();
        inputs.put("botToken", token);
        connector.setInputParameters(inputs);
        assertThatThrownBy(connector::validateInputParameters)
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Property
    void timeoutPositiveOnly(@ForAll("positiveTimeouts") int timeout) {
        var connector = new PinMessageConnector();
        var inputs = validInputs();
        inputs.put("connectTimeout", timeout);
        inputs.put("readTimeout", timeout);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void defaultValuesApplied(@ForAll("validBotTokens") String token) {
        var connector = new PinMessageConnector();
        var inputs = new HashMap<String, Object>();
        inputs.put("botToken", token);
        inputs.put("chatId", "-1001234567890");
        inputs.put("messageId", 1L);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void chatIdNegativeForGroups(@ForAll("negativeChatIdLongs") long chatId) {
        var connector = new PinMessageConnector();
        var inputs = validInputs();
        inputs.put("chatId", String.valueOf(chatId));
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void messageIdAcceptsIntegerType(@ForAll @IntRange(min = 1, max = Integer.MAX_VALUE) int messageId) {
        var connector = new PinMessageConnector();
        var inputs = validInputs();
        inputs.put("messageId", messageId);
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }

    @Property
    void messageIdAcceptsStringType(@ForAll @LongRange(min = 1, max = 999_999_999L) long messageId) {
        var connector = new PinMessageConnector();
        var inputs = validInputs();
        inputs.put("messageId", String.valueOf(messageId));
        connector.setInputParameters(inputs);
        assertThatCode(connector::validateInputParameters).doesNotThrowAnyException();
    }
}
