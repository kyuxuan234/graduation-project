package com.example.utils;

import cn.hutool.crypto.SecureUtil;
import com.example.common.Constants;

/**
 * 密码加密工具类
 */
public class PasswordUtils {

    private PasswordUtils() {
    }

    /**
     * MD5 + 固定盐值加密，返回32位小写密文
     */
    public static String encrypt(String rawPassword) {
        return SecureUtil.md5(rawPassword + Constants.PASSWORD_SALT);
    }

    /**
     * 校验明文密码与数据库密文是否匹配
     */
    public static boolean matches(String rawPassword, String encryptedPassword) {
        if (rawPassword == null || encryptedPassword == null) {
            return false;
        }
        return encrypt(rawPassword).equals(encryptedPassword);
    }
}
