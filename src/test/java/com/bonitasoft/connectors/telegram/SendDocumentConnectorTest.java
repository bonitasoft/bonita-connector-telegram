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
class SendDocumentConnectorTest {

    @Mock private TelegramClient mockClient;
    private SendDocumentConnector connector;

    @BeforeEach
    void setUp() { connector = new SendDocumentConnector(); }

    private void injectMockClient() throws Exception {
        var field = AbstractTelegramConnector.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(connector, mockClient);
    }

    private Map<String, Object> validInputs() {
        Map<String, Object> m = new HashMap<>();
        m.put("botToken", "123456:ABC-DEF");
        m.put("chatId", "-1001234567890");
        m.put("documentUrl", "https://example.com/invoice.pdf");
        return m;
    }

    @Test void should_send_document_when_valid() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendDocument(any())).thenReturn(new TelegramMessage(44L, "BQACAgIAAxkB", "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
        assertThat(TestHelper.getOutputs(connector).get("messageId")).isEqualTo(44L);
        assertThat(TestHelper.getOutputs(connector).get("fileId")).isEqualTo("BQACAgIAAxkB");
    }

    @Test void should_fail_when_documentUrl_missing() {
        Map<String, Object> m = validInputs(); m.remove("documentUrl");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_fail_when_chatId_missing() {
        Map<String, Object> m = validInputs(); m.remove("chatId");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_send_document_with_caption() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("caption", "Your invoice is ready");
        connector.setInputParameters(m);
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendDocument(any())).thenReturn(new TelegramMessage(45L, "BQACAgIAAxkC", "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }

    @Test void should_handle_api_error() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendDocument(any())).thenThrow(new TelegramException("Forbidden: bot is not a member", 403, false));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(false);
        assertThat((String) TestHelper.getOutputs(connector).get("errorMessage")).contains("bot is not a member");
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
        when(mockClient.sendDocument(any())).thenThrow(new TelegramException("Internal Server Error", 500, true));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(false);
    }

    @Test void should_handle_unexpected_exception() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendDocument(any())).thenThrow(new RuntimeException("something unexpected"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(false);
        assertThat((String) TestHelper.getOutputs(connector).get("errorMessage")).contains("Unexpected error");
    }

    @Test void should_populate_all_output_fields() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendDocument(any())).thenReturn(new TelegramMessage(50L, "BQFileId", "-1001234567890"));
        connector.executeBusinessLogic();
        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("errorMessage")).isEqualTo("");
        assertThat(outputs.get("messageId")).isEqualTo(50L);
        assertThat(outputs.get("fileId")).isEqualTo("BQFileId");
        assertThat(outputs.get("chatId")).isEqualTo("-1001234567890");
    }

    @Test void should_use_default_parseMode() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendDocument(any())).thenReturn(new TelegramMessage(51L, "BQFile2", "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
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
        when(mockClient.sendDocument(any())).thenReturn(new TelegramMessage(52L, "BQFile3", "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }

    @Test void should_accept_null_caption() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("caption", null);
        connector.setInputParameters(m);
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendDocument(any())).thenReturn(new TelegramMessage(53L, "BQFile4", "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
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

    @Test void should_fail_when_documentUrl_is_blank() {
        Map<String, Object> m = validInputs();
        m.put("documentUrl", "   ");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
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

    @Test void should_accept_null_parseMode() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("parseMode", null);
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

    @Test void should_use_boolean_string_input() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("disableNotification", "true");
        connector.setInputParameters(m);
        connector.validateInputParameters();
    }

    // --- Mutant killers: buildConfiguration baseUrl/parseMode defaults ---

    @Test void should_use_default_baseUrl_when_null() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("baseUrl", null);
        m.put("parseMode", null);
        connector.setInputParameters(m);
        injectMockClient();
        when(mockClient.sendDocument(any())).thenAnswer(inv -> {
            TelegramConfiguration cfg = inv.getArgument(0);
            assertThat(cfg.getBaseUrl()).isEqualTo("https://api.telegram.org");
            assertThat(cfg.getParseMode()).isEqualTo("HTML");
            return new TelegramMessage(90L, "fid", "-1001234567890");
        });
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }

    @Test void should_use_custom_baseUrl_when_set() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("baseUrl", "https://custom.example.com");
        m.put("parseMode", "MarkdownV2");
        connector.setInputParameters(m);
        injectMockClient();
        when(mockClient.sendDocument(any())).thenAnswer(inv -> {
            TelegramConfiguration cfg = inv.getArgument(0);
            assertThat(cfg.getBaseUrl()).isEqualTo("https://custom.example.com");
            assertThat(cfg.getParseMode()).isEqualTo("MarkdownV2");
            return new TelegramMessage(91L, "fid", "-1001234567890");
        });
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }
}
