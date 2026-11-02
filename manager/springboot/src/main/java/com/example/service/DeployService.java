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
import com.example.common.enums.ServerEnvEnum;
import com.example.dto.DeployExecSummaryVO;
import com.example.dto.DeployLogLinesVO;
import com.example.entity.Account;
import com.example.entity.App;
import com.example.entity.AppInstance;
import com.example.entity.Deploy;
import com.example.entity.DeployExecution;
import com.example.entity.DeployLog;
import com.example.entity.Project;
import com.example.entity.Server;
import com.example.exception.BusinessException;
import com.example.mapper.AppInstanceMapper;
import com.example.mapper.AppMapper;
import com.example.mapper.DeployExecutionMapper;
import com.example.mapper.DeployLogMapper;
import com.example.mapper.DeployMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.ServerMapper;
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
    private ServerMapper serverMapper;

    @Resource
    private DeployLogMapper deployLogMapper;

    @Resource
    private AppMapper appMapper;

    @Resource
    private AppInstanceMapper appInstanceMapper;

    @Resource
    private ProjectMapper projectMapper;

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
        if (StrUtil.isBlank(dbApp.getRepoUrl())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, "应用未配置代码仓库地址，无法生成拉取脚本");
        }
        if (StrUtil.isBlank(deploy.getVersion())) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR.code, "请填写目标版本");
        }
        if (ObjectUtil.isNull(deploy.getServerId())) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR.code, "请选择目标服务器");
        }
        Server server = serverMapper.selectById(deploy.getServerId());
        if (ObjectUtil.isNull(server) || StrUtil.isBlank(server.getEnv())) {
            throw new BusinessException(ResultCodeEnum.SERVER_NOT_EXIST_ERROR);
        }
        String env = server.getEnv();
        AppInstance instance = appInstanceMapper.selectByAppAndEnv(deploy.getAppId(), env);
        if (instance == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, "该应用尚未挂到" + envLabel(env) + "环境");
        }
        if (!env.equals(server.getEnv())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, "目标服务器不属于该环境");
        }
        assertVersionPromotable(deploy.getAppId(), env, deploy.getVersion().trim());
        deploy.setVersion(deploy.getVersion().trim());
        deploy.setEnvCode(env);
        deploy.setDeployNo(generateDeployNo());
        deploy.setStatus(initialStatus(env));
        deploy.setDeployType(DeployTypeEnum.FULL.name());
        deploy.setApplicant(currentUser.getUsername());
        deploy.setApplyTime(DateUtil.now());
        deploy.setExecuteMode(normalizeExecuteMode(deploy.getExecuteMode()));
        deployMapper.insert(deploy);
        saveLog(deploy.getId(), DeployActionEnum.APPLY, currentUser.getUsername(),
                "提交发布申请，环境：" + envLabel(env) + "，目标版本：" + deploy.getVersion());
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
     * 技术审查、测试验收、产品验收。result 取 PASS 或 REJECTED。
     */
    @Transactional(rollbackFor = Exception.class)
    public void gate(Integer id, String result, String remark) {
        Deploy dbDeploy = getExistDeploy(id);
        if (!DeployStatusMachine.isReviewGate(dbDeploy.getStatus())) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_STATUS_ERROR);
        }
        RoleEnum owner = reviewRole(dbDeploy.getStatus());
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_GATE_NO_PERMISSION, owner);
        boolean pass = "PASS".equals(result);
        if (!pass && !DeployStatusEnum.REJECTED.name().equals(result)) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_STATUS_ERROR);
        }
        String next = pass ? DeployStatusMachine.passTarget(dbDeploy.getStatus(), targetEnv(dbDeploy)) : DeployStatusEnum.REJECTED.name();
        String operator = currentUser.getUsername();
        int version = dbDeploy.getRowVersion() == null ? 0 : dbDeploy.getRowVersion();
        int rows = deployMapper.casApprove(id, dbDeploy.getStatus(), version,
                next, operator, DateUtil.now(), remark);
        if (rows == 0) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_CONFLICT);
        }
        String stage = reviewStage(dbDeploy.getStatus());
        String detail = pass ? stage + "通过" : stage + "驳回";
        if (ObjectUtil.isNotEmpty(remark)) {
            detail = detail + "，意见：" + remark;
        }
        saveLog(id, pass ? DeployActionEnum.APPROVE : DeployActionEnum.REJECT, operator, detail);
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

        markInstance(dbDeploy, AppStatusEnum.DEPLOYING.name(), null);

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

        if (success) {
            markInstance(dbDeploy, AppStatusEnum.ONLINE.name(), versionToWrite);
        } else {
            markInstance(dbDeploy, AppStatusEnum.OFFLINE.name(), null);
        }

        DeployActionEnum action = success ? DeployActionEnum.FINISH : DeployActionEnum.FAIL;
        String detail = success ? "部署成功，版本：" + versionToWrite : "部署失败，版本：" + dbDeploy.getVersion();
        saveLog(id, action, operator, detail);
    }

    /**
     * 回滚，只有发布失败的单子可以回滚，回滚后重新进入部署中状态，再走 finishDeploy
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollback(Integer id) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_EXECUTE_NO_PERMISSION, RoleEnum.OPS);
        Deploy dbDeploy = getExistDeploy(id);
        DeployStatusMachine.assertRollback(dbDeploy.getStatus());
        if (ObjectUtil.isEmpty(dbDeploy.getRollbackVersion())) {
            String previous = deployMapper.selectLatestSuccessVersion(dbDeploy.getAppId(), id, dbDeploy.getEnvCode());
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

        markInstance(dbDeploy, AppStatusEnum.DEPLOYING.name(), null);

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
        deployLog.setOperator(null);
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

    private String initialStatus(String env) {
        if (ServerEnvEnum.DEV.name().equals(env)) {
            return DeployStatusEnum.PENDING.name();
        }
        if (ServerEnvEnum.TEST.name().equals(env)) {
            return DeployStatusEnum.TESTING.name();
        }
        return DeployStatusEnum.REVIEW.name();
    }

    private String targetEnv(Deploy deploy) {
        if (StrUtil.isNotBlank(deploy.getEnvCode())) {
            return deploy.getEnvCode();
        }
        if (deploy.getServerId() == null) {
            return ServerEnvEnum.PROD.name();
        }
        Server server = serverMapper.selectById(deploy.getServerId());
        if (server == null || StrUtil.isBlank(server.getEnv())) {
            return ServerEnvEnum.PROD.name();
        }
        return server.getEnv();
    }

    private void assertVersionPromotable(Integer appId, String env, String version) {
        String source = null;
        if (ServerEnvEnum.TEST.name().equals(env)) {
            source = ServerEnvEnum.DEV.name();
        } else if (ServerEnvEnum.PROD.name().equals(env)) {
            source = ServerEnvEnum.TEST.name();
        }
        if (source == null) {
            return;
        }
        if (deployMapper.countSuccessVersion(appId, source, version) <= 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code,
                    "版本 " + version + " 尚未在" + envLabel(source) + "环境发布成功，不能进入" + envLabel(env) + "环境");
        }
    }

    public java.util.List<String> promotableVersions(Integer appId, String targetEnv) {
        AuthUtils.requireLogin();
        String source = null;
        if (ServerEnvEnum.TEST.name().equals(targetEnv)) {
            source = ServerEnvEnum.DEV.name();
        } else if (ServerEnvEnum.PROD.name().equals(targetEnv)) {
            source = ServerEnvEnum.TEST.name();
        }
        if (appId == null || source == null) {
            return java.util.Collections.emptyList();
        }
        return deployMapper.selectSuccessVersions(appId, source);
    }

    private void markInstance(Deploy deploy, String status, String version) {
        String env = targetEnv(deploy);
        AppInstance instance = appInstanceMapper.selectByAppAndEnv(deploy.getAppId(), env);
        if (instance == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, "找不到该环境上的应用实例");
        }
        AppInstance patch = new AppInstance();
        patch.setId(instance.getId());
        patch.setStatus(status);
        if (version != null) {
            patch.setVersion(version);
        }
        appInstanceMapper.updateById(patch);
    }

    private String envLabel(String env) {
        if (ServerEnvEnum.DEV.name().equals(env)) {
            return "开发";
        }
        if (ServerEnvEnum.TEST.name().equals(env)) {
            return "测试";
        }
        if (ServerEnvEnum.PROD.name().equals(env)) {
            return "生产";
        }
        return StrUtil.blankToDefault(env, "生产");
    }

    private RoleEnum reviewRole(String status) {
        if (DeployStatusEnum.REVIEW.name().equals(status)) {
            return RoleEnum.TL;
        }
        if (DeployStatusEnum.TESTING.name().equals(status)) {
            return RoleEnum.QA;
        }
        if (DeployStatusEnum.ACCEPTING.name().equals(status)) {
            return RoleEnum.PM;
        }
        return null;
    }

    private String reviewStage(String status) {
        if (DeployStatusEnum.REVIEW.name().equals(status)) {
            return "技术审查";
        }
        if (DeployStatusEnum.TESTING.name().equals(status)) {
            return "测试验收";
        }
        if (DeployStatusEnum.ACCEPTING.name().equals(status)) {
            return "产品验收";
        }
        return "审查";
    }

    private void rememberRollbackTarget(Deploy dbDeploy) {
        if (ObjectUtil.isNotEmpty(dbDeploy.getRollbackVersion())) {
            return;
        }
        String previous = deployMapper.selectLatestSuccessVersion(dbDeploy.getAppId(), dbDeploy.getId(), dbDeploy.getEnvCode());
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
     * DEV 只能操作自己提交、且仍在待技术审查的发布单
     */
    private void checkOwnPendingForDev(Deploy dbDeploy) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (!RoleEnum.DEV.name().equals(currentUser.getRole())) {
            return;
        }
        boolean own = currentUser.getUsername().equals(dbDeploy.getApplicant());
        boolean pending = DeployStatusEnum.REVIEW.name().equals(dbDeploy.getStatus());
        if (!own || !pending) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_NOT_OWNER_ERROR);
        }
    }

    /**
     * 根据ID查询
     */
    public Deploy selectById(Integer id) {
        Deploy deploy = deployMapper.selectById(id);
        if (deploy != null && !canView(deploy)) {
            throw new BusinessException(ResultCodeEnum.NO_PERMISSION_ERROR);
        }
        return deploy;
    }

    /**
     * 查询所有
     */
    public List<Deploy> selectAll(Deploy deploy) {
        if (deploy == null) {
            deploy = new Deploy();
        }
        applyVisibility(deploy);
        return deployMapper.selectAll(deploy);
    }

    /**
     * 分页查询
     */
    public PageInfo<Deploy> selectPage(Deploy deploy, Integer pageNum, Integer pageSize) {
        if (deploy == null) {
            deploy = new Deploy();
        }
        applyVisibility(deploy);
        PageHelper.startPage(pageNum, pageSize);
        List<Deploy> list = deployMapper.selectAll(deploy);
        return PageInfo.of(list);
    }

    /**
     * 按角色和环境只露出本人该处理的发布单。超级管理员仍看全部。
     */
    private void applyVisibility(Deploy query) {
        Account currentUser = TokenUtils.getCurrentUser();
        String role = currentUser == null ? null : currentUser.getRole();
        if (role == null || RoleEnum.ADMIN.name().equals(role)) {
            return;
        }
        if (RoleEnum.DEV.name().equals(role)) {
            query.setApplicant(null);
            query.setScopeApplicant(currentUser.getUsername());
            query.setScopeLeader(currentUser.getName());
            return;
        }
        if (RoleEnum.TL.name().equals(role)) {
            query.setStatus(DeployStatusEnum.REVIEW.name());
            query.setScopeEnv(ServerEnvEnum.PROD.name());
            return;
        }
        if (RoleEnum.QA.name().equals(role)) {
            query.setStatus(DeployStatusEnum.TESTING.name());
            query.setScopeEnv(ServerEnvEnum.TEST.name());
            return;
        }
        if (RoleEnum.PM.name().equals(role)) {
            query.setStatus(DeployStatusEnum.ACCEPTING.name());
            query.setScopeEnv(ServerEnvEnum.PROD.name());
            return;
        }
        if (RoleEnum.OPS.name().equals(role)) {
            List<String> allowed = new ArrayList<String>();
            allowed.add(DeployStatusEnum.PENDING.name());
            allowed.add(DeployStatusEnum.APPROVED.name());
            allowed.add(DeployStatusEnum.DEPLOYING.name());
            allowed.add(DeployStatusEnum.SUCCESS.name());
            allowed.add(DeployStatusEnum.FAILED.name());
            List<String> requested = query.getScopeStatuses();
            if (requested != null && !requested.isEmpty()) {
                List<String> narrowed = new ArrayList<String>();
                for (String item : requested) {
                    if (allowed.contains(item)) {
                        narrowed.add(item);
                    }
                }
                if (narrowed.isEmpty()) {
                    narrowed.add("__NONE__");
                }
                query.setStatus(null);
                query.setScopeStatuses(narrowed);
                return;
            }
            if (query.getStatus() == null || !allowed.contains(query.getStatus())) {
                query.setStatus(null);
                query.setScopeStatuses(allowed);
            }
        }
    }

    private boolean canView(Deploy deploy) {
        Account currentUser = TokenUtils.getCurrentUser();
        String role = currentUser == null ? null : currentUser.getRole();
        if (role == null || RoleEnum.ADMIN.name().equals(role)) {
            return true;
        }
        if (RoleEnum.DEV.name().equals(role)) {
            return currentUser.getUsername().equals(deploy.getApplicant()) || isProjectLeader(deploy, currentUser);
        }
        if (RoleEnum.TL.name().equals(role)) {
            return DeployStatusEnum.REVIEW.name().equals(deploy.getStatus())
                    && ServerEnvEnum.PROD.name().equals(deploy.getServerEnv());
        }
        if (RoleEnum.QA.name().equals(role)) {
            return DeployStatusEnum.TESTING.name().equals(deploy.getStatus())
                    && ServerEnvEnum.TEST.name().equals(deploy.getServerEnv());
        }
        if (RoleEnum.PM.name().equals(role)) {
            return DeployStatusEnum.ACCEPTING.name().equals(deploy.getStatus())
                    && ServerEnvEnum.PROD.name().equals(deploy.getServerEnv());
        }
        if (RoleEnum.OPS.name().equals(role)) {
            String status = deploy.getStatus();
            return DeployStatusEnum.PENDING.name().equals(status)
                    || DeployStatusEnum.APPROVED.name().equals(status)
                    || DeployStatusEnum.DEPLOYING.name().equals(status)
                    || DeployStatusEnum.SUCCESS.name().equals(status)
                    || DeployStatusEnum.FAILED.name().equals(status);
        }
        return false;
    }

    private boolean isProjectLeader(Deploy deploy, Account user) {
        if (deploy == null || deploy.getAppId() == null || user == null) {
            return false;
        }
        App app = appMapper.selectById(deploy.getAppId());
        if (app == null || app.getProjectId() == null) {
            return false;
        }
        Project project = projectMapper.selectById(app.getProjectId());
        if (project == null || StrUtil.isBlank(project.getLeader())) {
            return false;
        }
        return project.getLeader().equals(user.getUsername()) || project.getLeader().equals(user.getName());
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
        deployLog.setOperator(StrUtil.isBlank(operator) || "system".equals(operator) ? null : operator);
        deployLog.setDetail(detail);
        deployLogMapper.insert(deployLog);
    }

}
