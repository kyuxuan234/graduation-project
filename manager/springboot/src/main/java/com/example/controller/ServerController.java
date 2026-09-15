package com.example.controller;

import com.example.common.Result;
import com.example.entity.Server;
import com.example.service.ServerService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * 服务器前端操作接口
 **/
@RestController
@RequestMapping("/server")
public class ServerController {

    @Resource
    private ServerService serverService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Server server) {
        serverService.add(server);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        serverService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        serverService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<Void> updateById(@RequestBody Server server) {
        serverService.updateById(server);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result<Server> selectById(@PathVariable Integer id) {
        Server server = serverService.selectById(id);
        return Result.success(server);
    }

    /**
     * 查询所有未停机的服务器，给下拉框用
     */
    @GetMapping("/selectAll")
    public Result<List<Server>> selectAll() {
        List<Server> list = serverService.selectAll();
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result<PageInfo<Server>> selectPage(Server server,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Server> page = serverService.selectPage(server, pageNum, pageSize);
        return Result.success(page);
    }

}
