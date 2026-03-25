package com.bonitasoft.connectors.telegram;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TelegramConfiguration {
    private String botToken;
    @Builder.Default
    private String baseUrl = "https://api.telegram.org";
    @Builder.Default
    private int connectTimeout = 30_000;
    @Builder.Default
    private int readTimeout = 60_000;
    private String chatId;
    @Builder.Default
    private String parseMode = "HTML";
    @Builder.Default
    private boolean disableNotification = false;
    private String text;
    private String documentUrl;
    private String photoUrl;
    private String caption;
    private Long messageId;
}
