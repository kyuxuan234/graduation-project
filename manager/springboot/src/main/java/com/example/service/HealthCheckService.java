package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.CredentialCipher;
import com.example.common.DeployExecutor;
import com.example.common.enums.AlarmLevelEnum;
import com.example.common.enums.AppStatusEnum;
import com.example.entity.AppInstance;
import com.example.entity.Deploy;
import com.example.entity.Server;
import com.example.mapper.AppInstanceMapper;
import com.example.mapper.DeployMapper;
import com.example.mapper.ServerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 定时探活。单台失败不影响其他主机，告警失败也不影响下一台。
 */
@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    @Value("${healthcheck.enabled:true}")
    private boolean enabled;

    @Value("${healthcheck.auto-alarm-cooldown-minutes:30}")
    private int cooldownMinutes;

    @Resource
    private ServerMapper serverMapper;

    @Resource
    private AppInstanceMapper appInstanceMapper;

    @Resource
    private DeployMapper deployMapper;

    @Resource
    private DeployExecutor sshDeployExecutor;

    @Resource
    private CredentialCipher credentialCipher;

    @Resource
    private AlarmService alarmService;

    @Lazy
    @Resource
    private DeployService deployService;

    @Scheduled(cron = "${healthcheck.server-cron:0 */2 * * * ?}")
    public void checkServers() {
        if (!enabled) {
            return;
        }
        Server query = new Server();
        query.setStatus("RUNNING");
        List<Server> servers = serverMapper.selectAll(query);
        for (Server server : servers) {
            try {
                probeServer(server);
            } catch (Exception e) {
                log.warn("服务器探活异常 id={}: {}", server.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${healthcheck.app-cron:30 */2 * * * ?}")
    public void checkApps() {
        if (!enabled) {
            return;
        }
        List<AppInstance> instances = appInstanceMapper.selectOnline();
        for (AppInstance instance : instances) {
            try {
                probeInstance(instance);
            } catch (Exception e) {
                log.warn("应用探活异常 id={}: {}", instance.getId(), e.getMessage());
            }
        }
    }

    private void probeServer(Server server) {
        String marker = "[server:" + server.getId() + "]";
        String problem = credentialProblem(server);
        if (problem != null) {
            alarmService.raiseAuto(marker, null, server.getId(), null, AlarmLevelEnum.WARNING.name(),
                    marker + " " + server.getHostname() + " " + problem, cooldownMinutes);
            return;
        }
        if (StrUtil.isBlank(server.getSshUser()) || StrUtil.isBlank(server.getIp())) {
            alarmService.raiseAuto(marker, null, server.getId(), null, AlarmLevelEnum.WARNING.name(),
                    marker + " " + server.getHostname() + " SSH 配置不完整", cooldownMinutes);
            return;
        }
        try {
            sshDeployExecutor.testConnection(server);
            alarmService.markRecovered(marker, null, server.getId());
        } catch (Exception e) {
            String text = marker + " 主机不可达：" + server.getIp() + " " + shortMsg(e);
            alarmService.raiseAuto(marker, null, server.getId(), null, AlarmLevelEnum.CRITICAL.name(), text, cooldownMinutes);
            noteDeploys(server, text);
        }
    }

    private void probeInstance(AppInstance instance) {
        String name = StrUtil.blankToDefault(instance.getAppName(), "应用");
        String marker = "[app:" + instance.getAppId() + ":" + instance.getEnvCode() + "]";
        Server server = serverMapper.selectById(instance.getServerId());
        if (server == null) {
            alarmService.raiseAuto(marker, instance.getAppId(), null, null, AlarmLevelEnum.WARNING.name(),
                    marker + " 应用 " + name + " 未关联服务器", cooldownMinutes);
            return;
        }
        String problem = credentialProblem(server);
        if (problem != null || StrUtil.isBlank(server.getSshUser())) {
            alarmService.raiseAuto(marker, instance.getAppId(), server.getId(), null, AlarmLevelEnum.WARNING.name(),
                    marker + " 应用 " + name + " 所在主机 SSH 配置不完整", cooldownMinutes);
            return;
        }
        try {
            if (instance.getPort() != null && instance.getPort() > 0) {
                sshDeployExecutor.runCommand(server, "(echo >/dev/tcp/127.0.0.1/" + instance.getPort() + ") >/dev/null");
            } else if (StrUtil.isNotBlank(server.getDeployPath())) {
                String current = server.getDeployPath() + "/current";
                sshDeployExecutor.runCommand(server, "test -e '" + current.replace("'", "") + "'");
            } else {
                alarmService.raiseAuto(marker, instance.getAppId(), server.getId(), null, AlarmLevelEnum.WARNING.name(),
                        marker + " 应用 " + name + " 没有端口，服务器也没有部署目录", cooldownMinutes);
                return;
            }
            alarmService.markRecovered(marker, instance.getAppId(), server.getId());
        } catch (Exception e) {
            alarmService.raiseAuto(marker, instance.getAppId(), server.getId(), null, AlarmLevelEnum.CRITICAL.name(),
                    marker + " 应用 " + name + " 探活失败：" + shortMsg(e), cooldownMinutes);
        }
    }

    private void noteDeploys(Server server, String text) {
        String since = DateUtil.formatDateTime(DateUtil.offsetHour(new Date(), -24));
        List<Deploy> deploys = deployMapper.selectRecentForServer(server.getId(), since);
        for (Deploy deploy : deploys) {
            try {
                deployService.saveHealthCheckLog(deploy.getId(), "探活失败：" + text);
            } catch (Exception e) {
                log.warn("写入探活日志失败 deployId={}", deploy.getId());
            }
        }
    }

    private String credentialProblem(Server server) {
        if (!credentialCipher.isReady()) {
            return null;
        }
        String secret = "KEY".equalsIgnoreCase(server.getSshAuthType()) ? server.getSshPrivateKey() : server.getSshPassword();
        if (StrUtil.isBlank(secret)) {
            return "SSH 配置不完整";
        }
        if (!credentialCipher.isEncrypted(secret)) {
            return "SSH 凭据未加密";
        }
        if (credentialCipher.decrypt(secret) == null) {
            return "SSH 凭据无法解密";
        }
        return null;
    }

    private String shortMsg(Exception e) {
        String message = e.getMessage();
        if (ObjectUtil.isEmpty(message)) {
            return e.getClass().getSimpleName();
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }
}
