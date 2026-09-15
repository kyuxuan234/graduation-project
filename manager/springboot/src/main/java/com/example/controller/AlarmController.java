package com.example.controller;

import com.example.common.Result;
import com.example.entity.Alarm;
import com.example.service.AlarmService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

/**
 * 告警记录前端操作接口
 **/
@RestController
@RequestMapping("/alarm")
public class AlarmController {

    @Resource
    private AlarmService alarmService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Alarm alarm) {
        alarmService.add(alarm);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        alarmService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        alarmService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<Void> updateById(@RequestBody Alarm alarm) {
        alarmService.updateById(alarm);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result<Alarm> selectById(@PathVariable Integer id) {
        Alarm alarm = alarmService.selectById(id);
        return Result.success(alarm);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result<List<Alarm>> selectAll(Alarm alarm) {
        List<Alarm> list = alarmService.selectAll(alarm);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result<PageInfo<Alarm>> selectPage(Alarm alarm,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Alarm> page = alarmService.selectPage(alarm, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 接手处理
     */
    @PutMapping("/handle/{id}")
    public Result<Void> handle(@PathVariable Integer id) {
        alarmService.handle(id);
        return Result.success();
    }

    /**
     * 解决
     */
    @PutMapping("/resolve/{id}")
    public Result<Void> resolve(@PathVariable Integer id) {
        alarmService.resolve(id);
        return Result.success();
    }

}
