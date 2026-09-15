package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.common.DeployStatusMachine;
import com.example.common.enums.AppStatusEnum;
import com.example.common.enums.DeployActionEnum;
import com.example.common.enums.DeployStatusEnum;
import com.example.common.enums.DeployTypeEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.App;
import com.example.entity.Deploy;
import com.example.entity.DeployLog;
import com.example.exception.BusinessException;
import com.example.mapper.AppMapper;
import com.example.mapper.DeployLogMapper;
import com.example.mapper.DeployMapper;
import com.example.utils.AuthUtils;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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

        Deploy updateDeploy = new Deploy();
        updateDeploy.setId(id);
        updateDeploy.setStatus(result);
        updateDeploy.setApprover(currentUser.getUsername());
        updateDeploy.setApproveTime(DateUtil.now());
        updateDeploy.setRemark(remark);
        deployMapper.updateById(updateDeploy);

        DeployActionEnum action = approved ? DeployActionEnum.APPROVE : DeployActionEnum.REJECT;
        String detail = approved ? "审批通过" : "审批驳回";
        if (ObjectUtil.isNotEmpty(remark)) {
            detail = detail + "，意见：" + remark;
        }
        saveLog(id, action, currentUser.getUsername(), detail);
    }

    /**
     * 开始部署
     */
    @Transactional(rollbackFor = Exception.class)
    public void startDeploy(Integer id) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_EXECUTE_NO_PERMISSION, RoleEnum.OPS);
        Deploy dbDeploy = getExistDeploy(id);
        DeployStatusMachine.assertStart(dbDeploy.getStatus());

        Deploy updateDeploy = new Deploy();
        updateDeploy.setId(id);
        updateDeploy.setStatus(DeployStatusEnum.DEPLOYING.name());
        deployMapper.updateById(updateDeploy);

        // 开始部署只把应用置为部署中，版本与在线状态等 finishDeploy 成功后再反写
        App updateApp = new App();
        updateApp.setId(dbDeploy.getAppId());
        updateApp.setStatus(AppStatusEnum.DEPLOYING.name());
        appMapper.updateById(updateApp);

        saveLog(id, DeployActionEnum.START_DEPLOY, currentUser.getUsername(),
                "开始部署，目标版本：" + dbDeploy.getVersion());
    }

    /**
     * 部署完成，result 为 SUCCESS 或 FAILED：成功则反写目标版本并置在线，失败则置离线
     */
    @Transactional(rollbackFor = Exception.class)
    public void finishDeploy(Integer id, String result) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_EXECUTE_NO_PERMISSION, RoleEnum.OPS);
        Deploy dbDeploy = getExistDeploy(id);
        DeployStatusMachine.assertFinish(dbDeploy.getStatus(), result);
        boolean success = DeployStatusEnum.SUCCESS.name().equals(result);

        Deploy updateDeploy = new Deploy();
        updateDeploy.setId(id);
        updateDeploy.setStatus(result);
        updateDeploy.setFinishTime(DateUtil.now());
        deployMapper.updateById(updateDeploy);

        App updateApp = new App();
        updateApp.setId(dbDeploy.getAppId());
        if (success) {
            // 部署成功才把目标版本同步到应用并置为在线
            updateApp.setVersion(dbDeploy.getVersion());
            updateApp.setStatus(AppStatusEnum.ONLINE.name());
        } else {
            // 部署失败不能停留在在线/部署中，回落到离线，避免资产台账失真
            updateApp.setStatus(AppStatusEnum.OFFLINE.name());
        }
        appMapper.updateById(updateApp);

        DeployActionEnum action = success ? DeployActionEnum.FINISH : DeployActionEnum.FAIL;
        String detail = success ? "部署成功，版本：" + dbDeploy.getVersion() : "部署失败，版本：" + dbDeploy.getVersion();
        saveLog(id, action, currentUser.getUsername(), detail);
    }

    /**
     * 回滚，只有部署成功的单子可以回滚，回滚后重新进入部署中状态，再走 finishDeploy
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollback(Integer id) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.DEPLOY_EXECUTE_NO_PERMISSION, RoleEnum.OPS);
        Deploy dbDeploy = getExistDeploy(id);
        DeployStatusMachine.assertRollback(dbDeploy.getStatus());

        Deploy updateDeploy = new Deploy();
        updateDeploy.setId(id);
        updateDeploy.setDeployType(DeployTypeEnum.ROLLBACK.name());
        updateDeploy.setStatus(DeployStatusEnum.DEPLOYING.name());
        updateDeploy.setFinishTime("");
        deployMapper.updateById(updateDeploy);

        App updateApp = new App();
        updateApp.setId(dbDeploy.getAppId());
        updateApp.setStatus(AppStatusEnum.DEPLOYING.name());
        appMapper.updateById(updateApp);

        saveLog(id, DeployActionEnum.ROLLBACK, currentUser.getUsername(),
                "发起回滚，原版本：" + dbDeploy.getVersion());
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
