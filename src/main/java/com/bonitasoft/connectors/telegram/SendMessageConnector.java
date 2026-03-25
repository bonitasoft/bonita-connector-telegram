package com.bonitasoft.connectors.telegram;

public class SendMessageConnector extends AbstractTelegramConnector {

    static final String CHAT_ID = "chatId";
    static final String TEXT = "text";
    static final String PARSE_MODE = "parseMode";
    static final String DISABLE_NOTIFICATION = "disableNotification";

    @Override
    protected void doExecute() throws TelegramException {
        TelegramConfiguration cfg = buildConfiguration();
        TelegramMessage result = client.sendMessage(cfg);
        setOutputParameter("messageId", result.messageId());
        setOutputParameter("chatId", result.chatId());
    }

    @Override
    protected TelegramConfiguration buildConfiguration() {
        String token = resolveToken();
        String text = readMandatoryStringInput(TEXT);
        if (text.length() > 4096) {
            throw new IllegalArgumentException("Message text exceeds 4096 characters limit. Length: " + text.length());
        }
        return TelegramConfiguration.builder()
                .botToken(token)
                .baseUrl(readStringInput("baseUrl") != null ? readStringInput("baseUrl") : "https://api.telegram.org")
                .connectTimeout(readIntegerInput("connectTimeout", 30_000))
                .readTimeout(readIntegerInput("readTimeout", 60_000))
                .chatId(readMandatoryStringInput(CHAT_ID))
                .text(text)
                .parseMode(readStringInput(PARSE_MODE) != null ? readStringInput(PARSE_MODE) : "HTML")
                .disableNotification(readBooleanInput(DISABLE_NOTIFICATION, false))
                .build();
    }
}
