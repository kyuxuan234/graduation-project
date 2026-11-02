package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.App;
import com.example.service.AppService;
import com.example.utils.AuthUtils;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * 应用前端操作接口
 **/
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @PostMapping("/repoScript")
    public Result<String> repoScript(@RequestBody App app) {
        AuthUtils.requireRole(RoleEnum.DEV, RoleEnum.OPS, RoleEnum.TL);
        return Result.success(appService.repoScript(app));
    }

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody App app) {
        AuthUtils.requireRole(RoleEnum.DEV, RoleEnum.OPS);
        appService.add(app);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        AuthUtils.requireRole(RoleEnum.DEV, RoleEnum.OPS);
        appService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        AuthUtils.requireRole(RoleEnum.DEV, RoleEnum.OPS);
        appService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<Void> updateById(@RequestBody App app) {
        AuthUtils.requireRole(RoleEnum.DEV, RoleEnum.OPS);
        appService.updateById(app);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result<App> selectById(@PathVariable Integer id) {
        App app = appService.selectById(id);
        return Result.success(app);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result<List<App>> selectAll(App app) {
        List<App> list = appService.selectAll(app);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result<PageInfo<App>> selectPage(App app,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<App> page = appService.selectPage(app, pageNum, pageSize);
        return Result.success(page);
    }

}
