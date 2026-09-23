package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.DeployStatusMachine;
import com.example.common.enums.AppStatusEnum;
import com.example.common.enums.DeployActionEnum;
import com.example.common.enums.DeployExecuteModeEnum;
import com.example.common.enums.DeployStatusEnum;
import com.example.common.enums.DeployTypeEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.dto.DeployExecSummaryVO;
import com.example.dto.DeployLogLinesVO;
import com.example.entity.Account;
import com.example.entity.App;
import com.example.entity.Deploy;
import com.example.entity.DeployExecution;
import com.example.entity.DeployLog;
import com.example.exception.BusinessException;
import com.example.mapper.AppMapper;
import com.example.mapper.DeployExecutionMapper;
import com.example.mapper.DeployLogMapper;
import com.example.mapper.DeployMapper;
import com.example.utils.AuthUtils;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 部署单业务处理
 **/
@Service
public class DeployService {

    @Resource
    private DeployMapper deployMapper;

    @Resource
    private DeployLogMapper deployLogMapper;

    @Resource
    private AppMapper appMapper;

    @Resource
    private DeployExecutionMapper deployExecutionMapper;

    /**
     * 发起部署申请，只有DEV角色可以操作
     */
    @Transactional(rollbackFor = Exception.class)
    public void add(Deploy deploy) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_APPLY_NO_PERMISSION, RoleEnum.DEV);
        App dbApp = appMapper.selectById(deploy.getAppId());
        if (ObjectUtil.isNull(dbApp)) {
            throw new BusinessException(ResultCodeEnum.APP_NOT_EXIST_ERROR);
        }
        deploy.setDeployNo(generateDeployNo());
        deploy.setStatus(DeployStatusEnum.PENDING.name());
        deploy.setApplicant(currentUser.getUsername());
        deploy.setApplyTime(DateUtil.now());
        if (ObjectUtil.isEmpty(deploy.getDeployType())) {
            deploy.setDeployType(DeployTypeEnum.FULL.name());
        }
        deploy.setExecuteMode(normalizeExecuteMode(deploy.getExecuteMode()));
        if (ObjectUtil.isNull(deploy.getServerId())) {
            deploy.setServerId(dbApp.getServerId());
        }
        deployMapper.insert(deploy);
        saveLog(deploy.getId(), DeployActionEnum.APPLY, currentUser.getUsername(),
                "提交部署申请，目标版本：" + deploy.getVersion());
    }

    /**
     * 审批部署单，只有OPS角色可以操作
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Integer id, String result, String remark) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_APPROVE_NO_PERMISSION, RoleEnum.OPS);
        Deploy dbDeploy = getExistDeploy(id);
        DeployStatusMachine.assertApprove(dbDeploy.getStatus(), result);
        boolean approved = DeployStatusEnum.APPROVED.name().equals(result);

        // 状态与审批信息必须在同一条 CAS 语句里一次写完，被他人抢先变更则整体失败
        String approver = currentUser.getUsername();
        String approveTime = DateUtil.now();
        int version = dbDeploy.getRowVersion() == null ? 0 : dbDeploy.getRowVersion();
        int rows = deployMapper.casApprove(id, dbDeploy.getStatus(), version,
                result, approver, approveTime, remark);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_CONFLICT);
        }

        DeployActionEnum action = approved ? DeployActionEnum.APPROVE : DeployActionEnum.REJECT;
        String detail = approved ? "审批通过" : "审批驳回";
        if (ObjectUtil.isNotEmpty(remark)) {
            detail = detail + "，意见：" + remark;
        }
        saveLog(id, action, approver, detail);
    }

    /**
     * 开始部署
     */
    @Transactional(rollbackFor = Exception.class)
    public void startDeploy(Integer id) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_EXECUTE_NO_PERMISSION, RoleEnum.OPS);
        Deploy dbDeploy = getExistDeploy(id);
        DeployStatusMachine.assertStart(dbDeploy.getStatus());
        rememberRollbackTarget(dbDeploy);
        changeStatus(dbDeploy, DeployStatusEnum.DEPLOYING.name(), null, null);

        // 开始部署只把应用置为部署中，版本与在线状态等 finishDeploy 成功后再反写
        App updateApp = new App();
        updateApp.setId(dbDeploy.getAppId());
        updateApp.setStatus(AppStatusEnum.DEPLOYING.name());
        appMapper.updateById(updateApp);

        String startDetail = "开始部署，目标版本：" + dbDeploy.getVersion();
        if (DeployExecuteModeEnum.MANUAL.name().equals(dbDeploy.getExecuteMode())) {
            startDetail = startDetail + "，请在目标机操作后确认结果";
        }
        saveLog(id, DeployActionEnum.START_DEPLOY, currentUser.getUsername(), startDetail);
    }

    /**
     * 部署完成，result 为 SUCCESS 或 FAILED：成功则反写目标版本并置在线，失败则置离线
     */
    @Transactional(rollbackFor = Exception.class)
    public void finishDeploy(Integer id, String result) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_EXECUTE_NO_PERMISSION, RoleEnum.OPS);
        finishDeployAs(id, result, currentUser.getUsername());
    }

    /**
     * 给异步线程收口。调用方已经在请求线程里记下操作人，这里不再读登录态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void finishDeployAs(Integer id, String result, String operator) {
        Deploy dbDeploy = getExistDeploy(id);
        DeployStatusMachine.assertFinish(dbDeploy.getStatus(), result);
        boolean success = DeployStatusEnum.SUCCESS.name().equals(result);
        changeStatus(dbDeploy, result, DateUtil.now(), null);

        String versionToWrite = dbDeploy.getVersion();
        if (success && DeployTypeEnum.ROLLBACK.name().equals(dbDeploy.getDeployType())
                && ObjectUtil.isNotEmpty(dbDeploy.getRollbackVersion())) {
            versionToWrite = dbDeploy.getRollbackVersion();
        }

        App updateApp = new App();
        updateApp.setId(dbDeploy.getAppId());
        if (success) {
            updateApp.setVersion(versionToWrite);
            updateApp.setStatus(AppStatusEnum.ONLINE.name());
        } else {
            updateApp.setStatus(AppStatusEnum.OFFLINE.name());
        }
        appMapper.updateById(updateApp);

        DeployActionEnum action = success ? DeployActionEnum.FINISH : DeployActionEnum.FAIL;
        String detail = success ? "部署成功，版本：" + versionToWrite : "部署失败，版本：" + dbDeploy.getVersion();
        saveLog(id, action, operator, detail);
    }

    /**
     * 回滚，只有部署成功的单子可以回滚，回滚后重新进入部署中状态，再走 finishDeploy
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollback(Integer id) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_EXECUTE_NO_PERMISSION, RoleEnum.OPS);
        Deploy dbDeploy = getExistDeploy(id);
        DeployStatusMachine.assertRollback(dbDeploy.getStatus());
        if (ObjectUtil.isEmpty(dbDeploy.getRollbackVersion())) {
            String previous = deployMapper.selectLatestSuccessVersion(dbDeploy.getAppId(), id);
            if (ObjectUtil.isEmpty(previous)) {
                throw new BusinessException(ResultCodeEnum.DEPLOY_STATUS_ERROR.code, "没有可回滚的历史成功版本");
            }
            Deploy patch = new Deploy();
            patch.setId(id);
            patch.setRollbackVersion(previous);
            deployMapper.updateById(patch);
            dbDeploy.setRollbackVersion(previous);
        }
        changeStatus(dbDeploy, DeployStatusEnum.DEPLOYING.name(), "", DeployTypeEnum.ROLLBACK.name());

        App updateApp = new App();
        updateApp.setId(dbDeploy.getAppId());
        updateApp.setStatus(AppStatusEnum.DEPLOYING.name());
        appMapper.updateById(updateApp);

        String rollbackDetail = "发起回滚，目标版本：" + dbDeploy.getRollbackVersion();
        if (DeployExecuteModeEnum.MANUAL.name().equals(dbDeploy.getExecuteMode())) {
            rollbackDetail = rollbackDetail + "，请在目标机操作后确认结果";
        }
        saveLog(id, DeployActionEnum.ROLLBACK, currentUser.getUsername(), rollbackDetail);
    }

    /**
     * 探活失败记一条自动流转，不改部署单状态。
     */
    public void saveHealthCheckLog(Integer deployId, String detail) {
        DeployLog deployLog = new DeployLog();
        deployLog.setDeployId(deployId);
        deployLog.setAction(DeployActionEnum.HEALTH_CHECK_FAIL.name());
        deployLog.setOperator("system");
        deployLog.setDetail(detail);
        deployLog.setLogType("STATE");
        deployLog.setSeq(0);
        deployLog.setSource("AUTO");
        deployLogMapper.insert(deployLog);
    }

    public DeployLogLinesVO execLogs(Integer id, Integer cursor) {
        getExistDeploy(id);
        int from = cursor == null ? 0 : cursor;
        int pageSize = 200;
        List<DeployLog> rows = deployLogMapper.selectExecAfter(id, from, pageSize + 1);
        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            rows = rows.subList(0, pageSize);
        }
        DeployLogLinesVO vo = new DeployLogLinesVO();
        List<String> lines = new ArrayList<String>();
        int next = from;
        for (DeployLog row : rows) {
            lines.add(row.getDetail());
            if (row.getSeq() != null) {
                next = row.getSeq();
            }
        }
        vo.setLines(lines);
        vo.setHasMore(hasMore);
        vo.setNextCursor(next);
        return vo;
    }

    public DeployExecSummaryVO execSummary(Integer id) {
        getExistDeploy(id);
        int total = deployLogMapper.countExec(id);
        DeployExecution execution = deployExecutionMapper.selectLatest(id);
        if (total == 0 && execution == null) {
            return DeployExecSummaryVO.empty();
        }
        DeployExecSummaryVO vo = new DeployExecSummaryVO();
        vo.setTotalLines(total);
        vo.setStatus(execution == null ? "NONE" : execution.getStatus());
        vo.setHead(details(deployLogMapper.selectExecHead(id, 20)));
        List<DeployLog> tail = deployLogMapper.selectExecTail(id, 20);
        Collections.reverse(tail);
        vo.setTail(details(tail));
        return vo;
    }

    private List<Object> details(List<DeployLog> rows) {
        List<Object> lines = new ArrayList<Object>();
        if (rows == null) {
            return lines;
        }
        for (DeployLog row : rows) {
            lines.add(row.getDetail());
        }
        return lines;
    }

    private void rememberRollbackTarget(Deploy dbDeploy) {
        if (ObjectUtil.isNotEmpty(dbDeploy.getRollbackVersion())) {
            return;
        }
        String previous = deployMapper.selectLatestSuccessVersion(dbDeploy.getAppId(), dbDeploy.getId());
        if (ObjectUtil.isEmpty(previous)) {
            return;
        }
        Deploy patch = new Deploy();
        patch.setId(dbDeploy.getId());
        patch.setRollbackVersion(previous);
        deployMapper.updateById(patch);
        dbDeploy.setRollbackVersion(previous);
    }

    private void changeStatus(Deploy dbDeploy, String status, String finishTime, String deployType) {
        int version = dbDeploy.getRowVersion() == null ? 0 : dbDeploy.getRowVersion();
        int rows = deployMapper.casUpdateStatus(dbDeploy.getId(), dbDeploy.getStatus(), version, status, finishTime, deployType);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_CONFLICT);
        }
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        Deploy dbDeploy = getExistDeploy(id);
        checkOwnPendingForDev(dbDeploy);
        deployMapper.deleteById(id);
    }

    /**
     * 批量删除
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 修改，状态与审批相关字段不允许通过该接口变更
     */
    public void updateById(Deploy deploy) {
        Deploy dbDeploy = getExistDeploy(deploy.getId());
        checkOwnPendingForDev(dbDeploy);
        if (deploy.getExecuteMode() != null) {
            deploy.setExecuteMode(normalizeExecuteMode(deploy.getExecuteMode()));
        }
        deploy.setStatus(null);
        deploy.setApplicant(null);
        deploy.setApprover(null);
        deploy.setApproveTime(null);
        deploy.setFinishTime(null);
        deployMapper.updateById(deploy);
    }

    /**
     * DEV 只能操作自己提交且仍在待审批的部署单
     */
    private void checkOwnPendingForDev(Deploy dbDeploy) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (!RoleEnum.DEV.name().equals(currentUser.getRole())) {
            return;
        }
        boolean own = currentUser.getUsername().equals(dbDeploy.getApplicant());
        boolean pending = DeployStatusEnum.PENDING.name().equals(dbDeploy.getStatus());
        if (!own || !pending) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_NOT_OWNER_ERROR);
        }
    }

    /**
     * 根据ID查询
     */
    public Deploy selectById(Integer id) {
        return deployMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<Deploy> selectAll(Deploy deploy) {
        return deployMapper.selectAll(deploy);
    }

    /**
     * 分页查询
     */
    public PageInfo<Deploy> selectPage(Deploy deploy, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Deploy> list = deployMapper.selectAll(deploy);
        return PageInfo.of(list);
    }

    private String normalizeExecuteMode(String mode) {
        if (StrUtil.isBlank(mode)) {
            return DeployExecuteModeEnum.AUTO.name();
        }
        if (DeployExecuteModeEnum.AUTO.name().equals(mode) || DeployExecuteModeEnum.MANUAL.name().equals(mode)) {
            return mode;
        }
        throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
    }

    /**
     * 部署单号：D + yyyyMMddHHmmss + 3位随机数
     */
    private String generateDeployNo() {
        return "D" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(3);
    }

    /**
     * 查询部署单，不存在则抛异常
     */
    private Deploy getExistDeploy(Integer id) {
        Deploy dbDeploy = deployMapper.selectById(id);
        if (ObjectUtil.isNull(dbDeploy)) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_NOT_EXIST_ERROR);
        }
        return dbDeploy;
    }

    /**
     * 写入流转记录，与状态变更处于同一事务
     */
    private void saveLog(Integer deployId, DeployActionEnum action, String operator, String detail) {
        DeployLog deployLog = new DeployLog();
        deployLog.setDeployId(deployId);
        deployLog.setAction(action.name());
        deployLog.setOperator(operator);
        deployLog.setDetail(detail);
        deployLogMapper.insert(deployLog);
    }

}
