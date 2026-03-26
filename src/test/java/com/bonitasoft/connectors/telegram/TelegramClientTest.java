package com.bonitasoft.connectors.telegram;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelegramClientTest {

    private WireMockServer wireMock;
    private TelegramClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        baseUrl = "http://localhost:" + wireMock.port();
        TelegramConfiguration config = TelegramConfiguration.builder()
                .botToken("test-token")
                .baseUrl(baseUrl)
                .connectTimeout(5000)
                .readTimeout(5000)
                .build();
        client = new TelegramClient(config, new RetryPolicy() {
            @Override void sleep(long millis) {}
        });
    }

    @AfterEach
    void tearDown() { wireMock.stop(); }

    @Test
    void should_send_message_successfully() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":42,"chat":{"id":-1001234567890}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-1001234567890").text("Hello!")
                .parseMode("HTML")
                .build();
        TelegramMessage result = client.sendMessage(cfg);
        assertThat(result.messageId()).isEqualTo(42L);
        assertThat(result.chatId()).isEqualTo("-1001234567890");
        assertThat(result.fileId()).isNull();
    }

    @Test
    void should_send_document_successfully() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":43,"chat":{"id":-1001234567890},"document":{"file_id":"BQACAgIAAxkB","file_name":"report.pdf"}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-1001234567890")
                .documentUrl("https://example.com/report.pdf")
                .caption("Monthly report")
                .build();
        TelegramMessage result = client.sendDocument(cfg);
        assertThat(result.messageId()).isEqualTo(43L);
        assertThat(result.fileId()).isEqualTo("BQACAgIAAxkB");
    }

    @Test
    void should_send_photo_successfully() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":44,"chat":{"id":-1001234567890},"photo":[{"file_id":"small","width":90},{"file_id":"AgACAgIAAxkB","width":800}]}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-1001234567890")
                .photoUrl("https://example.com/photo.jpg")
                .build();
        TelegramMessage result = client.sendPhoto(cfg);
        assertThat(result.messageId()).isEqualTo(44L);
        assertThat(result.fileId()).isEqualTo("AgACAgIAAxkB"); // highest resolution
    }

    @Test
    void should_pin_message_successfully() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
                .willReturn(okJson("""
                        {"ok":true,"result":true}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-1001234567890")
                .messageId(42L)
                .build();
        PinResult result = client.pinMessage(cfg);
        assertThat(result.success()).isTrue();
    }

    @Test
    void should_handle_error_response() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(aResponse().withStatus(400).withBody("""
                        {"ok":false,"error_code":400,"description":"Bad Request: chat not found"}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("invalid").text("test")
                .build();
        assertThatThrownBy(() -> client.sendMessage(cfg))
                .isInstanceOf(TelegramException.class)
                .satisfies(e -> {
                    TelegramException te = (TelegramException) e;
                    assertThat(te.getStatusCode()).isEqualTo(400);
                    assertThat(te.getMessage()).contains("chat not found");
                });
    }

    @Test
    void should_handle_429_with_retry_after() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .inScenario("retry").whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(429).withBody("""
                        {"ok":false,"error_code":429,"description":"Too Many Requests: retry after 35","parameters":{"retry_after":35}}
                        """))
                .willSetStateTo("retried"));
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .inScenario("retry").whenScenarioStateIs("retried")
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":50,"chat":{"id":-100}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").text("retry test")
                .build();
        TelegramMessage result = client.sendMessage(cfg);
        assertThat(result.messageId()).isEqualTo(50L);
    }

    @Test
    void should_handle_forbidden_error() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .willReturn(aResponse().withStatus(403).withBody("""
                        {"ok":false,"error_code":403,"description":"Forbidden: bot is not a member of the channel chat"}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").documentUrl("https://example.com/f.pdf")
                .build();
        assertThatThrownBy(() -> client.sendDocument(cfg))
                .isInstanceOf(TelegramException.class)
                .satisfies(e -> assertThat(((TelegramException) e).getStatusCode()).isEqualTo(403));
    }

    @Test
    void should_send_message_with_disable_notification() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":51,"chat":{"id":-100}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").text("Silent")
                .disableNotification(true)
                .build();
        TelegramMessage result = client.sendMessage(cfg);
        assertThat(result.messageId()).isEqualTo(51L);
    }

    @Test
    void should_send_photo_with_caption() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":52,"chat":{"id":-100},"photo":[{"file_id":"ph1","width":200}]}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").photoUrl("https://example.com/img.png")
                .caption("Check this out")
                .parseMode("HTML")
                .build();
        TelegramMessage result = client.sendPhoto(cfg);
        assertThat(result.messageId()).isEqualTo(52L);
        assertThat(result.fileId()).isEqualTo("ph1");
    }

    @Test
    void should_handle_malformed_error_response() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(aResponse().withStatus(502).withBody("not json at all")));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").text("test")
                .build();
        assertThatThrownBy(() -> client.sendMessage(cfg))
                .isInstanceOf(TelegramException.class)
                .satisfies(e -> assertThat(((TelegramException) e).getStatusCode()).isEqualTo(502));
    }

    @Test
    void should_handle_ok_false_in_200_response() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(okJson("""
                        {"ok":false,"error_code":400,"description":"Bad Request: message text is empty"}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").text("test")
                .build();
        assertThatThrownBy(() -> client.sendMessage(cfg))
                .isInstanceOf(TelegramException.class)
                .hasMessageContaining("message text is empty");
    }

    @Test
    void should_send_document_without_caption() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":55,"chat":{"id":-100},"document":{"file_id":"doc1"}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").documentUrl("https://example.com/f.pdf")
                .parseMode("")
                .build();
        TelegramMessage result = client.sendDocument(cfg);
        assertThat(result.messageId()).isEqualTo(55L);
    }

    // --- Mutant killers: parseMode conditional in sendMessage (line 51) ---

    @Test
    void should_include_parse_mode_in_sendMessage_when_set() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .withRequestBody(containing("\"parse_mode\":\"HTML\""))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":70,"chat":{"id":-100}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").text("test").parseMode("HTML")
                .build();
        TelegramMessage result = client.sendMessage(cfg);
        assertThat(result.messageId()).isEqualTo(70L);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/bottest-token/sendMessage"))
                .withRequestBody(containing("\"parse_mode\":\"HTML\"")));
    }

    @Test
    void should_not_include_parse_mode_in_sendMessage_when_null() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":71,"chat":{"id":-100}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").text("test").parseMode(null)
                .build();
        TelegramMessage result = client.sendMessage(cfg);
        assertThat(result.messageId()).isEqualTo(71L);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/bottest-token/sendMessage"))
                .withRequestBody(notMatching(".*parse_mode.*")));
    }

    @Test
    void should_not_include_parse_mode_in_sendMessage_when_blank() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":72,"chat":{"id":-100}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").text("test").parseMode("  ")
                .build();
        TelegramMessage result = client.sendMessage(cfg);
        assertThat(result.messageId()).isEqualTo(72L);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/bottest-token/sendMessage"))
                .withRequestBody(notMatching(".*parse_mode.*")));
    }

    // --- Mutant killers: caption & parseMode conditionals in sendDocument (lines 70, 73) ---

    @Test
    void should_include_caption_in_sendDocument_when_set() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":73,"chat":{"id":-100},"document":{"file_id":"d1"}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").documentUrl("https://example.com/f.pdf")
                .caption("My caption").parseMode("HTML")
                .build();
        TelegramMessage result = client.sendDocument(cfg);
        assertThat(result.messageId()).isEqualTo(73L);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/bottest-token/sendDocument"))
                .withRequestBody(containing("My caption"))
                .withRequestBody(containing("parse_mode")));
    }

    @Test
    void should_not_include_caption_in_sendDocument_when_null() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":74,"chat":{"id":-100},"document":{"file_id":"d2"}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").documentUrl("https://example.com/f.pdf")
                .caption(null).parseMode(null)
                .build();
        TelegramMessage result = client.sendDocument(cfg);
        assertThat(result.messageId()).isEqualTo(74L);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/bottest-token/sendDocument"))
                .withRequestBody(notMatching(".*caption.*"))
                .withRequestBody(notMatching(".*parse_mode.*")));
    }

    @Test
    void should_not_include_caption_in_sendDocument_when_blank() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":75,"chat":{"id":-100},"document":{"file_id":"d3"}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").documentUrl("https://example.com/f.pdf")
                .caption("   ").parseMode("   ")
                .build();
        TelegramMessage result = client.sendDocument(cfg);
        assertThat(result.messageId()).isEqualTo(75L);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/bottest-token/sendDocument"))
                .withRequestBody(notMatching(".*caption.*"))
                .withRequestBody(notMatching(".*parse_mode.*")));
    }

    // --- Mutant killers: caption & parseMode conditionals in sendPhoto (lines 93, 96) ---

    @Test
    void should_include_caption_in_sendPhoto_when_set() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":76,"chat":{"id":-100},"photo":[{"file_id":"p1","width":200}]}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").photoUrl("https://example.com/img.jpg")
                .caption("Photo caption").parseMode("HTML")
                .build();
        TelegramMessage result = client.sendPhoto(cfg);
        assertThat(result.messageId()).isEqualTo(76L);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/bottest-token/sendPhoto"))
                .withRequestBody(containing("Photo caption"))
                .withRequestBody(containing("parse_mode")));
    }

    @Test
    void should_not_include_caption_in_sendPhoto_when_null() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":77,"chat":{"id":-100},"photo":[{"file_id":"p2","width":200}]}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").photoUrl("https://example.com/img.jpg")
                .caption(null).parseMode(null)
                .build();
        TelegramMessage result = client.sendPhoto(cfg);
        assertThat(result.messageId()).isEqualTo(77L);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/bottest-token/sendPhoto"))
                .withRequestBody(notMatching(".*caption.*"))
                .withRequestBody(notMatching(".*parse_mode.*")));
    }

    @Test
    void should_not_include_caption_in_sendPhoto_when_blank() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":78,"chat":{"id":-100},"photo":[{"file_id":"p3","width":200}]}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").photoUrl("https://example.com/img.jpg")
                .caption("  ").parseMode("  ")
                .build();
        TelegramMessage result = client.sendPhoto(cfg);
        assertThat(result.messageId()).isEqualTo(78L);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/bottest-token/sendPhoto"))
                .withRequestBody(notMatching(".*caption.*"))
                .withRequestBody(notMatching(".*parse_mode.*")));
    }

    // --- Mutant killer: sendRequest boundary (line 162) statusCode < 200 ---

    @Test
    void should_succeed_on_status_200() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(aResponse().withStatus(200).withBody("""
                        {"ok":true,"result":{"message_id":80,"chat":{"id":-100}}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").text("test")
                .build();
        TelegramMessage result = client.sendMessage(cfg);
        assertThat(result.messageId()).isEqualTo(80L);
    }

    // --- Mutant killer: handleErrorResponse VoidMethodCall (line 206) ---

    @Test
    void should_propagate_error_from_json_error_response_with_httpCode() {
        // When handleErrorResponse parses JSON, it passes httpCode to handleErrorFromBody.
        // The mutant removes this call, falling through to sendRequest's own ok-check,
        // which would use error_code from body (0 if absent) instead of HTTP status code.
        // By omitting error_code from the body, we can detect the difference.
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(aResponse().withStatus(502).withBody("""
                        {"ok":false,"description":"Bad Gateway"}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").text("test")
                .build();
        assertThatThrownBy(() -> client.sendMessage(cfg))
                .isInstanceOf(TelegramException.class)
                .satisfies(e -> {
                    TelegramException te = (TelegramException) e;
                    // Normal path: httpCode=502 is passed, errorCode=json.path("error_code").asInt(502)=502
                    // Mutant path: handleErrorFromBody(json) uses json.path("error_code").asInt(0)=0
                    assertThat(te.getStatusCode()).isEqualTo(502);
                    assertThat(te.getMessage()).contains("Bad Gateway");
                });
    }

    @Test
    void should_handle_error_json_response_with_429_and_retry_after() {
        // Verify that a JSON error response with retry_after is properly parsed
        // (kills handleErrorResponse VoidMethodCall mutant for retryable path)
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(aResponse().withStatus(429).withBody("""
                        {"ok":false,"error_code":429,"description":"Too Many Requests","parameters":{"retry_after":30}}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").text("test")
                .build();
        // With retry policy, it retries on 429. After MAX_RETRIES it throws.
        assertThatThrownBy(() -> client.sendMessage(cfg))
                .isInstanceOf(TelegramException.class)
                .satisfies(e -> {
                    TelegramException te = (TelegramException) e;
                    assertThat(te.isRetryable()).isTrue();
                    assertThat(te.getStatusCode()).isEqualTo(429);
                });
    }

    @Test
    void should_retry_on_500_then_succeed() {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
                .inScenario("retry500").whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500).withBody("""
                        {"ok":false,"error_code":500,"description":"Internal Server Error"}
                        """))
                .willSetStateTo("retried"));
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
                .inScenario("retry500").whenScenarioStateIs("retried")
                .willReturn(okJson("""
                        {"ok":true,"result":true}
                        """)));
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl(baseUrl).botToken("test-token")
                .chatId("-100").messageId(42L)
                .build();
        PinResult result = client.pinMessage(cfg);
        assertThat(result.success()).isTrue();
    }

    // --- Mutant killer: sendRequest InterruptedException path (line 176) ---

    @SuppressWarnings("unchecked")
    @Test
    void should_handle_interrupted_exception_in_sendRequest() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        when(mockHttpClient.send(any(), any())).thenThrow(new InterruptedException("test interrupt"));
        TelegramConfiguration config = TelegramConfiguration.builder()
                .botToken("test-token").baseUrl("http://localhost:9999")
                .connectTimeout(1000).readTimeout(1000)
                .build();
        TelegramClient mockClient = new TelegramClient(mockHttpClient, new ObjectMapper(), config, new RetryPolicy() {
            @Override void sleep(long millis) {}
        });
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl("http://localhost:9999").botToken("test-token")
                .chatId("-100").text("test")
                .build();
        assertThatThrownBy(() -> mockClient.sendMessage(cfg))
                .isInstanceOf(TelegramException.class)
                .hasMessageContaining("interrupted");
        // Verify interrupt flag was restored
        assertThat(Thread.interrupted()).isTrue();
    }

    // --- Mutant killer: sendRequest IOException path ---

    @SuppressWarnings("unchecked")
    @Test
    void should_handle_io_exception_in_sendRequest() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        when(mockHttpClient.send(any(), any())).thenThrow(new IOException("connection refused"));
        TelegramConfiguration config = TelegramConfiguration.builder()
                .botToken("test-token").baseUrl("http://localhost:9999")
                .connectTimeout(1000).readTimeout(1000)
                .build();
        TelegramClient mockClient = new TelegramClient(mockHttpClient, new ObjectMapper(), config, new RetryPolicy() {
            @Override void sleep(long millis) {}
        });
        TelegramConfiguration cfg = TelegramConfiguration.builder()
                .baseUrl("http://localhost:9999").botToken("test-token")
                .chatId("-100").text("test")
                .build();
        assertThatThrownBy(() -> mockClient.sendMessage(cfg))
                .isInstanceOf(TelegramException.class)
                .hasMessageContaining("Network error");
    }
}
