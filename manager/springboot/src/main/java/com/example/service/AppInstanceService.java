package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.AppStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.common.enums.ServerEnvEnum;
import com.example.entity.Account;
import com.example.entity.App;
import com.example.entity.AppInstance;
import com.example.entity.Server;
import com.example.exception.BusinessException;
import com.example.mapper.AppInstanceMapper;
import com.example.mapper.AppMapper;
import com.example.mapper.ServerMapper;
import com.example.utils.AuthUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AppInstanceService {

    @Resource
    private AppInstanceMapper appInstanceMapper;
    @Resource
    private AppMapper appMapper;
    @Resource
    private ServerMapper serverMapper;

    public List<AppInstance> listByApp(Integer appId) {
        AuthUtils.requireLogin();
        if (appId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR.code, "请选择应用");
        }
        return appInstanceMapper.selectByAppId(appId);
    }

    public void add(AppInstance row) {
        Account user = AuthUtils.requireRole(RoleEnum.DEV, RoleEnum.OPS);
        validate(row, user, true);
        if (appInstanceMapper.selectByAppAndEnv(row.getAppId(), row.getEnvCode()) != null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, "该应用在此环境已有实例");
        }
        if (StrUtil.isBlank(row.getStatus())) {
            row.setStatus(AppStatusEnum.OFFLINE.name());
        }
        appInstanceMapper.insert(row);
    }

    public void update(AppInstance row) {
        Account user = AuthUtils.requireRole(RoleEnum.DEV, RoleEnum.OPS);
        if (row == null || row.getId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR.code, "请选择实例");
        }
        AppInstance db = appInstanceMapper.selectById(row.getId());
        if (db == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, "实例不存在");
        }
        assertCanMaintain(user, db.getEnvCode());
        if (AppStatusEnum.ONLINE.name().equals(db.getStatus()) || AppStatusEnum.DEPLOYING.name().equals(db.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR.code, "在线或发布中的实例不能改挂载");
        }
        if (row.getServerId() != null) {
            assertServerInEnv(row.getServerId(), db.getEnvCode());
        }
        AppInstance patch = new AppInstance();
        patch.setId(db.getId());
        patch.setServerId(row.getServerId());
        patch.setPort(row.getPort());
        appInstanceMapper.updateById(patch);
    }

    private void validate(AppInstance row, Account user, boolean creating) {
        if (row == null || row.getAppId() == null || StrUtil.isBlank(row.getEnvCode()) || row.getServerId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR.code, "请选择应用、环境和服务器");
        }
        String env = row.getEnvCode().trim();
        row.setEnvCode(env);
        if (!ServerEnvEnum.DEV.name().equals(env) && !ServerEnvEnum.TEST.name().equals(env) && !ServerEnvEnum.PROD.name().equals(env)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, "环境只能是开发、测试或生产");
        }
        assertCanMaintain(user, env);
        App app = appMapper.selectById(row.getAppId());
        if (app == null) {
            throw new BusinessException(ResultCodeEnum.APP_NOT_EXIST_ERROR);
        }
        assertServerInEnv(row.getServerId(), env);
    }

    private void assertCanMaintain(Account user, String env) {
        if (RoleEnum.ADMIN.name().equals(user.getRole()) || RoleEnum.OPS.name().equals(user.getRole())) {
            return;
        }
        if (RoleEnum.DEV.name().equals(user.getRole()) && ServerEnvEnum.DEV.name().equals(env)) {
            return;
        }
        throw new BusinessException(ResultCodeEnum.NO_PERMISSION_ERROR.code, "测试和生产环境的实例由运维维护");
    }

    private void assertServerInEnv(Integer serverId, String env) {
        Server server = serverMapper.selectById(serverId);
        if (server == null) {
            throw new BusinessException(ResultCodeEnum.SERVER_NOT_EXIST_ERROR);
        }
        if (!env.equals(server.getEnv())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, "服务器不属于该环境");
        }
    }
}
