package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.Dev;
import com.example.service.DevService;
import com.example.utils.AuthUtils;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * 开发人员前端操作接口，仅管理员可访问
 **/
@RestController
@RequestMapping("/dev")
public class DevController {

    @Resource
    private DevService devService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Dev dev) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        devService.add(dev);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        devService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        devService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<Void> updateById(@RequestBody Dev dev) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        devService.updateById(dev);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result<Dev> selectById(@PathVariable Integer id) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        Dev dev = devService.selectById(id);
        if (dev != null) {
            // service层要把密码密文交给jwt拦截器验签，对外接口需去掉
            dev.setPassword(null);
        }
        return Result.success(dev);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result<List<Dev>> selectAll(Dev dev) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        List<Dev> list = devService.selectAll(dev);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result<PageInfo<Dev>> selectPage(Dev dev,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        PageInfo<Dev> page = devService.selectPage(dev, pageNum, pageSize);
        return Result.success(page);
    }

}
