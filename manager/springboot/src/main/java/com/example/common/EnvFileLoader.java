package com.example.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 启动前读取本地 .env 到系统属性，供 ${MAIL_PASSWORD} 等占位符使用。
 * 已存在的操作系统环境变量优先，不覆盖。
 */
public final class EnvFileLoader {

    private EnvFileLoader() {
    }

    public static void load() {
        Path file = findEnvFile();
        if (file == null) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                applyLine(line);
            }
        } catch (IOException ignored) {
            // 缺文件或读失败时保持现有环境，不阻断启动
        }
    }

    static Path findEnvFile() {
        String userDir = System.getProperty("user.dir", ".");
        Path[] candidates = new Path[]{
                Paths.get(userDir, ".env"),
                Paths.get(userDir, "springboot", ".env"),
                Paths.get(userDir, "manager", "springboot", ".env"),
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    static void applyLine(String raw) {
        if (raw == null) {
            return;
        }
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        int eq = line.indexOf('=');
        if (eq <= 0) {
            return;
        }
        String key = line.substring(0, eq).trim();
        String value = stripQuotes(line.substring(eq + 1).trim());
        if (key.isEmpty()) {
            return;
        }
        if (System.getenv(key) != null) {
            return;
        }
        if (System.getProperty(key) != null) {
            return;
        }
        System.setProperty(key, value);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
