package com.bonitasoft.connectors.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Integration tests that send real messages via the Telegram Bot API.
 * Only runs when TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID environment variables are set.
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "TELEGRAM_BOT_TOKEN", matches = ".+")
class TelegramConnectorIT {

    private String botToken;
    private String chatId;

    @BeforeEach
    void setUp() {
        botToken = System.getenv("TELEGRAM_BOT_TOKEN");
        chatId = System.getenv("TELEGRAM_CHAT_ID");
        assertThat(botToken).as("TELEGRAM_BOT_TOKEN must be set").isNotBlank();
        assertThat(chatId).as("TELEGRAM_CHAT_ID must be set").isNotBlank();
    }

    @Test
    void should_send_message_to_real_chat() throws Exception {
        SendMessageConnector connector = new SendMessageConnector();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("botToken", botToken);
        inputs.put("chatId", chatId);
        inputs.put("text", "<b>Integration Test</b> - Bonita Telegram Connector");
        inputs.put("parseMode", "HTML");
        inputs.put("disableNotification", true);

        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        connector.connect();
        connector.executeBusinessLogic();
        connector.disconnect();

        Map<String, Object> outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("errorMessage")).isEqualTo("");
        assertThat((Long) outputs.get("messageId")).isPositive();
        assertThat(outputs.get("chatId")).isNotNull();
    }

    @Test
    void should_send_photo_by_url() throws Exception {
        SendPhotoConnector connector = new SendPhotoConnector();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("botToken", botToken);
        inputs.put("chatId", chatId);
        inputs.put("photoUrl", "https://www.bonitasoft.com/favicon.ico");
        inputs.put("caption", "IT - Send Photo Test");
        inputs.put("disableNotification", true);

        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        connector.connect();
        connector.executeBusinessLogic();
        connector.disconnect();

        Map<String, Object> outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("errorMessage")).isEqualTo("");
        assertThat((Long) outputs.get("messageId")).isPositive();
    }

    @Test
    void should_pin_sent_message() throws Exception {
        // First, send a message to get a messageId
        SendMessageConnector sendConnector = new SendMessageConnector();
        Map<String, Object> sendInputs = new HashMap<>();
        sendInputs.put("botToken", botToken);
        sendInputs.put("chatId", chatId);
        sendInputs.put("text", "IT - Message to pin");
        sendInputs.put("disableNotification", true);

        sendConnector.setInputParameters(sendInputs);
        sendConnector.validateInputParameters();
        sendConnector.connect();
        sendConnector.executeBusinessLogic();
        sendConnector.disconnect();

        Map<String, Object> sendOutputs = TestHelper.getOutputs(sendConnector);
        assertThat(sendOutputs.get("success")).isEqualTo(true);
        Long messageId = (Long) sendOutputs.get("messageId");
        assertThat(messageId).isPositive();

        // Now pin that message
        PinMessageConnector pinConnector = new PinMessageConnector();
        Map<String, Object> pinInputs = new HashMap<>();
        pinInputs.put("botToken", botToken);
        pinInputs.put("chatId", chatId);
        pinInputs.put("messageId", messageId);
        pinInputs.put("disableNotification", true);

        pinConnector.setInputParameters(pinInputs);
        pinConnector.validateInputParameters();
        pinConnector.connect();
        pinConnector.executeBusinessLogic();
        pinConnector.disconnect();

        Map<String, Object> pinOutputs = TestHelper.getOutputs(pinConnector);
        assertThat(pinOutputs.get("success")).isEqualTo(true);
        assertThat(pinOutputs.get("pinned")).isEqualTo(true);
    }
}
