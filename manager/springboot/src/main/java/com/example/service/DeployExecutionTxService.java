package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.entity.DeployExecution;
import com.example.entity.DeployLog;
import com.example.mapper.DeployExecutionMapper;
import com.example.mapper.DeployLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 执行记录和日志各自单独提交，避免和 SSH 挤在同一个事务里。
 */
@Service
public class DeployExecutionTxService {

    @Resource
    private DeployExecutionMapper deployExecutionMapper;

    @Resource
    private DeployLogMapper deployLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public DeployExecution begin(Integer deployId) {
        DeployExecution running = deployExecutionMapper.selectRunning(deployId);
        if (running != null) {
            return running;
        }
        DeployExecution row = new DeployExecution();
        row.setDeployId(deployId);
        row.setStatus("RUNNING");
        row.setRetryCount(0);
        row.setStartTime(DateUtil.now());
        deployExecutionMapper.insert(row);
        return row;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void bumpRetry(Integer id, int retryCount) {
        DeployExecution row = new DeployExecution();
        row.setId(id);
        row.setRetryCount(retryCount);
        row.setErrorMessage("");
        deployExecutionMapper.updateById(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void complete(Integer id, String status, Integer exitCode, String errorMessage) {
        DeployExecution row = new DeployExecution();
        row.setId(id);
        row.setStatus(status);
        row.setExitCode(exitCode);
        row.setEndTime(DateUtil.now());
        row.setErrorMessage(errorMessage == null ? "" : errorMessage);
        deployExecutionMapper.updateById(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void appendLogs(List<DeployLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        deployLogMapper.insertBatch(logs);
    }

    public Integer maxExecSeq(Integer deployId) {
        Integer seq = deployLogMapper.maxExecSeq(deployId);
        return seq == null ? 0 : seq;
    }

    public java.util.List<DeployExecution> listRunning() {
        return deployExecutionMapper.selectByStatus("RUNNING");
    }
}
