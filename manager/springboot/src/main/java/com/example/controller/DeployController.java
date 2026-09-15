package com.example.controller;

import com.example.common.Result;
import com.example.entity.Deploy;
import com.example.service.DeployService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * 部署单前端操作接口
 **/
@RestController
@RequestMapping("/deploy")
public class DeployController {

    @Resource
    private DeployService deployService;

    /**
     * 发起部署申请，只有DEV角色可以操作
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Deploy deploy) {
        deployService.add(deploy);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        deployService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        deployService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<Void> updateById(@RequestBody Deploy deploy) {
        deployService.updateById(deploy);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result<Deploy> selectById(@PathVariable Integer id) {
        Deploy deploy = deployService.selectById(id);
        return Result.success(deploy);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result<List<Deploy>> selectAll(Deploy deploy) {
        List<Deploy> list = deployService.selectAll(deploy);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result<PageInfo<Deploy>> selectPage(Deploy deploy,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Deploy> page = deployService.selectPage(deploy, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 审批，只有OPS角色可以操作，result 取 APPROVED 或 REJECTED
     */
    @PutMapping("/approve/{id}")
    public Result<Void> approve(@PathVariable Integer id,
                                @RequestParam String result,
                                @RequestParam(required = false) String remark) {
        deployService.approve(id, result, remark);
        return Result.success();
    }

    /**
     * 开始部署
     */
    @PutMapping("/startDeploy/{id}")
    public Result<Void> startDeploy(@PathVariable Integer id) {
        deployService.startDeploy(id);
        return Result.success();
    }

    /**
     * 部署完成，result 取 SUCCESS 或 FAILED
     */
    @PutMapping("/finishDeploy/{id}")
    public Result<Void> finishDeploy(@PathVariable Integer id,
                                     @RequestParam String result) {
        deployService.finishDeploy(id, result);
        return Result.success();
    }

    /**
     * 回滚
     */
    @PutMapping("/rollback/{id}")
    public Result<Void> rollback(@PathVariable Integer id) {
        deployService.rollback(id);
        return Result.success();
    }

}
