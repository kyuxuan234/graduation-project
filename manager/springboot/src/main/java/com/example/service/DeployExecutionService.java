package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.DeployExecutor;
import com.example.common.DeployScripts;
import com.example.common.enums.DeployExecuteModeEnum;
import com.example.common.enums.DeployStatusEnum;
import com.example.common.enums.DeployTypeEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.App;
import com.example.entity.Deploy;
import com.example.entity.DeployExecution;
import com.example.entity.DeployLog;
import com.example.entity.Server;
import com.example.exception.BusinessException;
import com.example.mapper.AppMapper;
import com.example.mapper.DeployMapper;
import com.example.mapper.ServerMapper;
import com.example.utils.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 请求线程里只改状态。SSH 拉仓库和写执行日志都在事务提交之后的线程池里做。
 */
@Service
public class DeployExecutionService {

    private static final Logger log = LoggerFactory.getLogger(DeployExecutionService.class);

    @Resource
    private DeployService deployService;

    @Resource
    private DeployExecutionTxService txService;

    @Resource
    private DeployMapper deployMapper;

    @Resource
    private AppMapper appMapper;

    @Resource
    private ServerMapper serverMapper;

    @Resource
    private DeployExecutor sshDeployExecutor;

    @Resource(name = "deployExecutor")
    private ThreadPoolTaskExecutor worker;

    @Resource
    private ScheduledExecutorService deployRetryScheduler;

    @Value("${deploy.execution.max-retry:2}")
    private int maxRetry;

