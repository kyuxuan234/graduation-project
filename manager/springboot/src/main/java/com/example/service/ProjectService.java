package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ProjectStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Project;
import com.example.exception.BusinessException;
import com.example.mapper.AppMapper;
import com.example.mapper.ProjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 项目业务处理
 **/
@Service
public class ProjectService {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private AppMapper appMapper;

    /**
     * 新增
     */
    public void add(Project project) {
        checkCodeUnique(project.getCode(), null);
        if (ObjectUtil.isEmpty(project.getStatus())) {
            project.setStatus(ProjectStatusEnum.NOT_STARTED.name());
        }
        projectMapper.insert(project);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        Project dbProject = projectMapper.selectById(id);
        if (ObjectUtil.isNull(dbProject)) {
            throw new BusinessException(ResultCodeEnum.PROJECT_NOT_EXIST_ERROR);
        }
        // 该项目下仍有应用时不允许删除
        if (ProjectStatusEnum.FINISHED.name().equals(dbProject.getStatus())
                || ProjectStatusEnum.PAUSED.name().equals(dbProject.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        if (appMapper.countByProjectId(id) > 0) {
            throw new BusinessException(ResultCodeEnum.PROJECT_HAS_APPS);
        }
        projectMapper.deleteById(id);
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
    public void updateById(Project project) {
        Project dbProject = projectMapper.selectById(project.getId());
        if (ObjectUtil.isNull(dbProject)) {
            throw new BusinessException(ResultCodeEnum.PROJECT_NOT_EXIST_ERROR);
        }
        if (ProjectStatusEnum.FINISHED.name().equals(dbProject.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        checkCodeUnique(project.getCode(), project.getId());
        projectMapper.updateById(project);
    }

    /**
     * 项目编号唯一性校验，excludeId 为本次修改的记录id
     */
    private void checkCodeUnique(String code, Integer excludeId) {
        if (ObjectUtil.isEmpty(code)) {
            return;
        }
        Project dbProject = projectMapper.selectByCode(code);
        if (ObjectUtil.isNotNull(dbProject) && !dbProject.getId().equals(excludeId)) {
            throw new BusinessException(ResultCodeEnum.PROJECT_CODE_EXIST_ERROR);
        }
    }

    /**
     * 根据ID查询
     */
    public Project selectById(Integer id) {
        return projectMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<Project> selectAll(Project project) {
        return projectMapper.selectAll(project);
    }

    /**
     * 分页查询
     */
    public PageInfo<Project> selectPage(Project project, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Project> list = projectMapper.selectAll(project);
        return PageInfo.of(list);
    }

}
