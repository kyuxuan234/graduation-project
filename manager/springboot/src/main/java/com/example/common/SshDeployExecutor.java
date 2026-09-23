package com.example.common;

import cn.hutool.core.util.StrUtil;
import com.example.entity.Server;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 用 sshj 连目标机。超时后断开连接，不在事务里调用。
 */
@Component
public class SshDeployExecutor implements DeployExecutor {

    private static final Logger log = LoggerFactory.getLogger(SshDeployExecutor.class);

    @Resource
    private CredentialCipher credentialCipher;

    @Value("${deploy.execution.timeout-seconds:300}")
    private int timeoutSeconds;

    private final ScheduledExecutorService killer = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable, "ssh-timeout-");
        thread.setDaemon(true);
        return thread;
    });

    @PreDestroy
    public void shutdown() {
        killer.shutdownNow();
    }

    @Override
    public void testConnection(Server server) throws Exception {
        SSHClient client = open(server);
        client.disconnect();
    }

    @Override
    public void upload(Server server, String localPath, String remoteDir) throws Exception {
        SSHClient client = open(server);
        try {
            client.newSCPFileTransfer().upload(localPath, remoteDir);
        } finally {
            client.disconnect();
        }
    }

    @Override
    public String remoteMd5(Server server, String remoteFile) throws Exception {
        String output = runCommand(server, "md5sum " + quote(remoteFile));
        String trimmed = output.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("远端没有返回 MD5");
        }
        return trimmed.split("\\s+")[0];
    }

    @Override
    public String runCommand(Server server, String command) throws Exception {
        StringBuilder output = new StringBuilder();
        int code = runScript(server, command + "\n", line -> {
            if (output.length() > 0) {
                output.append('\n');
            }
            output.append(line);
        });
        if (code != 0) {
            throw new IllegalStateException("远程命令失败，退出码 " + code + "：" + output);
        }
        return output.toString();
    }

    @Override
    public int runScript(Server server, String script, Consumer<String> lines) throws Exception {
        SSHClient client = open(server);
        AtomicBoolean timedOut = new AtomicBoolean(false);
        ScheduledFuture<?> watch = killer.schedule(() -> {
            timedOut.set(true);
            try {
                client.disconnect();
            } catch (Exception ignored) {
                log.debug("断开超时连接失败");
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
        try {
            Session session = client.startSession();
            try {
                Session.Command command = session.exec("bash -s");
                OutputStream stdin = command.getOutputStream();
                stdin.write(script.getBytes(StandardCharsets.UTF_8));
                stdin.flush();
                stdin.close();
                Thread stderr = new Thread(() -> drain(command.getErrorStream(), lines), "ssh-stderr");
                stderr.setDaemon(true);
                stderr.start();
                drain(command.getInputStream(), lines);
                stderr.join(5000);
                command.join(5, TimeUnit.SECONDS);
                if (timedOut.get()) {
                    throw new IllegalStateException("执行超时");
                }
                Integer exit = command.getExitStatus();
                return exit == null ? 1 : exit;
            } finally {
                session.close();
            }
        } catch (Exception e) {
            if (timedOut.get()) {
                throw new IllegalStateException("执行超时");
            }
            throw e;
        } finally {
            watch.cancel(false);
            try {
                client.disconnect();
            } catch (Exception ignored) {
                log.debug("关闭 SSH 连接失败");
            }
        }
    }

    private void drain(InputStream input, Consumer<String> lines) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.accept(line);
            }
        } catch (Exception e) {
            log.debug("读取远程输出结束: {}", e.getMessage());
        }
    }

    /**
     * 给交互式终端用。命令执行仍走带读超时的连接，避免空闲脚本一直占着。
     */
    public SSHClient connectShell(Server server) throws Exception {
        SSHClient client = open(server);
        client.setTimeout(0);
        return client;
    }

    private SSHClient open(Server server) throws Exception {
        if (server == null || StrUtil.isBlank(server.getIp()) || StrUtil.isBlank(server.getSshUser())) {
            throw new IllegalStateException("SSH 配置不完整");
        }
        int port = server.getSshPort() == null || server.getSshPort() <= 0 ? 22 : server.getSshPort();
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.setConnectTimeout(10000);
        client.setTimeout(10000);
        client.connect(server.getIp(), port);
        if ("KEY".equalsIgnoreCase(server.getSshAuthType())) {
            String keyText = credentialCipher.decrypt(server.getSshPrivateKey());
            if (StrUtil.isBlank(keyText)) {
                client.disconnect();
                throw new IllegalStateException("私钥无法解密");
            }
            authPublicKey(client, server.getSshUser(), keyText);
        } else {
            String password = credentialCipher.decrypt(server.getSshPassword());
            if (password == null) {
                client.disconnect();
                throw new IllegalStateException("密码无法解密");
            }
            client.authPassword(server.getSshUser(), password);
        }
        return client;
    }

    /**
     * 用登记的密码登录一次，把对应公钥写入目标机，并确认私钥能登录。
     */
    public void provisionLoginKey(String ip, int port, String user, String password, String privatePem, String publicLine) throws Exception {
        SSHClient client = newClient();
        try {
            client.connect(ip, port);
            client.authPassword(user, password);
            int code = exec(client, installScript(publicLine));
            if (code != 0) {
                throw new IllegalStateException("写入登录公钥失败");
            }
        } finally {
            disconnect(client);
        }
        SSHClient check = newClient();
        try {
            check.connect(ip, port);
            authPublicKey(check, user, privatePem);
        } finally {
            disconnect(check);
        }
    }

    private SSHClient newClient() {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.setConnectTimeout(10000);
        client.setTimeout(10000);
        return client;
    }

    private void authPublicKey(SSHClient client, String user, String privatePem) throws Exception {
        Path keyFile = Files.createTempFile("ssh-key-", ".pem");
        try {
            Files.write(keyFile, privatePem.getBytes(StandardCharsets.UTF_8));
            KeyProvider keys = client.loadKeys(keyFile.toString());
            client.authPublickey(user, keys);
        } finally {
            Files.deleteIfExists(keyFile);
        }
    }

    private int exec(SSHClient client, String command) throws Exception {
        Session session = client.startSession();
        try {
            Session.Command cmd = session.exec(command);
            cmd.join(15, TimeUnit.SECONDS);
            Integer exit = cmd.getExitStatus();
            return exit == null ? 1 : exit;
        } finally {
            session.close();
        }
    }

    private String installScript(String publicLine) {
        String script = "set -e; umask 077; mkdir -p \"$HOME/.ssh\"; chmod 700 \"$HOME/.ssh\"; "
                + "touch \"$HOME/.ssh/authorized_keys\"; "
                + "grep -v '" + SshKeyMaterial.COMMENT + "' \"$HOME/.ssh/authorized_keys\" > \"$HOME/.ssh/authorized_keys.manager-tmp\" || true; "
                + "printf '%s\\n' '" + publicLine + "' >> \"$HOME/.ssh/authorized_keys.manager-tmp\"; "
                + "mv \"$HOME/.ssh/authorized_keys.manager-tmp\" \"$HOME/.ssh/authorized_keys\"; "
                + "chmod 600 \"$HOME/.ssh/authorized_keys\"";
        return "sh -c " + quote(script);
    }

    private void disconnect(SSHClient client) {
        try {
            client.disconnect();
        } catch (Exception ignored) {
            log.debug("关闭 SSH 连接失败");
        }
    }

    private String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
