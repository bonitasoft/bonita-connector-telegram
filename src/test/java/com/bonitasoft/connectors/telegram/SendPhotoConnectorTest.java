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
class SendPhotoConnectorTest {

    @Mock private TelegramClient mockClient;
    private SendPhotoConnector connector;

    @BeforeEach
    void setUp() { connector = new SendPhotoConnector(); }

    private void injectMockClient() throws Exception {
        var field = AbstractTelegramConnector.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(connector, mockClient);
    }

    private Map<String, Object> validInputs() {
        Map<String, Object> m = new HashMap<>();
        m.put("botToken", "123456:ABC-DEF");
        m.put("chatId", "-1001234567890");
        m.put("photoUrl", "https://example.com/photo.jpg");
        return m;
    }

    @Test void should_send_photo_when_valid() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendPhoto(any())).thenReturn(new TelegramMessage(46L, "AgACAgIAAxkB", "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
        assertThat(TestHelper.getOutputs(connector).get("messageId")).isEqualTo(46L);
        assertThat(TestHelper.getOutputs(connector).get("fileId")).isEqualTo("AgACAgIAAxkB");
    }

    @Test void should_fail_when_photoUrl_missing() {
        Map<String, Object> m = validInputs(); m.remove("photoUrl");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_fail_when_chatId_missing() {
        Map<String, Object> m = validInputs(); m.remove("chatId");
        connector.setInputParameters(m);
        assertThatThrownBy(() -> connector.validateInputParameters()).isInstanceOf(ConnectorValidationException.class);
    }

    @Test void should_send_photo_with_caption() throws Exception {
        Map<String, Object> m = validInputs();
        m.put("caption", "Process diagram");
        connector.setInputParameters(m);
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendPhoto(any())).thenReturn(new TelegramMessage(47L, "AgACAgIAAxkC", "-1001234567890"));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(true);
    }

    @Test void should_handle_api_error() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();
        when(mockClient.sendPhoto(any())).thenThrow(new TelegramException("Bad Request: wrong file identifier", 400, false));
        connector.executeBusinessLogic();
        assertThat(TestHelper.getOutputs(connector).get("success")).isEqualTo(false);
    }
}
