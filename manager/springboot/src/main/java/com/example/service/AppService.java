package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.DeployScripts;
import com.example.common.enums.AppStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.App;
import com.example.entity.Server;
import com.example.exception.BusinessException;
import com.example.mapper.AppInstanceMapper;
import com.example.mapper.AppMapper;
import com.example.mapper.ProjectMapper;
import com.example.mapper.ServerMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 应用业务处理
 **/
@Service
public class AppService {

    @Resource
    private AppMapper appMapper;

    @Resource
    private AppInstanceMapper appInstanceMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ServerMapper serverMapper;

    /**
     * 按应用仓库、版本和目标机部署目录生成拉取代码脚本。
     */
    public String repoScript(App app) {
        if (app == null || app.getId() == null || StrUtil.isBlank(app.getRepoUrl()) || StrUtil.isBlank(app.getVersion()) || app.getServerId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR.code, "请先选择应用、版本和目标服务器");
        }
        Server server = serverMapper.selectById(app.getServerId());
        if (server == null || StrUtil.isBlank(server.getDeployPath())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, "目标服务器未配置部署目录");
        }
        String appKey = StrUtil.isNotBlank(app.getCode()) ? app.getCode() : app.getName();
        if (StrUtil.isBlank(appKey)) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR.code, "请填写应用编号");
        }
        try {
            return DeployScripts.deploy(appKey, app.getVersion().trim(), server.getDeployPath(), app.getRepoUrl().trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.code, e.getMessage());
        }
    }

    /**
     * 新增
     */
    public void add(App app) {
        checkCodeUnique(app.getCode(), null);
        requireProject(app.getProjectId());
        if (ObjectUtil.isEmpty(app.getStatus())) {
            app.setStatus(AppStatusEnum.OFFLINE.name());
        }
        appMapper.insert(app);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        App dbApp = appMapper.selectById(id);
        if (ObjectUtil.isNull(dbApp)) {
            throw new BusinessException(ResultCodeEnum.APP_NOT_EXIST_ERROR);
        }
        if (appInstanceMapper.countOnlineByAppId(id) > 0) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        appInstanceMapper.deleteByAppId(id);
        appMapper.deleteById(id);
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
     * 修改
     */
    public void updateById(App app) {
        checkCodeUnique(app.getCode(), app.getId());
        if (app.getProjectId() != null) {
            requireProject(app.getProjectId());
        }
        appMapper.updateById(app);
    }

    /**
     * 应用编号唯一性校验，excludeId 为本次修改的记录id
     */
    private void checkCodeUnique(String code, Integer excludeId) {
        if (ObjectUtil.isEmpty(code)) {
            return;
        }
        App dbApp = appMapper.selectByCode(code);
        if (ObjectUtil.isNotNull(dbApp) && !dbApp.getId().equals(excludeId)) {
            throw new BusinessException(ResultCodeEnum.APP_CODE_EXIST_ERROR);
        }
    }

    private void requireProject(Integer projectId) {
        if (projectId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_LOST_ERROR.code, "应用必须属于一个项目");
        }
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ResultCodeEnum.PROJECT_NOT_EXIST_ERROR);
        }
    }

    /**
     * 根据ID查询
     */
    public App selectById(Integer id) {
        return appMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<App> selectAll(App app) {
        return appMapper.selectAll(app);
    }

    /**
     * 分页查询
     */
    public PageInfo<App> selectPage(App app, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<App> list = appMapper.selectAll(app);
        return PageInfo.of(list);
    }

}
