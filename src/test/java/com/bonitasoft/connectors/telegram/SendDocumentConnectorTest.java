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
}
