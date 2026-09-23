package com.example.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialCipherTest {

    @Test
    void encryptsAndDecryptsWhenKeyPresent() throws Exception {
        CredentialCipher cipher = cipher("dev-credential-key");
        String stored = cipher.encrypt("secret");
        assertTrue(cipher.isEncrypted(stored));
        assertEquals("secret", cipher.decrypt(stored));
        assertEquals("plain-history", cipher.decrypt("plain-history"));
        assertFalse(cipher.isEncrypted("plain-history"));
    }

    @Test
    void keepsPlaintextWhenKeyMissing() throws Exception {
        CredentialCipher cipher = cipher("");
        assertFalse(cipher.isReady());
        assertEquals("secret", cipher.encrypt("secret"));
        assertNull(cipher.decrypt("enc:aaaa"));
    }

    private CredentialCipher cipher(String key) throws Exception {
        CredentialCipher cipher = new CredentialCipher();
        Field field = CredentialCipher.class.getDeclaredField("keyText");
        field.setAccessible(true);
        field.set(cipher, key);
        cipher.init();
        return cipher;
    }
}
