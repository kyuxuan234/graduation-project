package com.example.common;

import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.List;
import java.util.Locale;

/**
 * SSH 主机密钥校验器，替代无脑信任的 {@link PromiscuousVerifier}。
 *
 * <p>校验档位由配置项 {@code deploy.ssh.host-key-check} 决定，共三档：</p>
 * <ul>
 *   <li>{@code accept-new}（默认）：TOFU（首次信任）。主机不在 known_hosts 时，记录其指纹并放行；
 *       再次连接时校验指纹，密钥变更则拒绝连接（抛出的错误信息能看出是主机密钥变更）。</li>
 *   <li>{@code known-hosts}：只信任 known_hosts 中已有的条目，未知主机直接拒绝。生产建议档。</li>
 *   <li>{@code off}：不校验主机密钥，等价旧的 {@link PromiscuousVerifier}。仅限 dev 排障，存在中间人风险。</li>
 * </ul>
 *
 * <p>本类只负责“是否信任该主机密钥”，不做连接与认证；连接逻辑由 {@link SshDeployExecutor} 复用同一个实例。</p>
 */
public class SshHostKeyVerifier implements HostKeyVerifier {

    /** 首次信任：未知主机写入 known_hosts 并放行，密钥变更拒绝。 */
    public static final String MODE_ACCEPT_NEW = "accept-new";
    /** 只信任 known_hosts 已有条目，未知主机拒绝。生产建议档。 */
    public static final String MODE_KNOWN_HOSTS = "known-hosts";
    /** 不校验主机密钥，仅 dev 排障。 */
    public static final String MODE_OFF = "off";

    private static final Logger log = LoggerFactory.getLogger(SshHostKeyVerifier.class);

    /** 写入 known_hosts 条目时追加的注释，便于人工识别来源。 */
    private static final String COMMENT = "added-by-manager";

    private final String mode;
    /** off 档为 null；其余档为解析（并在 accept-new 下可写）的 known_hosts。 */
    private final OpenSSHKnownHosts knownHosts;
    /** 仅 off 档非空。 */
    private final HostKeyVerifier promiscuous;

    private SshHostKeyVerifier(String mode, OpenSSHKnownHosts knownHosts) {
        this.mode = mode;
        this.knownHosts = knownHosts;
        this.promiscuous = knownHosts == null ? new PromiscuousVerifier() : null;
    }

    /**
     * 按档位创建校验器。
     *
     * @param mode           档位取值，大小写不敏感；空或非法值回退为 {@code accept-new}
     * @param knownHostsFile known_hosts 文件路径，父目录不存在会自动创建
     * @return 对应档位的 {@link HostKeyVerifier}
     */
    public static SshHostKeyVerifier create(String mode, Path knownHostsFile) {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        if (MODE_OFF.equals(normalized)) {
            log.warn("SSH 主机密钥校验已关闭（deploy.ssh.host-key-check=off），存在中间人风险，仅限开发排障");
            return new SshHostKeyVerifier(MODE_OFF, null);
        }
        boolean acceptNew;
        if (MODE_KNOWN_HOSTS.equals(normalized)) {
            acceptNew = false;
        } else {
            if (!normalized.isEmpty() && !MODE_ACCEPT_NEW.equals(normalized)) {
                log.warn("未知的 deploy.ssh.host-key-check 取值 [{}]，按默认档 accept-new 处理", mode);
            }
            acceptNew = true;
        }
        if (knownHostsFile == null) {
            throw new IllegalStateException("deploy.ssh.known-hosts 未配置，无法校验主机密钥");
        }
        try {
            File file = prepareFile(knownHostsFile);
            OpenSSHKnownHosts known = acceptNew ? new AcceptNewKnownHosts(file) : new OpenSSHKnownHosts(file);
            return new SshHostKeyVerifier(acceptNew ? MODE_ACCEPT_NEW : MODE_KNOWN_HOSTS, known);
        } catch (IOException e) {
            throw new IllegalStateException("初始化 SSH known_hosts 失败：" + knownHostsFile, e);
        }
    }

    /**
     * accept-new 与 known-hosts 都信任 known_hosts 中已有的条目；区别在未知主机的处理。
     *
     * <p>委托给 sshj 的 {@link OpenSSHKnownHosts#verify}，其语义为：命中条目且公钥匹配放行；
     * 命中条目但公钥不匹配视为“密钥变更”，拒绝；未命中条目视为“未知主机”，
     * 在 accept-new 档由 {@link AcceptNewKnownHosts} 覆写为写入并放行。</p>
     */
    @Override
    public boolean verify(String hostname, int port, PublicKey key) {
        if (promiscuous != null) {
            return promiscuous.verify(hostname, port, key);
        }
        return knownHosts.verify(hostname, port, key);
    }

    @Override
    public List<String> findExistingAlgorithms(String hostname, int port) {
        if (promiscuous != null) {
            return promiscuous.findExistingAlgorithms(hostname, port);
        }
        return knownHosts.findExistingAlgorithms(hostname, port);
    }

    /** 返回生效档位，便于日志与自测断言。 */
    public String getMode() {
        return mode;
    }

    /**
     * 创建 known_hosts 文件并保证父目录存在。并发部署时多个进程/线程可能同时初始化，这里只做幂等创建。
     */
    private static File prepareFile(Path knownHostsFile) throws IOException {
        Path file = knownHostsFile.toAbsolutePath();
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
        return file.toFile();
    }

    /**
     * accept-new 档专用：把“未知主机”从拒绝改为“写入指纹并放行”。
     *
     * <p>并发安全：{@link #verify} 与写入动作均以 {@code this} 为锁，保证同一实例内
     * 条目列表的读取与追加互斥；{@link OpenSSHKnownHosts#write} 以追加方式一次写入一行并 flush，
     * 不会写出半截文件。生产中 {@link SshDeployExecutor} 复用同一个校验器实例，故该锁可覆盖全部部署线程。</p>
     */
    static final class AcceptNewKnownHosts extends OpenSSHKnownHosts {

        AcceptNewKnownHosts(File location) throws IOException {
            super(location);
        }

        @Override
        public synchronized boolean verify(String hostname, int port, PublicKey key) {
            return super.verify(hostname, port, key);
        }

        /**
         * 未知主机：记录指纹后放行（TOFU）。密钥变更仍由 {@code hostKeyChangedAction} 拒绝，不走本方法。
         */
        @Override
        protected boolean hostKeyUnverifiableAction(String hostname, PublicKey key) {
            try {
                KeyType type = KeyType.fromKey(key);
                HostEntry entry = new HostEntry(null, hostname, type, key, COMMENT);
                write(entry);
                // write() 只落盘，不更新内存条目；追加后同步内存，避免同一实例再次连接时重复写入。
                entries().add(entry);
                log.info("首次连接记录主机密钥：{}（{}）", hostname, SecurityUtils.getFingerprint(key));
                return true;
            } catch (IOException e) {
                // SSHException 是 IOException 的子类，统一在此兜住。
                throw new IllegalStateException("写入 known_hosts 失败，已拒绝连接：" + hostname, e);
            }
        }
    }
}
