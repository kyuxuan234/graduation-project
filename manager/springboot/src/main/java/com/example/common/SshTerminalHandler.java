package com.example.common;

import com.example.common.enums.DeployExecuteModeEnum;
import com.example.common.enums.DeployStatusEnum;
import com.example.entity.Deploy;
import com.example.entity.Server;
import com.example.mapper.DeployMapper;
import com.example.mapper.ServerMapper;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import net.schmizz.sshj.connection.channel.direct.PTYMode;

import javax.annotation.Resource;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 手动部署时把浏览器里的 SSH 窗口接到目标机的 shell。连接跟部署单走，不接受任意主机。
 */
@Component
public class SshTerminalHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SshTerminalHandler.class);

    @Resource
    private SshDeployExecutor sshDeployExecutor;

    @Resource
    private DeployMapper deployMapper;

    @Resource
    private ServerMapper serverMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        ShellLink link = new ShellLink();
        session.getAttributes().put("link", link);
        Thread thread = new Thread(() -> attach(session, link), "ssh-term-" + session.getId());
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ShellLink link = link(session);
        if (link == null || link.shell == null) {
            return;
        }
        OutputStream out = link.shell.getOutputStream();
        synchronized (link) {
            out.write(message.getPayload().getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ShellLink link = link(session);
        if (link != null) {
            link.close();
        }
    }

    private void attach(WebSocketSession ws, ShellLink link) {
        if (link.closed || !ws.isOpen()) {
            return;
        }
        try {
            Object rawId = ws.getAttributes().get("deployId");
            Integer deployId = rawId instanceof Integer ? (Integer) rawId : null;
            Deploy deploy = deployId == null ? null : deployMapper.selectById(deployId);
            if (deploy == null
                    || !DeployExecuteModeEnum.MANUAL.name().equals(deploy.getExecuteMode())
                    || !DeployStatusEnum.DEPLOYING.name().equals(deploy.getStatus())) {
                send(ws, "当前部署单不在手动部署中\r\n");
                closeWs(ws);
                return;
            }
            Server server = serverMapper.selectById(deploy.getServerId());
            link.client = sshDeployExecutor.connectShell(server);
            link.session = link.client.startSession();
            try {
                link.session.allocatePTY("dumb", 120, 40, 0, 0, Collections.<PTYMode, Integer>emptyMap());
            } catch (Exception e) {
                log.debug("分配终端失败，改用普通 shell: {}", e.getMessage());
            }
            link.shell = link.session.startShell();
            String who = server.getSshUser() + "@" + server.getIp();
            send(ws, "已连接 " + who + "，在黑色窗口中输入命令。\n");
            InputStream in = link.shell.getInputStream();
            byte[] buf = new byte[4096];
            int n;
            StringBuilder pending = new StringBuilder();
            while (!link.closed && (n = in.read(buf)) != -1 && ws.isOpen()) {
                pending.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                String ready = takeReady(pending);
                if (!ready.isEmpty()) {
                    send(ws, visible(ready));
                }
            }
        } catch (Exception e) {
            log.warn("SSH 窗口连接失败: {}", e.getMessage());
            String message = e.getMessage() == null ? "连接失败" : e.getMessage();
            send(ws, "连接失败：" + message + "\r\n");
        } finally {
            link.close();
            closeWs(ws);
        }
    }

    private String takeReady(StringBuilder pending) {
        String text = pending.toString();
        int hold = trailingEscape(text);
        if (hold < 0) {
            pending.setLength(0);
            return text;
        }
        pending.setLength(0);
        pending.append(text.substring(hold));
        return text.substring(0, hold);
    }

    private int trailingEscape(String text) {
        int esc = text.lastIndexOf('\u001B');
        if (esc < 0) {
            return -1;
        }
        String tail = text.substring(esc);
        if (tail.matches("\u001B\\[[0-9;?]*[ -/]*[@-~]")) {
            return -1;
        }
        if (tail.matches("\u001B\\][^\\u0007\u001B]*(?:\u0007|\u001B\\\\)")) {
            return -1;
        }
        if (tail.matches("\u001B[@-_]")) {
            return -1;
        }
        return esc;
    }

    private String visible(String text) {
        String cleaned = text
                .replaceAll("\u001B\\][^\\u0007\u001B]*(?:\u0007|\u001B\\\\)", "")
                .replaceAll("\u001B\\[[0-9;?]*[ -/]*[@-~]", "")
                .replaceAll("\u001B[@-_]", "")
                .replace("\r\n", "\n")
                .replace("\r", "");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '\n' || c == '\t' || c >= ' ') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private void send(WebSocketSession ws, String text) {
        if (text == null || !ws.isOpen()) {
            return;
        }
        try {
            synchronized (ws) {
                if (ws.isOpen()) {
                    ws.sendMessage(new TextMessage(text));
                }
            }
        } catch (Exception e) {
            log.debug("SSH 窗口写出失败: {}", e.getMessage());
        }
    }

    private void closeWs(WebSocketSession ws) {
        try {
            if (ws.isOpen()) {
                ws.close();
            }
        } catch (Exception e) {
            log.debug("关闭 SSH 窗口失败");
        }
    }

    private ShellLink link(WebSocketSession session) {
        Object value = session.getAttributes().get("link");
        return value instanceof ShellLink ? (ShellLink) value : null;
    }

    private static final class ShellLink {
        private SSHClient client;
        private Session session;
        private Session.Shell shell;
        private volatile boolean closed;

        private void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                if (shell != null) {
                    shell.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (session != null) {
                    session.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (client != null) {
                    client.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
