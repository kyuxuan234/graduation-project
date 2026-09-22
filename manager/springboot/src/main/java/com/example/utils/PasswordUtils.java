package com.example.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.example.common.Constants;

import java.util.regex.Pattern;

/**
 * 新密码用 BCrypt。库里已有的 32 位加盐 MD5 仍可登录。
 */
public class PasswordUtils {

    private static final Pattern LEGACY_MD5 = Pattern.compile("^[a-f0-9]{32}$");

    private PasswordUtils() {
    }

    /**
     * BCrypt 密文，每次盐值不同。
     */
    public static String encrypt(String rawPassword) {
        return BCrypt.hashpw(rawPassword);
    }

    /**
     * 32 位小写十六进制视为旧的 MD5(明文 + 盐值)。
     */
    public static boolean isLegacyMd5(String encryptedPassword) {
        return encryptedPassword != null && LEGACY_MD5.matcher(encryptedPassword).matches();
    }

    /**
     * 校验明文密码与数据库密文是否匹配。
     */
    public static boolean matches(String rawPassword, String encryptedPassword) {
        if (rawPassword == null || encryptedPassword == null) {
            return false;
        }
        if (isLegacyMd5(encryptedPassword)) {
            return SecureUtil.md5(rawPassword + Constants.PASSWORD_SALT).equals(encryptedPassword);
        }
        try {
            return BCrypt.checkpw(rawPassword, encryptedPassword);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
