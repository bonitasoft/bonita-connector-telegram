package com.bonitasoft.connectors.telegram;

import lombok.extern.slf4j.Slf4j;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

@Slf4j
public abstract class AbstractTelegramConnector extends AbstractConnector {

    protected TelegramClient client;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            buildConfiguration();
        } catch (IllegalArgumentException e) {
            throw new ConnectorValidationException(e.getMessage());
        }
    }

    @Override
    public void connect() throws ConnectorException {
        TelegramConfiguration config = buildConfiguration();
        client = new TelegramClient(config, new RetryPolicy());
    }

    @Override
    public final void executeBusinessLogic() throws ConnectorException {
        try {
            doExecute();
            setOutputParameter("success", true);
            setOutputParameter("errorMessage", "");
        } catch (TelegramException e) {
            log.error("Telegram API error: {}", e.getMessage());
            setOutputParameter("success", false);
            setOutputParameter("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            setOutputParameter("success", false);
            setOutputParameter("errorMessage", "Unexpected error: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() throws ConnectorException {}

    protected abstract void doExecute() throws TelegramException;
    protected abstract TelegramConfiguration buildConfiguration();

    protected String readStringInput(String name) {
        Object val = getInputParameter(name);
        return val != null ? val.toString() : null;
    }

    protected String readMandatoryStringInput(String name) {
        String val = readStringInput(name);
        if (val == null || val.isBlank()) throw new IllegalArgumentException("'" + name + "' is mandatory.");
        return val;
    }

    protected Integer readIntegerInput(String name, int defaultValue) {
        Object val = getInputParameter(name);
        if (val == null) return defaultValue;
        if (val instanceof Integer i) return i;
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return defaultValue; }
    }

    protected Boolean readBooleanInput(String name, boolean defaultValue) {
        Object val = getInputParameter(name);
        if (val == null) return defaultValue;
        if (val instanceof Boolean b) return b;
        return Boolean.parseBoolean(val.toString());
    }

    protected Long readLongInput(String name) {
        Object val = getInputParameter(name);
        if (val == null) return null;
        if (val instanceof Long l) return l;
        if (val instanceof Integer i) return i.longValue();
        try { return Long.parseLong(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    protected String resolveToken() {
        String token = readStringInput("botToken");
        if (token != null && !token.isBlank()) return token;
        String sysProp = System.getProperty("telegram.bot.token");
        if (sysProp != null && !sysProp.isBlank()) return sysProp;
        String env = System.getenv("TELEGRAM_BOT_TOKEN");
        if (env != null && !env.isBlank()) return env;
        throw new IllegalArgumentException("Bot token not found. Set botToken, JVM prop 'telegram.bot.token', or env var 'TELEGRAM_BOT_TOKEN'.");
    }
}
