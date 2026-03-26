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
class PinMessageConnectorTest {

    @Mock private TelegramClient mockClient;
    private PinMessageConnector connector;

    @BeforeEach
    void setUp() { connector = new PinMessageConnector(); }

    private void injectMockClient() throws Exception {
        var field = AbstractTelegramConnector.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(connector, mockClient);
    }

    private Map<String, Object> validInputs() {
        Map<String, Object> m = new HashMap<>();
        m.put("botToken", "123456:ABC-DEF");
        m.put("chatId", "-1001234567890");
        m.put("messageId", 42L);
        return m;
    }

    @Test void should_pin_message_when_valid() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.pinMessage(any())).thenReturn(new PinResult(true));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
        assertThat(TestHelper.getOutputs(connector).get("pinned")).isEqualTo(true);
    }

    @Test void should_fail_when_messageId_missing() {
        Map<String, Object> m = validInputs(); m.remove("messageId");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_fail_when_chatId_missing() {
        Map<String, Object> m = validInputs(); m.remove("chatId");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_handle_api_error() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.pinMessage(any())).thenThrow(
                new TelegramException("Bad Request: not enough rights to pin a message", 400, false));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(false);
        assertThat((String) TestHelper.getOutputs(connector).get("errorMessage")).contains("not enough rights");
    }

    @Test void should_accept_integer_messageId() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("messageId", 42); // Integer instead of Long
        connector.setInputParameters(m);
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.pinMessage(any())).thenReturn(new PinResult(true));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }

    @Test void should_accept_string_messageId() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("messageId", "42"); // String
        connector.setInputParameters(m);
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.pinMessage(any())).thenReturn(new PinResult(true));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }

    @Test void should_fail_when_messageId_is_invalid_string() {
        Map<String, Object> m = validInputs();
        m.put("messageId", "not-a-number");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("messageId");
    }

    @Test void should_handle_unexpected_exception() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.pinMessage(any())).thenThrow(new RuntimeException("something"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(false);
        assertThat((String) TestHelper.getOutputs(connector).get("errorMessage")).contains("Unexpected error");
    }

    @Test void should_fail_when_botToken_missing() {
        Map<String, Object> m = validInputs(); m.remove("botToken");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_handle_server_error_500() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.pinMessage(any())).thenThrow(new TelegramException("Internal Server Error", 500, true));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(false);
    }

    @Test void should_populate_all_output_fields_on_success() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.pinMessage(any())).thenReturn(new PinResult(true));
        connector.executeBusinessLogic();
        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("errorMessage")).isEqualTo("");
        assertThat(outputs.get("pinned")).isEqualTo(true);
    }

    @Test void should_populate_all_output_fields_on_error() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.pinMessage(any())).thenThrow(new TelegramException("Not enough rights", 400, false));
        connector.executeBusinessLogic();
        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).isNotEmpty();
    }

    @Test void should_use_custom_timeouts() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("connectTimeout", 5000);
        m.put("readTimeout", 10000);
        m.put("disableNotification", true);
        connector.setInputParameters(m);
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.pinMessage(any())).thenReturn(new PinResult(true));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }

    @Test void should_connect_and_disconnect() throws Exception {
        connector.setInputParameters(validInputs());
        connector.connect();
        connector.disconnect();
    }

    @Test void should_use_custom_baseUrl() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("baseUrl", "https://custom-proxy.example.com");
        connector.setInputParameters(m);
        connector.validateInputParameters();
    }

    @Test void should_fail_when_chatId_is_blank() {
        Map<String, Object> m = validInputs();
        m.put("chatId", "   ");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_fail_when_botToken_is_blank() {
        Map<String, Object> m = validInputs();
        m.put("botToken", "   ");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_accept_negative_chatId_for_groups() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("chatId", "-1001999999999");
        connector.setInputParameters(m);
        connector.validateInputParameters();
    }

    @Test void should_accept_null_disableNotification() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("disableNotification", null);
        connector.setInputParameters(m);
        connector.validateInputParameters();
    }

    @Test void should_parse_string_timeout() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("connectTimeout", "5000");
        connector.setInputParameters(m);
        connector.validateInputParameters();
    }

    @Test void should_use_default_for_invalid_timeout() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("connectTimeout", "not-a-number");
        connector.setInputParameters(m);
        connector.validateInputParameters();
    }

    @Test void should_use_boolean_string_disableNotification() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("disableNotification", "true");
        connector.setInputParameters(m);
        connector.validateInputParameters();
    }

    @Test void should_accept_large_messageId() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("messageId", 999999999L);
        connector.setInputParameters(m);
        connector.validateInputParameters();
    }

    // --- Mutant killer: buildConfiguration baseUrl default ---

    @Test void should_use_default_baseUrl_when_null() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("baseUrl", null);
        connector.setInputParameters(m);
        injectMockClient();
        when(mockClient.pinMessage(any())).thenAnswer(inv -> {
            TelegramConfiguration cfg = inv.getArgument(0);
            assertThat(cfg.getBaseUrl()).isEqualTo("https://api.telegram.org");
            return new PinResult(true);
        });
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }

    @Test void should_use_custom_baseUrl_when_set() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("baseUrl", "https://custom.example.com");
        connector.setInputParameters(m);
        injectMockClient();
        when(mockClient.pinMessage(any())).thenAnswer(inv -> {
            TelegramConfiguration cfg = inv.getArgument(0);
            assertThat(cfg.getBaseUrl()).isEqualTo("https://custom.example.com");
            return new PinResult(true);
        });
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }
}
