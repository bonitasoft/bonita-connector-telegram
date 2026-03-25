package com.bonitasoft.connectors.telegram;

public record TelegramMessage(long messageId, String fileId, String chatId) {}
