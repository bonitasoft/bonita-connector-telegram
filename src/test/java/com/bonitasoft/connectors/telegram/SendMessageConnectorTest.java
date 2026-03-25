package com.bonitasoft.connectors.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendMessageConnectorTest {

    @Mock private TelegramClient mockClient;
    private SendMessageConnector connector;

    @BeforeEach
    void setUp() { connector = new SendMessageConnector(); }

    private void injectMockClient() throws Exception {
        var field = AbstractTelegramConnector.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(connector, mockClient);
    }

    private Map<String, Object> validInputs() {
        Map<String, Object> m = new HashMap<>();
        m.put("botToken", "123456:ABC-DEF");
        m.put("chatId", "-1001234567890");
        m.put("text", "Hello from Bonita BPM!");
        return m;
    }

    @Test void should_send_message_when_valid() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendMessage(any())).thenReturn(new TelegramMessage(42L, null, "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
        assertThat(TestHelper.getOutputs(connector).get("messageId")).isEqualTo(42L);
        assertThat(TestHelper.getOutputs(connector).get("chatId")).isEqualTo("-1001234567890");
    }

    @Test void should_fail_when_text_missing() {
        Map<String, Object> m = validInputs(); m.remove("text");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_fail_when_text_exceeds_4096() {
        Map<String, Object> m = validInputs(); m.put("text", "x".repeat(4097));
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class).hasMessageContaining("4096");
    }

    @Test void should_fail_when_chatId_missing() {
        Map<String, Object> m = validInputs(); m.remove("chatId");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_fail_when_botToken_missing() {
        Map<String, Object> m = validInputs(); m.remove("botToken");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_set_error_on_api_failure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendMessage(any())).thenThrow(
                new TelegramException("Bad Request: chat not found", 400, false));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(false);
        assertThat((String) TestHelper.getOutputs(connector).get("errorMessage")).contains("chat not found");
    }

    @Test void should_handle_server_error() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendMessage(any())).thenThrow(new TelegramException("Internal Server Error", 500, true));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(false);
    }

    @Test void should_use_default_parseMode() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendMessage(any())).thenReturn(new TelegramMessage(43L, null, "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }

    @Test void should_handle_unexpected_exception() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendMessage(any())).thenThrow(new RuntimeException("something unexpected"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(false);
        assertThat((String) TestHelper.getOutputs(connector).get("errorMessage")).contains("Unexpected error");
    }

    @Test void should_use_custom_timeouts() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("connectTimeout", 5000);
        m.put("readTimeout", 10000);
        m.put("parseMode", "MarkdownV2");
        m.put("disableNotification", true);
        connector.setInputParameters(m);
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendMessage(any())).thenReturn(new TelegramMessage(44L, null, "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }

    @Test void should_parse_string_timeout() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("connectTimeout", "5000");
        connector.setInputParameters(m);
        connector.validateInputParameters();
        // Just test that validation passes with string-valued timeout
    }

    @Test void should_use_default_for_invalid_timeout() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("connectTimeout", "not-a-number");
        connector.setInputParameters(m);
        connector.validateInputParameters();
        // Should use default value without error
    }

    @Test void should_connect_and_disconnect() throws Exception {
        connector.setInputParameters(validInputs());
        connector.connect();
        connector.disconnect();
        // Just verify no exception
    }

    @Test void should_use_custom_baseUrl() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("baseUrl", "https://custom-proxy.example.com");
        connector.setInputParameters(m);
        connector.validateInputParameters();
    }

    @Test void should_use_boolean_string_input() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("disableNotification", "true");
        connector.setInputParameters(m);
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendMessage(any())).thenReturn(new TelegramMessage(45L, null, "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }
}
