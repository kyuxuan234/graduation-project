package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.Project;
import com.example.service.ProjectService;
import com.example.utils.AuthUtils;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * 项目前端操作接口
 **/
@RestController
@RequestMapping("/project")
public class ProjectController {

    @Resource
    private ProjectService projectService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Project project) {
        AuthUtils.requireRole(RoleEnum.DEV);
        projectService.add(project);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        AuthUtils.requireRole(RoleEnum.DEV);
        projectService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        AuthUtils.requireRole(RoleEnum.DEV);
        projectService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<Void> updateById(@RequestBody Project project) {
        AuthUtils.requireRole(RoleEnum.DEV, RoleEnum.TL);
        projectService.updateById(project);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result<Project> selectById(@PathVariable Integer id) {
        Project project = projectService.selectById(id);
        return Result.success(project);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result<List<Project>> selectAll(Project project) {
        List<Project> list = projectService.selectAll(project);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result<PageInfo<Project>> selectPage(Project project,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Project> page = projectService.selectPage(project, pageNum, pageSize);
        return Result.success(page);
    }

}
