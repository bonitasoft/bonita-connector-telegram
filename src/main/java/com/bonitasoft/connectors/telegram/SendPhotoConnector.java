package com.bonitasoft.connectors.telegram;

public class SendPhotoConnector extends AbstractTelegramConnector {

    static final String CHAT_ID = "chatId";
    static final String PHOTO_URL = "photoUrl";
    static final String CAPTION = "caption";
    static final String PARSE_MODE = "parseMode";
    static final String DISABLE_NOTIFICATION = "disableNotification";

    @Override
    protected void doExecute() throws TelegramException {
        TelegramConfiguration cfg = buildConfiguration();
        TelegramMessage result = client.sendPhoto(cfg);
        setOutputParameter("messageId", result.messageId());
        setOutputParameter("fileId", result.fileId());
        setOutputParameter("chatId", result.chatId());
    }

    @Override
    protected TelegramConfiguration buildConfiguration() {
        String token = resolveToken();
        return TelegramConfiguration.builder()
                .botToken(token)
                .baseUrl(readStringInput("baseUrl") != null ? readStringInput("baseUrl") : "https://api.telegram.org")
                .connectTimeout(readIntegerInput("connectTimeout", 30_000))
                .readTimeout(readIntegerInput("readTimeout", 60_000))
                .chatId(readMandatoryStringInput(CHAT_ID))
                .photoUrl(readMandatoryStringInput(PHOTO_URL))
                .caption(readStringInput(CAPTION))
                .parseMode(readStringInput(PARSE_MODE) != null ? readStringInput(PARSE_MODE) : "HTML")
                .disableNotification(readBooleanInput(DISABLE_NOTIFICATION, false))
                .build();
    }
}
