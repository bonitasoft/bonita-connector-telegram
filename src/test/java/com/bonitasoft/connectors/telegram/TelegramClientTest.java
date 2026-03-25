package com.bonitasoft.connectors.telegram;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
