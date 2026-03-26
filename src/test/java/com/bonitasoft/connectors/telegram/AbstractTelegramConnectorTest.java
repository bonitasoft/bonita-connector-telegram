package com.bonitasoft.connectors.telegram;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AbstractTelegramConnectorTest {

    private SendMessageConnector connector() {
        return new SendMessageConnector();
    }

    @SuppressWarnings("unchecked")
    private Object getOutput(SendMessageConnector c, String name) throws Exception {
        Method m = org.bonitasoft.engine.connector.AbstractConnector.class.getDeclaredMethod("getOutputParameters");
        m.setAccessible(true);
        return ((Map<String, Object>) m.invoke(c)).get(name);
    }

    private Map<String, Object> validInputs() {
        var m = new HashMap<String, Object>();
        m.put("botToken", "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11");
        m.put("chatId", "-100123456789");
        m.put("text", "Hello");
        return m;
    }

    // --- readBooleanInput ---

    @Nested class ReadBooleanInput {
        private Boolean invoke(Object value) throws Exception {
            var c = connector();
            var inputs = validInputs();
            inputs.put("disableNotification", value);
            c.setInputParameters(inputs);
            Method m = AbstractTelegramConnector.class.getDeclaredMethod("readBooleanInput", String.class, boolean.class);
            m.setAccessible(true);
            return (Boolean) m.invoke(c, "disableNotification", false);
        }

        @Test void should_return_default_when_null() throws Exception {
            assertThat(invoke(null)).isFalse();
        }

        @Test void should_return_true_default_when_null() throws Exception {
            var c = connector();
            c.setInputParameters(validInputs());
            Method m = AbstractTelegramConnector.class.getDeclaredMethod("readBooleanInput", String.class, boolean.class);
            m.setAccessible(true);
            assertThat((Boolean) m.invoke(c, "disableNotification", true)).isTrue();
        }

        @Test void should_return_boolean_true() throws Exception {
            assertThat(invoke(Boolean.TRUE)).isTrue();
        }

        @Test void should_return_boolean_false() throws Exception {
            assertThat(invoke(Boolean.FALSE)).isFalse();
        }

        @Test void should_parse_string_true() throws Exception {
            assertThat(invoke("true")).isTrue();
        }

        @Test void should_parse_string_false() throws Exception {
            assertThat(invoke("false")).isFalse();
        }

        @Test void should_parse_invalid_string_as_false() throws Exception {
            assertThat(invoke("yes")).isFalse();
        }
    }

    // --- readIntegerInput ---

    @Nested class ReadIntegerInput {
        private Integer invoke(Object value) throws Exception {
            var c = connector();
            var inputs = validInputs();
            inputs.put("connectTimeout", value);
            c.setInputParameters(inputs);
            Method m = AbstractTelegramConnector.class.getDeclaredMethod("readIntegerInput", String.class, int.class);
            m.setAccessible(true);
            return (Integer) m.invoke(c, "connectTimeout", 10000);
        }

        @Test void should_return_default_when_null() throws Exception {
            assertThat(invoke(null)).isEqualTo(10000);
        }

        @Test void should_return_integer_directly() throws Exception {
            assertThat(invoke(5000)).isEqualTo(5000);
        }

        @Test void should_parse_string() throws Exception {
            assertThat(invoke("7500")).isEqualTo(7500);
        }

        @Test void should_return_default_for_invalid_string() throws Exception {
            assertThat(invoke("not-a-number")).isEqualTo(10000);
        }

        @Test void should_return_zero() throws Exception {
            assertThat(invoke(0)).isEqualTo(0);
        }
    }

    // --- readLongInput ---

    @Nested class ReadLongInput {
        private Long invoke(Object value) throws Exception {
            var c = connector();
            var inputs = validInputs();
            inputs.put("messageId", value);
            c.setInputParameters(inputs);
            Method m = AbstractTelegramConnector.class.getDeclaredMethod("readLongInput", String.class);
            m.setAccessible(true);
            return (Long) m.invoke(c, "messageId");
        }

        @Test void should_return_null_when_null() throws Exception {
            assertThat(invoke(null)).isNull();
        }

        @Test void should_return_long_directly() throws Exception {
            assertThat(invoke(42L)).isEqualTo(42L);
        }

        @Test void should_convert_integer_to_long() throws Exception {
            assertThat(invoke(42)).isEqualTo(42L);
        }

        @Test void should_parse_string() throws Exception {
            assertThat(invoke("12345")).isEqualTo(12345L);
        }

        @Test void should_return_null_for_invalid_string() throws Exception {
            assertThat(invoke("abc")).isNull();
        }
    }

    // --- resolveToken ---

    @Nested class ResolveToken {
        @Test void should_resolve_from_input() throws Exception {
            var c = connector();
            c.setInputParameters(validInputs());
            Method m = AbstractTelegramConnector.class.getDeclaredMethod("resolveToken");
            m.setAccessible(true);
            assertThat((String) m.invoke(c)).isEqualTo("123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11");
        }

        @Test void should_fallback_to_system_property() throws Exception {
            var c = connector();
            var inputs = validInputs();
            inputs.put("botToken", null);
            c.setInputParameters(inputs);
            System.setProperty("telegram.bot.token", "sysprop-token");
            try {
                Method m = AbstractTelegramConnector.class.getDeclaredMethod("resolveToken");
                m.setAccessible(true);
                assertThat((String) m.invoke(c)).isEqualTo("sysprop-token");
            } finally {
                System.clearProperty("telegram.bot.token");
            }
        }

        @Test void should_throw_when_all_empty() {
            var c = connector();
            var inputs = validInputs();
            inputs.put("botToken", "  ");
            c.setInputParameters(inputs);
            assertThatThrownBy(() -> c.validateInputParameters())
                    .isInstanceOf(ConnectorValidationException.class);
        }

        @Test void should_skip_blank_input() throws Exception {
            var c = connector();
            var inputs = validInputs();
            inputs.put("botToken", "");
            c.setInputParameters(inputs);
            System.setProperty("telegram.bot.token", "fallback");
            try {
                Method m = AbstractTelegramConnector.class.getDeclaredMethod("resolveToken");
                m.setAccessible(true);
                assertThat((String) m.invoke(c)).isEqualTo("fallback");
            } finally {
                System.clearProperty("telegram.bot.token");
            }
        }

        @Test void should_skip_blank_sysprop() {
            var c = connector();
            var inputs = validInputs();
            inputs.put("botToken", "");
            c.setInputParameters(inputs);
            System.setProperty("telegram.bot.token", "  ");
            try {
                assertThatThrownBy(() -> c.validateInputParameters())
                        .isInstanceOf(ConnectorValidationException.class);
            } finally {
                System.clearProperty("telegram.bot.token");
            }
        }
    }

    // --- readStringInput / readMandatoryStringInput ---

    @Nested class StringInputs {
        @Test void should_return_null_for_null_input() throws Exception {
            var c = connector();
            var inputs = validInputs();
            inputs.put("parseMode", null);
            c.setInputParameters(inputs);
            Method m = AbstractTelegramConnector.class.getDeclaredMethod("readStringInput", String.class);
            m.setAccessible(true);
            assertThat(m.invoke(c, "parseMode")).isNull();
        }

        @Test void should_return_string_for_non_null() throws Exception {
            var c = connector();
            var inputs = validInputs();
            inputs.put("parseMode", "HTML");
            c.setInputParameters(inputs);
            Method m = AbstractTelegramConnector.class.getDeclaredMethod("readStringInput", String.class);
            m.setAccessible(true);
            assertThat(m.invoke(c, "parseMode")).isEqualTo("HTML");
        }

        @Test void should_reject_blank_mandatory_input() {
            var c = connector();
            var inputs = validInputs();
            inputs.put("text", "  ");
            c.setInputParameters(inputs);
            assertThatThrownBy(() -> c.validateInputParameters())
                    .isInstanceOf(ConnectorValidationException.class);
        }

        @Test void should_reject_null_mandatory_input() {
            var c = connector();
            var inputs = validInputs();
            inputs.put("text", null);
            c.setInputParameters(inputs);
            assertThatThrownBy(() -> c.validateInputParameters())
                    .isInstanceOf(ConnectorValidationException.class);
        }
    }

    // --- executeBusinessLogic paths ---

    @Nested class ExecuteBusinessLogic {
        @Test void should_set_success_true_on_happy_path() throws Exception {
            var c = connector();
            var inputs = validInputs();
            c.setInputParameters(inputs);

            // Inject mock client
            var mockClient = mock(TelegramClient.class);
            when(mockClient.sendMessage(any())).thenReturn(new TelegramMessage(1L, null, "-100123456789"));
            var field = AbstractTelegramConnector.class.getDeclaredField("client");
            field.setAccessible(true);
            field.set(c, mockClient);

            c.executeBusinessLogic();

            assertThat(getOutput(c, "success")).isEqualTo(true);
            assertThat(getOutput(c, "errorMessage")).isEqualTo("");
        }

        @Test void should_set_success_false_on_telegram_error() throws Exception {
            var c = connector();
            c.setInputParameters(validInputs());

            var mockClient = mock(TelegramClient.class);
            when(mockClient.sendMessage(any())).thenThrow(new TelegramException("Bot blocked", 403, false));
            var field = AbstractTelegramConnector.class.getDeclaredField("client");
            field.setAccessible(true);
            field.set(c, mockClient);

            c.executeBusinessLogic();

            assertThat(getOutput(c, "success")).isEqualTo(false);
            assertThat(getOutput(c, "errorMessage")).asString().contains("Bot blocked");
        }

        @Test void should_set_success_false_on_unexpected_error() throws Exception {
            var c = connector();
            c.setInputParameters(validInputs());

            var mockClient = mock(TelegramClient.class);
            when(mockClient.sendMessage(any())).thenThrow(new RuntimeException("NullPointer"));
            var field = AbstractTelegramConnector.class.getDeclaredField("client");
            field.setAccessible(true);
            field.set(c, mockClient);

            c.executeBusinessLogic();

            assertThat(getOutput(c, "success")).isEqualTo(false);
            assertThat(getOutput(c, "errorMessage")).asString().contains("Unexpected error");
        }
    }
}
