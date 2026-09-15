package com.example.utils;

import cn.hutool.crypto.SecureUtil;
import com.example.common.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilsTest {

    @Test
    void encrypt_usesBcrypt() {
        String hash = PasswordUtils.encrypt("admin");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
        assertTrue(PasswordUtils.matches("admin", hash));
        assertFalse(PasswordUtils.matches("wrong", hash));
        assertFalse(PasswordUtils.isLegacyMd5(hash));
    }

    @Test
    void matches_legacyMd5ThenStillWorks() {
        String md5 = SecureUtil.md5("admin" + Constants.PASSWORD_SALT);
        assertTrue(PasswordUtils.isLegacyMd5(md5));
        assertTrue(PasswordUtils.matches("admin", md5));
        assertFalse(PasswordUtils.matches("wrong", md5));
    }

    @Test
    void matches_rejectsNull() {
        assertFalse(PasswordUtils.matches(null, "x"));
        assertFalse(PasswordUtils.matches("x", null));
    }
}
