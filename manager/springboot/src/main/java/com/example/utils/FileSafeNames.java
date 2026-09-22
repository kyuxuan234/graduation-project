package com.example.utils;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.exception.BusinessException;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 上传文件名、扩展名与 Magic Number 校验。
 */
public final class FileSafeNames {

    public static final int MAX_BYTES = 10 * 1024 * 1024;
    public static final Pattern STORED_NAME = Pattern.compile("^[a-f0-9-]{32,36}(\\.[a-zA-Z0-9]{1,5})?$");

    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "zip", "gz"
    ));

    private FileSafeNames() {
    }

    /**
     * 从原始文件名解析允许的存储后缀；.tar.gz 存为 gz（GET 标识后缀最长 5）。
     */
    public static String resolveAllowedExt(String originalName) {
        if (StrUtil.isBlank(originalName)
                || originalName.indexOf('\0') >= 0
                || originalName.toLowerCase(Locale.ROOT).contains("%00")
                || originalName.contains("..")
                || originalName.contains("/")
                || originalName.contains("\\")) {
            throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
        }
        String lower = originalName.toLowerCase(Locale.ROOT);
        String ext;
        if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
            ext = "gz";
        } else {
            int dot = lower.lastIndexOf('.');
            if (dot < 0 || dot == lower.length() - 1) {
                throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
            }
            ext = lower.substring(dot + 1);
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
        }
        return ext;
    }

    public static void assertMagic(String ext, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
        }
        if (!magicMatches(ext, bytes)) {
            throw new BusinessException(ResultCodeEnum.FILE_TYPE_ERROR);
        }
    }

    public static boolean magicMatches(String ext, byte[] bytes) {
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return startsWith(bytes, 0xFF, 0xD8, 0xFF);
        }
        if ("png".equals(ext)) {
            return startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
        }
        if ("gif".equals(ext)) {
            return startsWith(bytes, 0x47, 0x49, 0x46, 0x38);
        }
        if ("webp".equals(ext)) {
            return bytes.length >= 12
                    && startsWith(bytes, 0x52, 0x49, 0x46, 0x46)
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
        }
        if ("pdf".equals(ext)) {
            return startsWith(bytes, 0x25, 0x50, 0x44, 0x46);
        }
        if ("zip".equals(ext)) {
            return startsWith(bytes, 0x50, 0x4B, 0x03, 0x04) || startsWith(bytes, 0x50, 0x4B, 0x05, 0x06);
        }
        if ("gz".equals(ext)) {
            return startsWith(bytes, 0x1F, 0x8B);
        }
        return false;
    }

    public static String newStoredName(String ext) {
        return UUID.randomUUID().toString() + "." + ext;
    }

    public static boolean isValidStoredName(String flag) {
        return StrUtil.isNotBlank(flag) && STORED_NAME.matcher(flag).matches();
    }

    /**
     * 匿名 GET 仅放行白名单后缀的合法存储名。
     */
    public static boolean isPublicGetFlag(String flag) {
        if (!isValidStoredName(flag) || !flag.contains(".")) {
            return false;
        }
        String ext = flag.substring(flag.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXT.contains(ext);
    }

    public static void assertStoredName(String flag) {
        if (!isValidStoredName(flag)) {
            throw new BusinessException(ResultCodeEnum.FILE_NAME_INVALID_ERROR);
        }
    }

    public static Path resolveInside(Path root, String flag) {
        assertStoredName(flag);
        Path base = root.toAbsolutePath().normalize();
        Path target = base.resolve(flag).normalize();
        if (!target.startsWith(base) || !target.getParent().equals(base)) {
            throw new BusinessException(ResultCodeEnum.FILE_NAME_INVALID_ERROR);
        }
        return target;
    }

    private static boolean startsWith(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[i] & 0xFF) != (expected[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
