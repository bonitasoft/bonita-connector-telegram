package com.bonitasoft.connectors.telegram;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WireMock integration tests for the SendMessage operation end-to-end.
 */
class SendMessageConnectorIntegrationTest {

    private WireMockServer wireMock;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        baseUrl = "http://localhost:" + wireMock.port();
    }

    @AfterEach
    void tearDown() { wireMock.stop(); }

    private SendMessageConnector createConnector(Map<String, Object> inputs) throws Exception {
        var connector = new SendMessageConnector();
        inputs.putIfAbsent("baseUrl", baseUrl);
        inputs.putIfAbsent("botToken", "test-token");
        inputs.putIfAbsent("connectTimeout", 5000);
        inputs.putIfAbsent("readTimeout", 5000);
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        connector.connect();
        return connector;
    }

    private Map<String, Object> validInputs() {
        var m = new HashMap<String, Object>();
        m.put("chatId", "-1001234567890");
        m.put("text", "Hello from Bonita!");
        return m;
    }

    @Test
    void should_send_message_successfully() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":100,"chat":{"id":-1001234567890}}}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("messageId")).isEqualTo(100L);
        assertThat(outputs.get("chatId")).isEqualTo("-1001234567890");
        assertThat(outputs.get("errorMessage")).isEqualTo("");
    }

    @Test
    void should_handle_rate_limit_429() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .inScenario("rateLimit").whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(429).withBody("""
                        {"ok":false,"error_code":429,"description":"Too Many Requests: retry after 1","parameters":{"retry_after":1}}
                        """))
                .willSetStateTo("retried"));
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .inScenario("rateLimit").whenScenarioStateIs("retried")
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":101,"chat":{"id":-1001234567890}}}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("messageId")).isEqualTo(101L);
    }

    @Test
    void should_handle_unauthorized_401() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(aResponse().withStatus(401).withBody("""
                        {"ok":false,"error_code":401,"description":"Unauthorized"}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Unauthorized");
    }

    @Test
    void should_handle_forbidden_403() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(aResponse().withStatus(403).withBody("""
                        {"ok":false,"error_code":403,"description":"Forbidden: bot was blocked by the user"}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("bot was blocked");
    }

    @Test
    void should_handle_server_error_500() throws Exception {
        // All retries fail with 500
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(aResponse().withStatus(500).withBody("""
                        {"ok":false,"error_code":500,"description":"Internal Server Error"}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Internal Server Error");
    }

    @Test
    void should_handle_bad_request_400() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(aResponse().withStatus(400).withBody("""
                        {"ok":false,"error_code":400,"description":"Bad Request: chat not found"}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("chat not found");
    }

    @Test
    void should_send_message_with_disable_notification() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":103,"chat":{"id":-1001234567890}}}
                        """)));

        var inputs = validInputs();
        inputs.put("disableNotification", true);
        var connector = createConnector(inputs);
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("messageId")).isEqualTo(103L);
    }

    @Test
    void should_send_message_with_markdownV2() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":104,"chat":{"id":-1001234567890}}}
                        """)));

        var inputs = validInputs();
        inputs.put("parseMode", "MarkdownV2");
        var connector = createConnector(inputs);
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
    }
}
