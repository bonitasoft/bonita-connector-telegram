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
 * WireMock integration tests for the PinMessage operation end-to-end.
 */
class PinMessageConnectorIntegrationTest {

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

    private PinMessageConnector createConnector(Map<String, Object> inputs) throws Exception {
        var connector = new PinMessageConnector();
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
        m.put("messageId", 42L);
        return m;
    }

    @Test
    void should_pin_message_successfully() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
                .willReturn(okJson("""
                        {"ok":true,"result":true}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("pinned")).isEqualTo(true);
    }

    @Test
    void should_handle_message_not_found() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
                .willReturn(aResponse().withStatus(400).withBody("""
                        {"ok":false,"error_code":400,"description":"Bad Request: message to pin not found"}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("message to pin not found");
    }

    @Test
    void should_handle_rate_limit_429() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
                .inScenario("rateLimit").whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(429).withBody("""
                        {"ok":false,"error_code":429,"description":"Too Many Requests: retry after 1","parameters":{"retry_after":1}}
                        """))
                .willSetStateTo("retried"));
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
                .inScenario("rateLimit").whenScenarioStateIs("retried")
                .willReturn(okJson("""
                        {"ok":true,"result":true}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("pinned")).isEqualTo(true);
    }

    @Test
    void should_handle_unauthorized_401() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
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
    void should_handle_server_error_500() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
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
    void should_handle_forbidden_403() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
                .willReturn(aResponse().withStatus(403).withBody("""
                        {"ok":false,"error_code":403,"description":"Forbidden: not enough rights to pin a message"}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("not enough rights");
    }

    @Test
    void should_pin_with_disable_notification() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
                .willReturn(okJson("""
                        {"ok":true,"result":true}
                        """)));

        var inputs = validInputs();
        inputs.put("disableNotification", true);
        var connector = createConnector(inputs);
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("pinned")).isEqualTo(true);
    }

    @Test
    void should_handle_bad_request_with_malformed_json() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/pinChatMessage"))
                .willReturn(aResponse().withStatus(502).withBody("not json at all")));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).isNotEmpty();
    }
}
