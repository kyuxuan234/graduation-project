package com.example.controller;

import com.example.common.Result;
import com.example.dto.DeployExecSummaryVO;
import com.example.dto.DeployLogLinesVO;
import com.example.entity.Deploy;
import com.example.service.DeployExecutionService;
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

    @Resource
    private DeployExecutionService deployExecutionService;

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
     * 技术审查、测试验收、产品验收。result 取 PASS 或 REJECTED。
     */
    @PutMapping("/gate/{id}")
    public Result<Void> gate(@PathVariable Integer id,
                             @RequestParam String result,
                             @RequestParam(required = false) String remark) {
        deployService.gate(id, result, remark);
        return Result.success();
    }

    /**
     * 开始部署
     */
    @PutMapping("/startDeploy/{id}")
    public Result<Void> startDeploy(@PathVariable Integer id, @RequestParam String executeMode) {
        deployExecutionService.startDeployEntry(id, executeMode);
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
        deployExecutionService.rollbackEntry(id);
        return Result.success();
    }

    /**
     * 执行日志，cursor 为已读到的最大行序
     */
    @GetMapping("/{id}/logs")
    public Result<DeployLogLinesVO> logs(@PathVariable Integer id,
                                         @RequestParam(required = false) Integer cursor) {
        return Result.success(deployService.execLogs(id, cursor));
    }

    /**
     * 执行摘要：总行数、最近一次执行状态、头尾各若干行
     */
    @GetMapping("/{id}/execSummary")
    public Result<DeployExecSummaryVO> execSummary(@PathVariable Integer id) {
        return Result.success(deployService.execSummary(id));
    }

}
