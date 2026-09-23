package com.example.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SSH 主机密钥校验三档行为。不连真实网络，用本地生成的公钥直接调 verify() 断言放行/拒绝。
 */
class SshHostKeyVerifierTest {

    private static PublicKey hostKeyA;
    private static PublicKey hostKeyB;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pairA = generator.generateKeyPair();
        KeyPair pairB = generator.generateKeyPair();
        hostKeyA = pairA.getPublic();
        hostKeyB = pairB.getPublic();
    }

    @Test
    void offModeTrustsAnyHostKey() {
        SshHostKeyVerifier verifier = SshHostKeyVerifier.create("off", null);

        assertEquals(SshHostKeyVerifier.MODE_OFF, verifier.getMode());
        assertTrue(verifier.verify("10.0.0.1", 22, hostKeyA));
        assertTrue(verifier.verify("10.0.0.2", 2222, hostKeyB));
    }

    @Test
    void knownHostsRejectsUnknownHost() throws Exception {
        Path file = tempDir.resolve("known_hosts");
        SshHostKeyVerifier verifier = SshHostKeyVerifier.create("known-hosts", file);

        assertEquals(SshHostKeyVerifier.MODE_KNOWN_HOSTS, verifier.getMode());
        assertFalse(verifier.verify("10.0.0.9", 22, hostKeyA));
        assertTrue(entries(file).isEmpty());
    }

    @Test
    void knownHostsTrustsRecordedEntryAndRejectsChangedKey() throws Exception {
        Path file = tempDir.resolve("known_hosts");
        // 先用 accept-new 首次信任，记下主机指纹。
        assertTrue(SshHostKeyVerifier.create("accept-new", file).verify("h1.test", 22, hostKeyA));

        SshHostKeyVerifier strict = SshHostKeyVerifier.create("known-hosts", file);
        assertTrue(strict.verify("h1.test", 22, hostKeyA));
        assertFalse(strict.verify("h1.test", 22, hostKeyB));
    }

    @Test
    void acceptNewWritesFingerprintThenRejectsKeyChange() throws Exception {
        Path file = tempDir.resolve("known_hosts");
        SshHostKeyVerifier verifier = SshHostKeyVerifier.create("accept-new", file);

        assertEquals(SshHostKeyVerifier.MODE_ACCEPT_NEW, verifier.getMode());
        // 首次连接：未知主机，写入指纹并放行。
        assertTrue(verifier.verify("app-dev-01", 22, hostKeyA));
        List<String> first = entries(file);
        assertEquals(1, first.size());
        assertTrue(first.get(0).startsWith("app-dev-01 ssh-rsa "), "实际写入：" + first.get(0));

        // 再次连接同一主机同一指纹：命中已有条目，放行且不重复写入。
        assertTrue(verifier.verify("app-dev-01", 22, hostKeyA));
        assertEquals(1, entries(file).size());

        // 同一主机换了密钥：视为密钥变更，拒绝。
        assertFalse(verifier.verify("app-dev-01", 22, hostKeyB));
        assertEquals(1, entries(file).size());
    }

    @Test
    void acceptNewRecordsNonStandardPortInBracketForm() throws Exception {
        Path file = tempDir.resolve("known_hosts");
        SshHostKeyVerifier verifier = SshHostKeyVerifier.create("accept-new", file);

        assertTrue(verifier.verify("db-prod-01", 2222, hostKeyA));
        assertTrue(entries(file).get(0).startsWith("[db-prod-01]:2222 ssh-rsa "),
                "非 22 端口应写成 [host]:port，实际：" + entries(file).get(0));
    }

    @Test
    void acceptNewCreatesMissingParentDirectory() throws Exception {
        Path file = tempDir.resolve("nested/config/known_hosts");
        assertFalse(Files.exists(file.getParent()));

        SshHostKeyVerifier verifier = SshHostKeyVerifier.create("accept-new", file);
        assertTrue(verifier.verify("h4.test", 22, hostKeyA));
        assertTrue(Files.exists(file));
    }

    @Test
    void blankOrUnknownModeFallsBackToAcceptNew() throws Exception {
        Path file = tempDir.resolve("known_hosts");
        assertEquals(SshHostKeyVerifier.MODE_ACCEPT_NEW, SshHostKeyVerifier.create(null, file).getMode());
        assertEquals(SshHostKeyVerifier.MODE_ACCEPT_NEW, SshHostKeyVerifier.create("  ", file).getMode());
        assertEquals(SshHostKeyVerifier.MODE_ACCEPT_NEW, SshHostKeyVerifier.create("ACCEPT-NEW", file).getMode());
        assertEquals(SshHostKeyVerifier.MODE_ACCEPT_NEW, SshHostKeyVerifier.create("typo-mode", file).getMode());
    }

    @TempDir
    Path tempDir;

    private static List<String> entries(Path file) throws Exception {
        if (!Files.exists(file)) {
            return java.util.Collections.emptyList();
        }
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.toList());
    }
}
