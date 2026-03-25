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
 * WireMock integration tests for the SendDocument operation end-to-end.
 */
class SendDocumentConnectorIntegrationTest {

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

    private SendDocumentConnector createConnector(Map<String, Object> inputs) throws Exception {
        var connector = new SendDocumentConnector();
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
        m.put("documentUrl", "https://example.com/report.pdf");
        return m;
    }

    @Test
    void should_send_document_successfully() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":200,"chat":{"id":-1001234567890},"document":{"file_id":"BQACAgIDoc123","file_name":"report.pdf"}}}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("messageId")).isEqualTo(200L);
        assertThat(outputs.get("fileId")).isEqualTo("BQACAgIDoc123");
        assertThat(outputs.get("chatId")).isEqualTo("-1001234567890");
    }

    @Test
    void should_handle_rate_limit_429() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .inScenario("rateLimit").whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(429).withBody("""
                        {"ok":false,"error_code":429,"description":"Too Many Requests: retry after 1","parameters":{"retry_after":1}}
                        """))
                .willSetStateTo("retried"));
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .inScenario("rateLimit").whenScenarioStateIs("retried")
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":201,"chat":{"id":-1001234567890},"document":{"file_id":"BQRetry"}}}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("fileId")).isEqualTo("BQRetry");
    }

    @Test
    void should_handle_invalid_document_url() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
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
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
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
    void should_return_file_id_on_success() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":202,"chat":{"id":-100},"document":{"file_id":"BQACSpecificFileId","file_name":"data.csv","file_size":12345}}}
                        """)));

        var inputs = validInputs();
        inputs.put("caption", "Monthly data export");
        var connector = createConnector(inputs);
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("fileId")).isEqualTo("BQACSpecificFileId");
        assertThat(outputs.get("messageId")).isEqualTo(202L);
    }

    @Test
    void should_handle_server_error_500() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
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
    void should_send_document_with_parseMode() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":203,"chat":{"id":-100},"document":{"file_id":"BQParse"}}}
                        """)));

        var inputs = validInputs();
        inputs.put("parseMode", "MarkdownV2");
        inputs.put("caption", "*Bold* caption");
        var connector = createConnector(inputs);
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("fileId")).isEqualTo("BQParse");
    }

    @Test
    void should_handle_forbidden_403() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo("/bottest-token/sendDocument"))
                .willReturn(aResponse().withStatus(403).withBody("""
                        {"ok":false,"error_code":403,"description":"Forbidden: bot is not a member of the channel chat"}
                        """)));

        var connector = createConnector(validInputs());
        connector.executeBusinessLogic();

        var outputs = TestHelper.getOutputs(connector);
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("bot is not a member");
    }
}
