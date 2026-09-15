package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.Ops;
import com.example.service.OpsService;
import com.example.utils.AuthUtils;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * 运维人员前端操作接口，仅管理员可访问
 **/
@RestController
@RequestMapping("/ops")
public class OpsController {

    @Resource
    private OpsService opsService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Ops ops) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        opsService.add(ops);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        opsService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        opsService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<Void> updateById(@RequestBody Ops ops) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        opsService.updateById(ops);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result<Ops> selectById(@PathVariable Integer id) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        Ops ops = opsService.selectById(id);
        if (ops != null) {
            // service层要把密码密文交给jwt拦截器验签，对外接口需去掉
            ops.setPassword(null);
        }
        return Result.success(ops);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result<List<Ops>> selectAll(Ops ops) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        List<Ops> list = opsService.selectAll(ops);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result<PageInfo<Ops>> selectPage(Ops ops,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        AuthUtils.requireRole(RoleEnum.ADMIN);
        PageInfo<Ops> page = opsService.selectPage(ops, pageNum, pageSize);
        return Result.success(page);
    }

}
