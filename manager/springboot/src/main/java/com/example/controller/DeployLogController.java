package com.example.controller;

import com.example.common.Result;
import com.example.entity.DeployLog;
import com.example.service.DeployLogService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * 部署流转记录前端操作接口
 * 记录只由Deploy模块内部写入，这里只提供查询
 **/
@RestController
@RequestMapping("/deployLog")
public class DeployLogController {

    @Resource
    private DeployLogService deployLogService;

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result<DeployLog> selectById(@PathVariable Integer id) {
        DeployLog deployLog = deployLogService.selectById(id);
        return Result.success(deployLog);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result<List<DeployLog>> selectAll(DeployLog deployLog) {
        List<DeployLog> list = deployLogService.selectAll(deployLog);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result<PageInfo<DeployLog>> selectPage(DeployLog deployLog,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<DeployLog> page = deployLogService.selectPage(deployLog, pageNum, pageSize);
        return Result.success(page);
    }

}
