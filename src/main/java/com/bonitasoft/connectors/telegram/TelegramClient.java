package com.bonitasoft.connectors.telegram;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Telegram Bot API HTTP client using java.net.HttpClient (Java 17 built-in).
 */
@Slf4j
public class TelegramClient {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final TelegramConfiguration config;
    private final RetryPolicy retryPolicy;

    public TelegramClient(TelegramConfiguration config, RetryPolicy retryPolicy) {
        this.config = config;
        this.retryPolicy = retryPolicy;
        this.mapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectTimeout()))
                .build();
    }

    // Visible for testing
    TelegramClient(HttpClient httpClient, ObjectMapper mapper,
                   TelegramConfiguration config, RetryPolicy retryPolicy) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.config = config;
        this.retryPolicy = retryPolicy;
    }

    /**
     * Send a text message via sendMessage API.
     */
    public TelegramMessage sendMessage(TelegramConfiguration cfg) throws TelegramException {
        return retryPolicy.execute(() -> {
            var body = mapper.createObjectNode();
            body.put("chat_id", cfg.getChatId());
            body.put("text", cfg.getText());
            if (cfg.getParseMode() != null && !cfg.getParseMode().isBlank()) {
                body.put("parse_mode", cfg.getParseMode());
            }
            body.put("disable_notification", cfg.isDisableNotification());

            String url = buildUrl(cfg, "sendMessage");
            JsonNode result = executePostJson(url, body);
            return extractMessage(result);
        });
    }

    /**
     * Send a document via sendDocument API (URL-based).
     */
    public TelegramMessage sendDocument(TelegramConfiguration cfg) throws TelegramException {
        return retryPolicy.execute(() -> {
            var builder = MultipartBodyPublisher.newBuilder()
                    .textPart("chat_id", cfg.getChatId())
                    .textPart("document", cfg.getDocumentUrl());
            if (cfg.getCaption() != null && !cfg.getCaption().isBlank()) {
                builder.textPart("caption", cfg.getCaption());
            }
            if (cfg.getParseMode() != null && !cfg.getParseMode().isBlank()) {
                builder.textPart("parse_mode", cfg.getParseMode());
            }
            builder.textPart("disable_notification", String.valueOf(cfg.isDisableNotification()));
            MultipartBodyPublisher multipart = builder.build();

            String url = buildUrl(cfg, "sendDocument");
            JsonNode result = executePostMultipart(url, multipart);
            return extractMessageWithFile(result, "document");
        });
    }

    /**
     * Send a photo via sendPhoto API (URL-based).
     */
    public TelegramMessage sendPhoto(TelegramConfiguration cfg) throws TelegramException {
        return retryPolicy.execute(() -> {
            var builder = MultipartBodyPublisher.newBuilder()
                    .textPart("chat_id", cfg.getChatId())
                    .textPart("photo", cfg.getPhotoUrl());
            if (cfg.getCaption() != null && !cfg.getCaption().isBlank()) {
                builder.textPart("caption", cfg.getCaption());
            }
            if (cfg.getParseMode() != null && !cfg.getParseMode().isBlank()) {
                builder.textPart("parse_mode", cfg.getParseMode());
            }
            builder.textPart("disable_notification", String.valueOf(cfg.isDisableNotification()));
            MultipartBodyPublisher multipart = builder.build();

            String url = buildUrl(cfg, "sendPhoto");
            JsonNode result = executePostMultipart(url, multipart);
            return extractMessageWithFile(result, "photo");
        });
    }

    /**
     * Pin a message in a chat via pinChatMessage API.
     */
    public PinResult pinMessage(TelegramConfiguration cfg) throws TelegramException {
        return retryPolicy.execute(() -> {
            var body = mapper.createObjectNode();
            body.put("chat_id", cfg.getChatId());
            body.put("message_id", cfg.getMessageId());
            body.put("disable_notification", cfg.isDisableNotification());

            String url = buildUrl(cfg, "pinChatMessage");
            JsonNode result = executePostJson(url, body);
            return new PinResult(result.asBoolean(true));
        });
    }

    private String buildUrl(TelegramConfiguration cfg, String method) {
        return cfg.getBaseUrl() + "/bot" + cfg.getBotToken() + "/" + method;
    }

    private JsonNode executePostJson(String url, Object body) throws TelegramException {
        byte[] bodyBytes;
        try {
            bodyBytes = mapper.writeValueAsBytes(body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new TelegramException("Failed to serialize request: " + e.getMessage(), e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build();

        return sendRequest(request);
    }

    private JsonNode executePostMultipart(String url, MultipartBodyPublisher multipart) throws TelegramException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(config.getReadTimeout()))
                .header("Content-Type", multipart.contentType())
                .POST(multipart.toBodyPublisher())
                .build();

        return sendRequest(request);
    }

    private JsonNode sendRequest(HttpRequest request) throws TelegramException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body() != null ? response.body() : "";

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                handleErrorResponse(response.statusCode(), responseBody);
            }

            JsonNode json = mapper.readTree(responseBody);
            if (!json.path("ok").asBoolean(false)) {
                handleErrorFromBody(json);
            }
            return json.path("result");
        } catch (TelegramException e) {
            throw e;
        } catch (IOException e) {
            throw new TelegramException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TelegramException("Request interrupted: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TelegramException("Unexpected error: " + e.getMessage(), e);
        }
    }

    private TelegramMessage extractMessage(JsonNode result) {
        long messageId = result.path("message_id").asLong(0);
        String chatId = String.valueOf(result.path("chat").path("id").asLong(0));
        return new TelegramMessage(messageId, null, chatId);
    }

    private TelegramMessage extractMessageWithFile(JsonNode result, String fileField) {
        long messageId = result.path("message_id").asLong(0);
        String chatId = String.valueOf(result.path("chat").path("id").asLong(0));
        String fileId = null;
        JsonNode fileNode = result.path(fileField);
        if (fileNode.isArray() && !fileNode.isEmpty()) {
            // For photos, pick the last (highest-resolution) element
            fileId = fileNode.get(fileNode.size() - 1).path("file_id").asText(null);
        } else if (fileNode.isObject()) {
            fileId = fileNode.path("file_id").asText(null);
        }
        return new TelegramMessage(messageId, fileId, chatId);
    }

    private void handleErrorResponse(int httpCode, String responseBody) throws TelegramException {
        try {
            JsonNode json = mapper.readTree(responseBody);
            handleErrorFromBody(json, httpCode);
        } catch (TelegramException e) {
            throw e;
        } catch (Exception e) {
            throw new TelegramException("HTTP " + httpCode + ": " + responseBody, httpCode, false);
        }
    }

    private void handleErrorFromBody(JsonNode json) throws TelegramException {
        handleErrorFromBody(json, json.path("error_code").asInt(0));
    }

    private void handleErrorFromBody(JsonNode json, int httpCode) throws TelegramException {
        int errorCode = json.path("error_code").asInt(httpCode);
        String description = json.path("description").asText("Unknown error");
        Integer retryAfter = null;
        JsonNode params = json.path("parameters");
        if (params.has("retry_after")) {
            retryAfter = params.path("retry_after").asInt(0);
        }
        boolean retryable = httpCode == 429 || httpCode >= 500;
        throw new TelegramException(
                String.format("Telegram API error %d: %s", errorCode, description),
                errorCode, retryable, retryAfter);
    }
}
