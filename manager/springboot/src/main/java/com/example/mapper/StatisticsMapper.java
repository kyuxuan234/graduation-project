package com.example.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 统计相关数据接口
*/
public interface StatisticsMapper {

    /**
      * 项目总数
    */
    int countProject();

    /**
      * 服务器总数
    */
    int countServer();

    /**
      * 应用总数
    */
    int countApp();

    /**
      * 在线应用数
    */
    int countOnlineApp();

    /**
      * 部署单总数
    */
    int countDeploy();

    /**
      * 待审批部署数
    */
    int countPendingDeploy();

    /**
      * 未处理告警数
    */
    int countUnhandledAlarm();

    /**
      * 指定起始日期之后，按天统计部署单数量
    */
    List<Map<String, Object>> countDeployGroupByDate(@Param("startDate") String startDate);

    /**
      * 按级别统计告警数量
    */
    List<Map<String, Object>> countAlarmGroupByLevel();

    /**
      * 按环境统计服务器数量
    */
    List<Map<String, Object>> countServerGroupByEnv();

    /**
      * 按状态统计部署单数量
    */
    List<Map<String, Object>> countDeployGroupByStatus();

    /**
      * 按状态统计告警数量
    */
    List<Map<String, Object>> countAlarmGroupByStatus();

    /**
      * 各应用的部署单数与告警数，用于对照变更和风险
    */
    List<Map<String, Object>> countAppDeployAndAlarm();

    List<Map<String, Object>> countFailedByApp(@Param("startDate") String startDate);

    List<Map<String, Object>> countFailedByServer(@Param("startDate") String startDate);

    List<Map<String, Object>> countFailedByDay(@Param("startDate") String startDate);

    int countExecKeyword(@Param("startDate") String startDate, @Param("keyword") String keyword);

    List<Map<String, Object>> countAlarmByDay(@Param("startDate") String startDate);

}
