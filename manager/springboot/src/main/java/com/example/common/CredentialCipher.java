package com.example.common;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SSH 凭据加密。密钥为空时只告警，按明文保存，不阻断启动。
 * 密文格式：enc: + Base64(12字节IV + GCM密文)。
 */
@Component
public class CredentialCipher {

    private static final Logger log = LoggerFactory.getLogger(CredentialCipher.class);
    private static final String PREFIX = "enc:";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    @Value("${credential.key:}")
    private String keyText;

    private byte[] key;
    private boolean ready;

    @PostConstruct
    public void init() {
        if (StrUtil.isBlank(keyText)) {
            log.warn("未配置 CREDENTIAL_KEY，SSH 凭据将按明文保存");
            ready = false;
            return;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(keyText.getBytes(StandardCharsets.UTF_8));
            byte[] aes = new byte[16];
            System.arraycopy(hashed, 0, aes, 0, 16);
            key = aes;
            ready = true;
        } catch (Exception e) {
            log.warn("CREDENTIAL_KEY 无法使用，SSH 凭据将按明文保存");
            ready = false;
        }
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isEncrypted(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public String encrypt(String plain) {
        if (StrUtil.isBlank(plain) || isEncrypted(plain)) {
            return plain;
        }
        if (!ready) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(packed);
        } catch (Exception e) {
            log.warn("凭据加密失败，按明文保存");
            return plain;
        }
    }

    /**
     * 解密失败返回 null，调用方按配置错误处理。历史明文原样返回。
     */
    public String decrypt(String stored) {
        if (StrUtil.isBlank(stored)) {
            return stored;
        }
        if (!isEncrypted(stored)) {
            return stored;
        }
        if (!ready) {
            return null;
        }
        try {
            byte[] packed = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (packed.length <= IV_LEN) {
                return null;
            }
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(packed, 0, iv, 0, IV_LEN);
            byte[] encrypted = new byte[packed.length - IV_LEN];
            System.arraycopy(packed, IV_LEN, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
