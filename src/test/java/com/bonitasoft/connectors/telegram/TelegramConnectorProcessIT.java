package com.bonitasoft.connectors.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.bonitasoft.web.client.BonitaClient;
import org.bonitasoft.web.client.api.ArchivedProcessInstanceApi;
import org.bonitasoft.web.client.api.ProcessInstanceApi;
import org.bonitasoft.web.client.exception.NotFoundException;
import org.bonitasoft.web.client.model.ArchivedProcessInstance;
import org.bonitasoft.web.client.services.policies.OrganizationImportPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Process-based integration tests for Telegram connectors.
 *
 * These tests build a Bonita process containing the connector, deploy it
 * to a Docker Bonita instance, and verify the connector executes correctly
 * within the process engine.
 *
 * Requires:
 * - Docker running
 * - TELEGRAM_BOT_TOKEN environment variable set
 * - TELEGRAM_CHAT_ID environment variable set
 * - Project built with mvn package (JAR must exist in target/)
 */
@Testcontainers
@EnabledIfEnvironmentVariable(named = "TELEGRAM_BOT_TOKEN", matches = ".+")
class TelegramConnectorProcessIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramConnectorProcessIT.class);

    // Connector definition IDs and versions (must match pom.xml properties)
    private static final String SEND_MESSAGE_DEF_ID = "telegram-send-message";
    private static final String SEND_MESSAGE_DEF_VERSION = "1.0.0";

    private static final String SEND_DOCUMENT_DEF_ID = "telegram-send-document";
    private static final String SEND_DOCUMENT_DEF_VERSION = "1.0.0";

    private static final String SEND_PHOTO_DEF_ID = "telegram-send-photo";
    private static final String SEND_PHOTO_DEF_VERSION = "1.0.0";

    private static final String PIN_MESSAGE_DEF_ID = "telegram-pin-message";
    private static final String PIN_MESSAGE_DEF_VERSION = "1.0.0";

    @Container
    static GenericContainer<?> BONITA_CONTAINER = new GenericContainer<>(
            DockerImageName.parse("bonita:10.2.0"))
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/bonita"))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER));

    private BonitaClient client;

    @BeforeAll
    static void installOrganization() {
        var client = BonitaClient
                .builder(String.format("http://%s:%s/bonita",
                        BONITA_CONTAINER.getHost(),
                        BONITA_CONTAINER.getFirstMappedPort()))
                .build();
        client.login("install", "install");
        client.users().importOrganization(
                new File(TelegramConnectorProcessIT.class.getResource("/ACME.xml").getFile()),
                OrganizationImportPolicy.IGNORE_DUPLICATES);
        client.logout();
    }

    @BeforeEach
    void login() {
        client = BonitaClient
                .builder(String.format("http://%s:%s/bonita",
                        BONITA_CONTAINER.getHost(),
                        BONITA_CONTAINER.getFirstMappedPort()))
                .build();
        client.login("install", "install");
    }

    @AfterEach
    void logout() {
        client.logout();
    }

    @Test
    void should_sendMessage_when_validInputsProvided() throws Exception {
        var inputs = commonInputs();
        inputs.put("text", "Process-based IT test message");
        inputs.put("parseMode", "HTML");

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultMessageId", ConnectorTestToolkit.Output.create("messageId", Long.class.getName()));

        var bar = ConnectorTestToolkit.buildConnectorToTest(
                SEND_MESSAGE_DEF_ID, SEND_MESSAGE_DEF_VERSION, inputs, outputs);
        var response = ConnectorTestToolkit.importAndLaunchProcess(bar, client);

        await().until(pollInstanceState(response.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client, response.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    @Test
    void should_sendDocument_when_validUrlProvided() throws Exception {
        var inputs = commonInputs();
        inputs.put("documentUrl", "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");
        inputs.put("caption", "Process-based IT test document");
        inputs.put("parseMode", "HTML");

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultMessageId", ConnectorTestToolkit.Output.create("messageId", Long.class.getName()));

        var bar = ConnectorTestToolkit.buildConnectorToTest(
                SEND_DOCUMENT_DEF_ID, SEND_DOCUMENT_DEF_VERSION, inputs, outputs);
        var response = ConnectorTestToolkit.importAndLaunchProcess(bar, client);

        await().until(pollInstanceState(response.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client, response.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    @Test
    void should_sendPhoto_when_validUrlProvided() throws Exception {
        var inputs = commonInputs();
        inputs.put("photoUrl", "https://via.placeholder.com/150");
        inputs.put("caption", "Process-based IT test photo");
        inputs.put("parseMode", "HTML");

        var outputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultMessageId", ConnectorTestToolkit.Output.create("messageId", Long.class.getName()));

        var bar = ConnectorTestToolkit.buildConnectorToTest(
                SEND_PHOTO_DEF_ID, SEND_PHOTO_DEF_VERSION, inputs, outputs);
        var response = ConnectorTestToolkit.importAndLaunchProcess(bar, client);

        await().until(pollInstanceState(response.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client, response.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    @Test
    void should_pinMessage_when_validMessageIdProvided() throws Exception {
        // First send a message to get a messageId to pin
        var sendInputs = commonInputs();
        sendInputs.put("text", "Message to be pinned by IT");
        sendInputs.put("parseMode", "HTML");

        var sendOutputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultMessageId", ConnectorTestToolkit.Output.create("messageId", Long.class.getName()));

        var sendBar = ConnectorTestToolkit.buildConnectorToTest(
                SEND_MESSAGE_DEF_ID, SEND_MESSAGE_DEF_VERSION, sendInputs, sendOutputs);
        var sendResponse = ConnectorTestToolkit.importAndLaunchProcess(sendBar, client);

        await().until(pollInstanceState(sendResponse.getCaseId()), "started"::equals);

        var messageId = ConnectorTestToolkit.getProcessVariableValue(client, sendResponse.getCaseId(), "resultMessageId");
        assertThat(messageId).isNotNull();

        // Now pin that message
        var pinInputs = commonInputs();
        pinInputs.put("messageId", messageId);

        var pinOutputs = Map.of(
                "resultSuccess", ConnectorTestToolkit.Output.create("success", Boolean.class.getName()),
                "resultPinned", ConnectorTestToolkit.Output.create("pinned", Boolean.class.getName()));

        var pinBar = ConnectorTestToolkit.buildConnectorToTest(
                PIN_MESSAGE_DEF_ID, PIN_MESSAGE_DEF_VERSION, pinInputs, pinOutputs);
        var pinResponse = ConnectorTestToolkit.importAndLaunchProcess(pinBar, client);

        await().until(pollInstanceState(pinResponse.getCaseId()), "started"::equals);

        var success = ConnectorTestToolkit.getProcessVariableValue(client, pinResponse.getCaseId(), "resultSuccess");
        assertThat(success).isEqualTo("true");
    }

    private Map<String, String> commonInputs() {
        var inputs = new HashMap<String, String>();
        inputs.put("botToken", System.getenv("TELEGRAM_BOT_TOKEN"));
        inputs.put("chatId", System.getenv("TELEGRAM_CHAT_ID"));
        return inputs;
    }

    private Callable<String> pollInstanceState(String id) {
        return () -> {
            try {
                var instance = client.get(ProcessInstanceApi.class)
                        .getProcessInstanceById(id, (String) null);
                return instance.getState().name().toLowerCase();
            } catch (NotFoundException e) {
                return getCompletedProcess(id).getState().name().toLowerCase();
            }
        };
    }

    private ArchivedProcessInstance getCompletedProcess(String id) {
        var archivedInstances = client.get(ArchivedProcessInstanceApi.class)
                .searchArchivedProcessInstances(
                        new ArchivedProcessInstanceApi.SearchArchivedProcessInstancesQueryParams()
                                .c(1)
                                .p(0)
                                .f(List.of("caller=any", "sourceObjectId=" + id)));
        if (!archivedInstances.isEmpty()) {
            return archivedInstances.get(0);
        }
        return null;
    }
}