    public void startDeployEntry(Integer id, String executeMode) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_EXECUTE_NO_PERMISSION, RoleEnum.OPS);
        String mode = normalizeMode(executeMode);
        Deploy dbDeploy = deployService.selectById(id);
        if (dbDeploy == null) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_NOT_EXIST_ERROR);
        }
        if (deployMapper.countOtherDeploying(dbDeploy.getAppId(), id, dbDeploy.getEnvCode()) > 0) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_BUSY);
        }
        Deploy patch = new Deploy();
        patch.setId(id);
        patch.setExecuteMode(mode);
        deployMapper.updateById(patch);
        dbDeploy.setExecuteMode(mode);
        deployService.startDeploy(id);
        if (manual(dbDeploy)) {
            return;
        }
        submit(id, currentUser.getUsername(), false);
    }

    public void rollbackEntry(Integer id) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_EXECUTE_NO_PERMISSION, RoleEnum.OPS);
        Deploy dbDeploy = deployService.selectById(id);
        if (dbDeploy == null) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_NOT_EXIST_ERROR);
        }
        if (deployMapper.countOtherDeploying(dbDeploy.getAppId(), id, dbDeploy.getEnvCode()) > 0) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_BUSY);
        }
        deployService.rollback(id);
        if (manual(dbDeploy)) {
            return;
        }
        submit(id, currentUser.getUsername(), true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterrupted() {
        try {
            List<DeployExecution> running = txService.listRunning();
            for (DeployExecution row : running) {
                txService.complete(row.getId(), "FAILED", null, cut("服务重启，执行中断", 500));
                try {
                    deployService.finishDeployAs(row.getDeployId(), DeployStatusEnum.FAILED.name(), null);
                } catch (Exception e) {
                    log.warn("回收中断部署单失败 deployId={}", row.getDeployId());
                }
            }
        } catch (Exception e) {
            log.warn("启动时回收执行记录失败: {}", e.getMessage());
        }
    }

    public void run(Integer deployId, String operator, boolean rollback) {
        DeployExecution execution = null;
        LogBuffer buffer = null;
        try {
            Deploy deploy = deployMapper.selectById(deployId);
            App app = deploy == null ? null : appMapper.selectById(deploy.getAppId());
            Server server = deploy == null ? null : serverMapper.selectById(deploy.getServerId());
            execution = txService.begin(deployId);
            buffer = new LogBuffer(deployId, txService.maxExecSeq(deployId));
            if (deploy == null || app == null || server == null) {
                fail(execution, buffer, operator, null, "部署单、应用或服务器不存在", false);
                return;
            }
            if (StrUtil.isBlank(server.getSshUser()) || StrUtil.isBlank(server.getDeployPath())) {
                fail(execution, buffer, operator, null, "服务器未配置 SSH 用户或部署目录", false);
                return;
            }
            String version = rollback && StrUtil.isNotBlank(deploy.getRollbackVersion())
                    ? deploy.getRollbackVersion() : deploy.getVersion();
            String appKey = StrUtil.isNotBlank(app.getCode()) ? app.getCode() : app.getName();
            String script;
            if (rollback) {
                script = DeployScripts.rollback(version, server.getDeployPath());
            } else {
                if (StrUtil.isBlank(app.getRepoUrl())) {
                    fail(execution, buffer, operator, null, "应用未配置代码仓库地址，无法生成拉取脚本", false);
                    return;
                }
                script = DeployScripts.deploy(appKey, version, server.getDeployPath(), app.getRepoUrl().trim());
                buffer.add("已生成仓库拉取脚本，按版本 " + version + " 获取代码");
            }
            int exit = sshDeployExecutor.runScript(server, script, buffer::add);
            buffer.flush();
            if (exit == 0) {
                txService.complete(execution.getId(), "SUCCESS", 0, "");
                deployService.finishDeployAs(deployId, DeployStatusEnum.SUCCESS.name(), operator);
            } else {
                fail(execution, buffer, operator, exit, "远程脚本退出码 " + exit, true);
            }
        } catch (IllegalArgumentException e) {
            fail(execution, buffer, operator, null, e.getMessage(), false);
        } catch (Exception e) {
            log.warn("部署执行失败 deployId={}", deployId, e);
            String message = e.getMessage() == null ? "执行失败" : e.getMessage();
            boolean retry = true;
            String status = "FAILED";
            if (message.contains("超时")) {
                status = "TIMEOUT";
            }
            fail(execution, buffer, operator, null, message, retry, status);
        }
    }

    private boolean manual(Deploy deploy) {
        return deploy != null && DeployExecuteModeEnum.MANUAL.name().equals(deploy.getExecuteMode());
    }

    private String normalizeMode(String mode) {
        if (DeployExecuteModeEnum.AUTO.name().equals(mode) || DeployExecuteModeEnum.MANUAL.name().equals(mode)) {
            return mode;
        }
        throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
    }

    private void submit(Integer id, String operator, boolean rollback) {
        try {
            worker.execute(() -> run(id, operator, rollback));
        } catch (RejectedExecutionException e) {
            deployService.finishDeployAs(id, DeployStatusEnum.FAILED.name(), operator);
            throw new BusinessException("6035", "部署执行队列已满，请稍后重试");
        }
    }

    private void fail(DeployExecution execution, LogBuffer buffer, String operator, Integer exitCode, String message, boolean retry) {
        String status = message != null && message.contains("超时") ? "TIMEOUT" : "FAILED";
        fail(execution, buffer, operator, exitCode, message, retry, status);
    }

    private void fail(DeployExecution execution, LogBuffer buffer, String operator, Integer exitCode,
                      String message, boolean retry, String status) {
        if (buffer != null) {
            buffer.add(message);
            buffer.flush();
        }
        if (execution == null) {
            return;
        }
        int used = execution.getRetryCount() == null ? 0 : execution.getRetryCount();
        if (retry && used < maxRetry) {
            txService.bumpRetry(execution.getId(), used + 1);
            boolean rollback = false;
            Deploy deploy = deployMapper.selectById(execution.getDeployId());
            if (deploy != null && DeployTypeEnum.ROLLBACK.name().equals(deploy.getDeployType())) {
                rollback = true;
            }
            boolean doRollback = rollback;
            deployRetryScheduler.schedule(() -> run(execution.getDeployId(), operator, doRollback), 5, TimeUnit.SECONDS);
            return;
        }
        txService.complete(execution.getId(), status, exitCode, cut(message, 500));
        try {
            deployService.finishDeployAs(execution.getDeployId(), DeployStatusEnum.FAILED.name(), operator);
        } catch (Exception e) {
            log.warn("回写部署失败状态未成功 deployId={}", execution.getDeployId());
        }
    }

    private String cut(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    private class LogBuffer {
        private final Integer deployId;
        private int seq;
        private long lastFlush = System.currentTimeMillis();
        private final List<DeployLog> batch = new ArrayList<DeployLog>();

        private LogBuffer(Integer deployId, int seq) {
            this.deployId = deployId;
            this.seq = seq;
        }

        private void add(String line) {
            if (line == null) {
                return;
            }
            String text = line.replace("\r", "");
            if (text.length() > 1000) {
                text = text.substring(0, 1000);
            }
            seq++;
            DeployLog row = new DeployLog();
            row.setDeployId(deployId);
            row.setAction("EXEC");
            row.setOperator(null);
            row.setDetail(text);
            row.setLogType("EXEC");
            row.setSeq(seq);
            row.setSource("AUTO");
            batch.add(row);
            if (batch.size() >= 50 || System.currentTimeMillis() - lastFlush >= 1000) {
                flush();
            }
        }

        private void flush() {
            if (batch.isEmpty()) {
                return;
            }
            txService.appendLogs(new ArrayList<DeployLog>(batch));
            batch.clear();
            lastFlush = System.currentTimeMillis();
        }
    }
}
