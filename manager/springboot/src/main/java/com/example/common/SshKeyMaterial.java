package com.example.common;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 登记密码时生成的登录私钥。用户不填写私钥，之后用这把私钥登录。
 */
public class SshKeyMaterial {

    static final String COMMENT = "manager-password-key";

    private final String privatePem;
    private final String publicLine;

    private SshKeyMaterial(String privatePem, String publicLine) {
        this.privatePem = privatePem;
        this.publicLine = publicLine;
    }

    public String getPrivatePem() {
        return privatePem;
    }

    public String getPublicLine() {
        return publicLine;
    }

    public static SshKeyMaterial generate() {
        try {
            KeyPairGenerator generator = new KeyPairGenerator();
            generator.initialize(EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519), new SecureRandom());
            KeyPair pair = generator.generateKeyPair();
            byte[] seed = ((EdDSAPrivateKey) pair.getPrivate()).getSeed();
            byte[] pub = ((EdDSAPublicKey) pair.getPublic()).getAbyte();
            byte[] publicBlob = sshPublicBlob(pub);
            String publicLine = "ssh-ed25519 " + Base64.getEncoder().encodeToString(publicBlob) + " " + COMMENT;
            String privatePem = openSshPrivatePem(seed, pub, publicBlob);
            return new SshKeyMaterial(privatePem, publicLine);
        } catch (Exception e) {
            throw new IllegalStateException("生成登录私钥失败");
        }
    }

    private static byte[] sshPublicBlob(byte[] pub) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, "ssh-ed25519");
        writeString(out, pub);
        return out.toByteArray();
    }

    private static String openSshPrivatePem(byte[] seed, byte[] pub, byte[] publicBlob) {
        byte[] check = new byte[4];
        new SecureRandom().nextBytes(check);
        ByteArrayOutputStream secret = new ByteArrayOutputStream();
        secret.write(check, 0, 4);
        secret.write(check, 0, 4);
        writeString(secret, "ssh-ed25519");
        writeString(secret, pub);
        byte[] priv = new byte[64];
        System.arraycopy(seed, 0, priv, 0, 32);
        System.arraycopy(pub, 0, priv, 32, 32);
        writeString(secret, priv);
        writeString(secret, COMMENT);
        int pad = secret.size() % 8;
        if (pad != 0) {
            for (int i = 1; i <= 8 - pad; i++) {
                secret.write(i);
            }
        }
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        byte[] magic = "openssh-key-v1\0".getBytes(StandardCharsets.US_ASCII);
        all.write(magic, 0, magic.length);
        writeString(all, "none");
        writeString(all, "none");
        writeString(all, new byte[0]);
        writeUint32(all, 1);
        writeString(all, publicBlob);
        writeString(all, secret.toByteArray());
        String encoded = Base64.getMimeEncoder(70, new byte[]{'\n'}).encodeToString(all.toByteArray());
        return "-----BEGIN OPENSSH PRIVATE KEY-----\n" + encoded + "\n-----END OPENSSH PRIVATE KEY-----\n";
    }

    private static void writeUint32(ByteArrayOutputStream out, int value) {
        out.write((value >>> 24) & 0xff);
        out.write((value >>> 16) & 0xff);
        out.write((value >>> 8) & 0xff);
        out.write(value & 0xff);
    }

    private static void writeString(ByteArrayOutputStream out, String text) {
        writeString(out, text.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeString(ByteArrayOutputStream out, byte[] data) {
        writeUint32(out, data.length);
        out.write(data, 0, data.length);
    }
}
