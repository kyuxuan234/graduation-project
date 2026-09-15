package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.mapper.StatisticsMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页统计业务处理
 **/
@Service
public class StatisticsService {

    /** 部署趋势统计天数 */
    private static final int TREND_DAYS = 7;

    @Resource
    private StatisticsMapper statisticsMapper;

    /**
     * 首页汇总数量
     */
    public Map<String, Object> count() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("projectCount", statisticsMapper.countProject());
        map.put("serverCount", statisticsMapper.countServer());
        map.put("appCount", statisticsMapper.countApp());
        map.put("onlineAppCount", statisticsMapper.countOnlineApp());
        map.put("deployCount", statisticsMapper.countDeploy());
        map.put("pendingDeployCount", statisticsMapper.countPendingDeploy());
        map.put("unhandledAlarmCount", statisticsMapper.countUnhandledAlarm());
        return map;
    }

    /**
     * 近7天每日部署单数量，没有数据的日期补0
     */
    public List<Map<String, Object>> deployTrend() {
        Date startDate = DateUtil.offsetDay(new Date(), -(TREND_DAYS - 1));
        List<Map<String, Object>> dbList =
                statisticsMapper.countDeployGroupByDate(DateUtil.formatDate(startDate) + " 00:00:00");

        Map<String, Object> dbCountMap = new HashMap<>();
        for (Map<String, Object> row : dbList) {
            dbCountMap.put(String.valueOf(row.get("date")), row.get("count"));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            String date = DateUtil.formatDate(DateUtil.offsetDay(startDate, i));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date);
            item.put("count", dbCountMap.containsKey(date) ? dbCountMap.get(date) : 0);
            list.add(item);
        }
        return list;
    }

    /**
     * 各告警级别数量分布
     */
    public List<Map<String, Object>> alarmLevel() {
        return statisticsMapper.countAlarmGroupByLevel();
    }

    /**
     * 各环境服务器数量分布
     */
    public List<Map<String, Object>> serverEnv() {
        return statisticsMapper.countServerGroupByEnv();
    }

    /**
     * 部署单按状态补齐，便于看交付卡在哪一环
     */
    public List<Map<String, Object>> deployStatus() {
        return fillStatus(statisticsMapper.countDeployGroupByStatus(),
                new String[]{"PENDING", "APPROVED", "DEPLOYING", "SUCCESS", "FAILED", "REJECTED"});
    }

    /**
     * 告警按处置状态补齐，便于看异常是否闭环
     */
    public List<Map<String, Object>> alarmStatus() {
        return fillStatus(statisticsMapper.countAlarmGroupByStatus(),
                new String[]{"UNHANDLED", "HANDLING", "RESOLVED"});
    }

    /**
     * 各应用部署次数与告警次数对照，统一字段名避免驱动把别名改成小写
     */
    public List<Map<String, Object>> appRisk() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : statisticsMapper.countAppDeployAndAlarm()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", pick(row, "id"));
            item.put("name", pick(row, "name"));
            item.put("deployCount", num(pick(row, "deployCount", "deploycount")));
            item.put("alarmCount", num(pick(row, "alarmCount", "alarmcount")));
            item.put("openAlarmCount", num(pick(row, "openAlarmCount", "openalarmcount")));
            item.put("criticalOpenCount", num(pick(row, "criticalOpenCount", "criticalopencount")));
            item.put("failedDeployCount", num(pick(row, "failedDeployCount", "faileddeploycount")));
            result.add(item);
        }
        return result;
    }

    private Object pick(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key);
            }
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key) && entry.getValue() != null) {
                    return entry.getValue();
                }
            }
        }
        return 0;
    }

    private int num(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<Map<String, Object>> fillStatus(List<Map<String, Object>> dbList, String[] statuses) {
        Map<String, Object> dbCountMap = new HashMap<>();
        for (Map<String, Object> row : dbList) {
            dbCountMap.put(String.valueOf(pick(row, "status")), pick(row, "count"));
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (String status : statuses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", status);
            item.put("count", dbCountMap.containsKey(status) ? dbCountMap.get(status) : 0);
            list.add(item);
        }
        return list;
    }

}
