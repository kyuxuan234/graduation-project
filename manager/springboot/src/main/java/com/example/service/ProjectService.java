package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ProjectStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Person;
import com.example.entity.Project;
import com.example.exception.BusinessException;
import com.example.mapper.AppMapper;
import com.example.mapper.ProjectMapper;
import com.example.utils.TokenUtils;
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

    @Resource
    private PersonService personService;

    /**
     * 新增。开发负责人只能由技术负责人或超级管理员指定。
     */
    public void add(Project project) {
        checkCodeUnique(project.getCode(), null);
        if (ObjectUtil.isEmpty(project.getStatus())) {
            project.setStatus(ProjectStatusEnum.NOT_STARTED.name());
        }
        if (canAssignLeader()) {
            project.setLeader(normalizeLeader(project.getLeader()));
        } else {
            project.setLeader(null);
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
     * 修改。技术负责人只能改开发负责人，开发不能改这个字段。
     */
    public void updateById(Project project) {
        Project dbProject = projectMapper.selectById(project.getId());
        if (ObjectUtil.isNull(dbProject)) {
            throw new BusinessException(ResultCodeEnum.PROJECT_NOT_EXIST_ERROR);
        }
        if (ProjectStatusEnum.FINISHED.name().equals(dbProject.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        if (isTechnicalLead()) {
            Project patch = new Project();
            patch.setId(project.getId());
            patch.setLeader(normalizeLeader(project.getLeader()));
            projectMapper.updateById(patch);
            return;
        }
        checkCodeUnique(project.getCode(), project.getId());
        if (canAssignLeader()) {
            project.setLeader(normalizeLeader(project.getLeader()));
        } else {
            project.setLeader(dbProject.getLeader());
        }
        projectMapper.updateById(project);
    }

    private boolean canAssignLeader() {
        String role = currentRole();
        return RoleEnum.ADMIN.name().equals(role) || RoleEnum.TL.name().equals(role);
    }

    private boolean isTechnicalLead() {
        return RoleEnum.TL.name().equals(currentRole());
    }

    private String currentRole() {
        Account current = TokenUtils.getCurrentUser();
        return current == null ? null : current.getRole();
    }

    /**
     * 空表示尚未分配。有值时必须是在职开发，并统一存用户名。
     */
    private String normalizeLeader(String leader) {
        if (StrUtil.isBlank(leader)) {
            return null;
        }
        Person person = personService.findActiveDeveloper(leader);
        if (person == null) {
            throw new BusinessException(ResultCodeEnum.PROJECT_LEADER_INVALID);
        }
        return person.getUsername();
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
