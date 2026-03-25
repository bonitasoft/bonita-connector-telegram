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
 * WireMock integration tests for the SendPhoto operation end-to-end.
 */
class SendPhotoConnectorIntegrationTest {

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

    private SendPhotoConnector createConnector(Map<String, Object> inputs) throws Exception {
        var connector = new SendPhotoConnector();
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
        m.put("photoUrl", "https://example.com/photo.jpg");
        return m;
    }

    @Test
    void should_send_photo_successfully() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":300,"chat":{"id":-1001234567890},"photo":[{"file_id":"small","width":90},{"file_id":"AgACLargeRes","width":1280}]}}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("messageId")).isEqualTo(300L);
        assertThat(outputs.get("fileId")).isEqualTo("AgACLargeRes");
        assertThat(outputs.get("chatId")).isEqualTo("-1001234567890");
    }

    @Test
    void should_handle_rate_limit_429() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .inScenario("rateLimit").whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(429).withBody("""
                        {"ok":false,"error_code":429,"description":"Too Many Requests: retry after 1","parameters":{"retry_after":1}}
                        """))
                .willSetStateTo("retried"));
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .inScenario("rateLimit").whenScenarioStateIs("retried")
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":301,"chat":{"id":-100},"photo":[{"file_id":"AgRetry","width":800}]}}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("fileId")).isEqualTo("AgRetry");
    }

    @Test
    void should_handle_invalid_photo_url() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .willReturn(aResponse().withStatus(400).withBody("""
                        {"ok":false,"error_code":400,"description":"Bad Request: wrong file identifier/HTTP URL specified"}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("wrong file identifier");
    }

    @Test
    void should_handle_unauthorized_401() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
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
    void should_return_highest_resolution_file_id() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":302,"chat":{"id":-100},"photo":[
                            {"file_id":"thumb","width":90,"height":90},
                            {"file_id":"medium","width":320,"height":320},
                            {"file_id":"AgACHighRes","width":1280,"height":1280}
                        ]}}
                        """)));

        var inputs = validInputs();
        inputs.put("caption", "High-res test");
        var connector = createConnector(inputs);
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("fileId")).isEqualTo("AgACHighRes");
    }

    @Test
    void should_handle_server_error_500() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
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
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
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
    void should_send_photo_with_caption_and_parseMode() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendPhoto"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":303,"chat":{"id":-100},"photo":[{"file_id":"AgCaption","width":800}]}}
                        """)));

        var inputs = validInputs();
        inputs.put("caption", "Process diagram");
        inputs.put("parseMode", "HTML");
        var connector = createConnector(inputs);
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("fileId")).isEqualTo("AgCaption");
    }
}
