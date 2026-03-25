package com.bonitasoft.connectors.telegram;

public class PinMessageConnector extends AbstractTelegramConnector {

    static final String CHAT_ID = "chatId";
    static final String MESSAGE_ID = "messageId";
    static final String DISABLE_NOTIFICATION = "disableNotification";

    @Override
    protected void doExecute() throws TelegramException {
        TelegramConfiguration cfg = buildConfiguration();
        PinResult result = client.pinMessage(cfg);
        setOutputParameter("pinned", result.success());
    }

    @Override
    protected TelegramConfiguration buildConfiguration() {
        String token = resolveToken();
        Long msgId = readLongInput(MESSAGE_ID);
        if (msgId == null) {
            throw new IllegalArgumentException("Parameter 'messageId' is mandatory and must be a valid number.");
        }
        return TelegramConfiguration.builder()
                .botToken(token)
                .baseUrl(readStringInput("baseUrl") != null ? readStringInput("baseUrl") : "https://api.telegram.org")
                .connectTimeout(readIntegerInput("connectTimeout", 30_000))
                .readTimeout(readIntegerInput("readTimeout", 60_000))
                .chatId(readMandatoryStringInput(CHAT_ID))
                .messageId(msgId)
                .disableNotification(readBooleanInput(DISABLE_NOTIFICATION, false))
                .build();
    }
}
