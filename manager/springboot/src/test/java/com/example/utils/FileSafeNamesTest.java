package com.example.utils;

import com.example.common.enums.ResultCodeEnum;
import com.example.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSafeNamesTest {

    @Test
    void resolveAllowedExt_acceptsWhitelistAndTarGz() {
        assertEquals("jpg", FileSafeNames.resolveAllowedExt("a.jpg"));
        assertEquals("png", FileSafeNames.resolveAllowedExt("photo.PNG"));
        assertEquals("gz", FileSafeNames.resolveAllowedExt("pkg.tar.gz"));
        assertEquals("gz", FileSafeNames.resolveAllowedExt("pkg.tgz"));
        assertEquals("pdf", FileSafeNames.resolveAllowedExt("doc.pdf"));
    }

    @Test
    void resolveAllowedExt_rejectsExeAndTraversal() {
        assertEquals(ResultCodeEnum.FILE_TYPE_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.resolveAllowedExt("evil.exe")).getCode());
        assertEquals(ResultCodeEnum.FILE_TYPE_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.resolveAllowedExt("../a.png")).getCode());
        assertEquals(ResultCodeEnum.FILE_TYPE_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.resolveAllowedExt("a/b.png")).getCode());
    }

    @Test
    void magicMatches_pngAndJpeg() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 1};
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        byte[] exe = new byte[]{0x4D, 0x5A, 0x00};
        assertTrue(FileSafeNames.magicMatches("png", png));
        assertTrue(FileSafeNames.magicMatches("jpg", jpeg));
        assertFalse(FileSafeNames.magicMatches("png", jpeg));
        assertFalse(FileSafeNames.magicMatches("png", exe));
        FileSafeNames.assertMagic("png", png);
        assertEquals(ResultCodeEnum.FILE_TYPE_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.assertMagic("png", jpeg)).getCode());
    }

    @Test
    void storedNameRegex_rejectsPathTraversal() {
        String uuid = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee.png";
        assertTrue(FileSafeNames.isValidStoredName(uuid));
        assertTrue(FileSafeNames.isPublicGetFlag(uuid));
        assertFalse(FileSafeNames.isValidStoredName("../x.png"));
        assertFalse(FileSafeNames.isValidStoredName("..%2fx.png"));
        assertFalse(FileSafeNames.isPublicGetFlag("../x.png"));
        assertEquals(ResultCodeEnum.FILE_NAME_INVALID_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.assertStoredName("../secret")).getCode());
    }

    @Test
    void resolveAllowedExt_rejectsTraversalNullByteAndAcceptsPlainName() {
        assertEquals(ResultCodeEnum.FILE_TYPE_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.resolveAllowedExt("../a.png")).getCode());
        assertEquals(ResultCodeEnum.FILE_TYPE_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.resolveAllowedExt("..\\a.png")).getCode());
        assertEquals(ResultCodeEnum.FILE_TYPE_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.resolveAllowedExt("C:\\Windows\\x.txt")).getCode());
        assertEquals(ResultCodeEnum.FILE_TYPE_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.resolveAllowedExt("/etc/passwd")).getCode());
        assertEquals(ResultCodeEnum.FILE_TYPE_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.resolveAllowedExt("a%00.png")).getCode());
        assertEquals(ResultCodeEnum.FILE_TYPE_ERROR.code,
                assertThrows(BusinessException.class, () -> FileSafeNames.resolveAllowedExt("a\u0000.png")).getCode());
        assertEquals("png", FileSafeNames.resolveAllowedExt("myapp-v1.2.0.png"));
    }

    @Test
    void resolveInside_staysInRoot() {
        Path root = Paths.get("files").toAbsolutePath();
        Path target = FileSafeNames.resolveInside(root, "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee.png");
        assertEquals(root.normalize(), target.getParent());
        assertThrows(BusinessException.class, () -> FileSafeNames.resolveInside(root, "../x.png"));
    }
}
