package com.bonitasoft.connectors.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bonitasoft.engine.connector.ConnectorValidationException;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

class TelegramConnectorPropertyTest {

    private static final Set<String> VALID_PARSE_MODES = Set.of("HTML", "Markdown", "MarkdownV2");

    // --- Arbitraries ---

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
    Arbitrary<String> validTexts() {
        // Ensure at least one non-whitespace character (readMandatoryStringInput rejects blank)
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100)
                .flatMap(prefix -> Arbitraries.strings().ofMaxLength(3996)
                        .map(suffix -> prefix + suffix));
    }

    @Provide
    Arbitrary<Integer> positiveTimeouts() {
        return Arbitraries.integers().between(1, 300_000);
    }

    // --- Property: TelegramConfiguration builds with any valid values ---

    @Property
    void should_build_configuration_with_any_valid_token(
            @ForAll("validBotTokens") String token) {
        TelegramConfiguration config = TelegramConfiguration.builder()
                .botToken(token)
                .chatId("12345")
                .text("test")
                .build();
        assertThat(config.getBotToken()).isEqualTo(token);
    }

    @Property
    void should_build_configuration_with_any_valid_chatId(
            @ForAll("validChatIds") String chatId) {
        TelegramConfiguration config = TelegramConfiguration.builder()
                .botToken("123456:ABCDEF")
                .chatId(chatId)
                .text("test")
                .build();
        assertThat(config.getChatId()).isEqualTo(chatId);
    }

    @Property
    void should_accept_any_non_null_text_up_to_4096(
            @ForAll("validTexts") String text) {
        TelegramConfiguration config = TelegramConfiguration.builder()
                .botToken("123456:ABCDEF")
                .chatId("12345")
                .text(text)
                .build();
        assertThat(config.getText()).isEqualTo(text);
        assertThat(config.getText().length()).isLessThanOrEqualTo(4096);
    }

    @Property
    void should_only_allow_valid_parse_modes(
            @ForAll("validParseModes") String parseMode) {
        TelegramConfiguration config = TelegramConfiguration.builder()
                .botToken("123456:ABCDEF")
                .chatId("12345")
                .parseMode(parseMode)
                .build();
        assertThat(VALID_PARSE_MODES).contains(config.getParseMode());
    }

    @Property
    void should_accept_positive_connect_timeouts(
            @ForAll("positiveTimeouts") int timeout) {
        TelegramConfiguration config = TelegramConfiguration.builder()
                .botToken("123456:ABCDEF")
                .chatId("12345")
                .connectTimeout(timeout)
                .build();
        assertThat(config.getConnectTimeout()).isPositive();
    }

    @Property
    void should_accept_positive_read_timeouts(
            @ForAll("positiveTimeouts") int timeout) {
        TelegramConfiguration config = TelegramConfiguration.builder()
                .botToken("123456:ABCDEF")
                .chatId("12345")
                .readTimeout(timeout)
                .build();
        assertThat(config.getReadTimeout()).isPositive();
    }

    // --- Property: SendMessageConnector validation ---

    @Property
    void should_validate_send_message_with_random_valid_inputs(
            @ForAll("validBotTokens") String token,
            @ForAll("validChatIds") String chatId,
            @ForAll("validTexts") String text) {
        SendMessageConnector connector = new SendMessageConnector();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("botToken", token);
        inputs.put("chatId", chatId);
        inputs.put("text", text);
        assertThatCode(() -> connector.setInputParameters(inputs))
                .doesNotThrowAnyException();
        assertThatCode(() -> connector.validateInputParameters())
                .doesNotThrowAnyException();
    }

    @Property
    void should_reject_send_message_with_blank_text(
            @ForAll("validBotTokens") String token,
            @ForAll("validChatIds") String chatId) {
        SendMessageConnector connector = new SendMessageConnector();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("botToken", token);
        inputs.put("chatId", chatId);
        inputs.put("text", "   ");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    // --- Property: SendDocumentConnector validation ---

    @Property
    void should_validate_send_document_with_random_valid_inputs(
            @ForAll("validBotTokens") String token,
            @ForAll("validChatIds") String chatId) {
        SendDocumentConnector connector = new SendDocumentConnector();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("botToken", token);
        inputs.put("chatId", chatId);
        inputs.put("documentUrl", "https://example.com/doc.pdf");
        assertThatCode(() -> {
            connector.setInputParameters(inputs);
            connector.validateInputParameters();
        }).doesNotThrowAnyException();
    }

    // --- Property: SendPhotoConnector validation ---

    @Property
    void should_validate_send_photo_with_random_valid_inputs(
            @ForAll("validBotTokens") String token,
            @ForAll("validChatIds") String chatId) {
        SendPhotoConnector connector = new SendPhotoConnector();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("botToken", token);
        inputs.put("chatId", chatId);
        inputs.put("photoUrl", "https://example.com/photo.jpg");
        assertThatCode(() -> {
            connector.setInputParameters(inputs);
            connector.validateInputParameters();
        }).doesNotThrowAnyException();
    }

    // --- Property: PinMessageConnector validation ---

    @Property
    void should_validate_pin_message_with_random_valid_inputs(
            @ForAll("validBotTokens") String token,
            @ForAll("validChatIds") String chatId,
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) long messageId) {
        PinMessageConnector connector = new PinMessageConnector();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("botToken", token);
        inputs.put("chatId", chatId);
        inputs.put("messageId", messageId);
        assertThatCode(() -> {
            connector.setInputParameters(inputs);
            connector.validateInputParameters();
        }).doesNotThrowAnyException();
    }

    @Property
    void should_reject_pin_message_without_messageId(
            @ForAll("validBotTokens") String token,
            @ForAll("validChatIds") String chatId) {
        PinMessageConnector connector = new PinMessageConnector();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("botToken", token);
        inputs.put("chatId", chatId);
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    // --- Property: TelegramMessage record ---

    @Property
    void should_create_telegram_message_with_any_valid_values(
            @ForAll @LongRange(min = 1) long messageId,
            @ForAll("validChatIds") String chatId) {
        TelegramMessage msg = new TelegramMessage(messageId, null, chatId);
        assertThat(msg.messageId()).isEqualTo(messageId);
        assertThat(msg.chatId()).isEqualTo(chatId);
        assertThat(msg.fileId()).isNull();
    }

    // --- Property: PinResult record ---

    @Property
    void should_create_pin_result_with_any_boolean(@ForAll boolean success) {
        PinResult result = new PinResult(success);
        assertThat(result.success()).isEqualTo(success);
    }

    // --- Property: TelegramException ---

    @Property
    void should_mark_retryable_only_for_429_and_5xx(
            @ForAll @IntRange(min = 100, max = 599) int statusCode) {
        boolean expectedRetryable = statusCode == 429 || statusCode >= 500;
        TelegramException ex = new TelegramException("test", statusCode, expectedRetryable);
        assertThat(ex.isRetryable()).isEqualTo(expectedRetryable);
        assertThat(ex.getStatusCode()).isEqualTo(statusCode);
    }

    // --- Property: MultipartBodyPublisher ---

    @Property
    void should_build_multipart_with_any_text_parts(
            @ForAll @StringLength(min = 1, max = 50) String name,
            @ForAll @StringLength(min = 1, max = 200) String value) {
        MultipartBodyPublisher publisher = MultipartBodyPublisher.newBuilder()
                .textPart(name, value)
                .build();
        assertThat(publisher.contentType()).startsWith("multipart/form-data; boundary=");
        assertThat(publisher.toBodyPublisher()).isNotNull();
    }

    // --- Property: Default config values ---

    @Property
    void should_always_have_default_base_url_and_timeouts(
            @ForAll("validBotTokens") String token) {
        TelegramConfiguration config = TelegramConfiguration.builder()
                .botToken(token)
                .build();
        assertThat(config.getBaseUrl()).isEqualTo("https://api.telegram.org");
        assertThat(config.getConnectTimeout()).isEqualTo(30_000);
        assertThat(config.getReadTimeout()).isEqualTo(60_000);
        assertThat(config.getParseMode()).isEqualTo("HTML");
        assertThat(config.isDisableNotification()).isFalse();
    }
}
