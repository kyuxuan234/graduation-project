package com.example.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnvFileLoaderTest {

    private static final String KEY = "MAIL_PASSWORD_UNIT_TEST";

    @AfterEach
    void clear() {
        System.clearProperty(KEY);
    }

    @Test
    void applyLine_setsSystemProperty() {
        EnvFileLoader.applyLine(KEY + "=secret-value");
        assertEquals("secret-value", System.getProperty(KEY));
    }

    @Test
    void applyLine_stripsQuotesAndSkipsComments() {
        EnvFileLoader.applyLine("# " + KEY + "=nope");
        assertNull(System.getProperty(KEY));
        EnvFileLoader.applyLine(KEY + "=\"quoted\"");
        assertEquals("quoted", System.getProperty(KEY));
    }
}
