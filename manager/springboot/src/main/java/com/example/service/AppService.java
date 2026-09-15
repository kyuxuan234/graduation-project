package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.AppStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.App;
import com.example.exception.BusinessException;
import com.example.mapper.AppMapper;
import com.example.mapper.DeployMapper;
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
    private DeployMapper deployMapper;

    /**
     * 新增
     */
    public void add(App app) {
        checkCodeUnique(app.getCode(), null);
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
        if (AppStatusEnum.ONLINE.name().equals(dbApp.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        // 该应用下仍有部署单时不允许删除
        if (deployMapper.countByAppId(id) > 0) {
            throw new BusinessException(ResultCodeEnum.APP_HAS_DEPLOYS);
        }
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
