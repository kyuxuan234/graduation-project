package com.example.controller;

import com.example.common.Result;
import com.example.dto.FailureAnalysisVO;
import com.example.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 首页统计前端操作接口
 **/
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    /**
     * 各项汇总数量
     */
    @GetMapping("/count")
    public Result<Map<String, Object>> count() {
        return Result.success(statisticsService.count());
    }

    /**
     * 近7天每日部署单数量
     */
    @GetMapping("/deployTrend")
    public Result<List<Map<String, Object>>> deployTrend() {
        return Result.success(statisticsService.deployTrend());
    }

    /**
     * 各告警级别数量分布
     */
    @GetMapping("/alarmLevel")
    public Result<List<Map<String, Object>>> alarmLevel() {
        return Result.success(statisticsService.alarmLevel());
    }

    /**
     * 各环境服务器数量分布
     */
    @GetMapping("/serverEnv")
    public Result<List<Map<String, Object>>> serverEnv() {
        return Result.success(statisticsService.serverEnv());
    }

    /**
     * 部署单按状态分布
     */
    @GetMapping("/deployStatus")
    public Result<List<Map<String, Object>>> deployStatus() {
        return Result.success(statisticsService.deployStatus());
    }

    /**
     * 告警按处置状态分布
     */
    @GetMapping("/alarmStatus")
    public Result<List<Map<String, Object>>> alarmStatus() {
        return Result.success(statisticsService.alarmStatus());
    }

    /**
     * 各应用部署次数与告警次数
     */
    @GetMapping("/appRisk")
    public Result<List<Map<String, Object>>> appRisk() {
        return Result.success(statisticsService.appRisk());
    }

    /**
     * 近 14 天部署趋势
     */
    @GetMapping("/deployTrend14")
    public Result<List<Map<String, Object>>> deployTrend14() {
        return Result.success(statisticsService.deployTrend14());
    }

    /**
     * 失败分布。days 默认 30，范围 1 到 365
     */
    @GetMapping("/failure")
    public Result<FailureAnalysisVO> failure(@RequestParam(required = false) Integer days) {
        return Result.success(statisticsService.failureAnalysis(days));
    }

    /**
     * 执行日志错误关键字前 10
     */
    @GetMapping("/errorTop")
    public Result<List<Map<String, Object>>> errorTop(@RequestParam(required = false) Integer days) {
        return Result.success(statisticsService.errorTop(days));
    }

    /**
     * 告警按天数量和 7 日移动平均
     */
    @GetMapping("/alarmTrend")
    public Result<List<Map<String, Object>>> alarmTrend(@RequestParam(required = false) Integer days) {
        return Result.success(statisticsService.alarmTrend(days));
    }

}
