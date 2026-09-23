package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.common.ExecLogKeywords;
import com.example.dto.FailureAnalysisVO;
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
            item.put("alarmAfterSuccess", num(pick(row, "alarmAfterSuccess", "alarmaftersuccess")));
            result.add(item);
        }
        return result;
    }

    /**
     * 近 14 天部署单数量，缺的日期补 0。首页原有 7 天接口不变。
     */
    public List<Map<String, Object>> deployTrend14() {
        return zeroFill(14, statisticsMapper.countDeployGroupByDate(startOf(14)));
    }

    /**
     * 失败次数按应用、服务器、天汇总。天数限制在 1 到 365，默认 30。
     */
    public FailureAnalysisVO failureAnalysis(Integer days) {
        int span = clampDays(days);
        String start = startOf(span);
        FailureAnalysisVO vo = new FailureAnalysisVO();
        vo.setByApp(normalizeCount(statisticsMapper.countFailedByApp(start)));
        vo.setByServer(normalizeCount(statisticsMapper.countFailedByServer(start)));
        vo.setByDay(fillOutcome(span, statisticsMapper.countFailedByDay(start)));
        return vo;
    }

    /**
     * 执行日志关键字出现次数，取前 10。
     */
    public List<Map<String, Object>> errorTop(Integer days) {
        String start = startOf(clampDays(days));
        List<Map<String, Object>> list = new ArrayList<>();
        for (String keyword : ExecLogKeywords.KEYWORDS) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("keyword", keyword);
            item.put("count", statisticsMapper.countExecKeyword(start, keyword));
            list.add(item);
        }
        list.sort((a, b) -> Integer.compare(num(b.get("count")), num(a.get("count"))));
        if (list.size() > 10) {
            return new ArrayList<>(list.subList(0, 10));
        }
        return list;
    }

    /**
     * 每日告警数，以及截至当天、最多 7 天的移动平均，保留 1 位小数。
     */
    public List<Map<String, Object>> alarmTrend(Integer days) {
        int span = clampDays(days);
        List<Map<String, Object>> daysRows = zeroFill(span, statisticsMapper.countAlarmByDay(startOf(span)));
        for (int i = 0; i < daysRows.size(); i++) {
            int from = Math.max(0, i - 6);
            double sum = 0;
            int width = i - from + 1;
            for (int j = from; j <= i; j++) {
                sum += num(daysRows.get(j).get("count"));
            }
            double avg = Math.round(sum / width * 10.0) / 10.0;
            daysRows.get(i).put("avg", avg);
        }
        return daysRows;
    }

    private int clampDays(Integer days) {
        if (days == null) {
            return 30;
        }
        if (days < 1) {
            return 1;
        }
        return Math.min(days, 365);
    }

    private String startOf(int days) {
        Date startDate = DateUtil.offsetDay(new Date(), -(days - 1));
        return DateUtil.formatDate(startDate) + " 00:00:00";
    }

    /** 按天补齐完成次数 total 和失败次数 count，没有记录的日期为 0。 */
    private List<Map<String, Object>> fillOutcome(int days, List<Map<String, Object>> dbList) {
        Date startDate = DateUtil.offsetDay(new Date(), -(days - 1));
        Map<String, int[]> db = new HashMap<>();
        for (Map<String, Object> row : dbList) {
            String date = String.valueOf(pick(row, "date"));
            db.put(date, new int[]{num(pick(row, "total")), num(pick(row, "count"))});
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String date = DateUtil.formatDate(DateUtil.offsetDay(startDate, i));
            int[] pair = db.get(date);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date);
            item.put("total", pair == null ? 0 : pair[0]);
            item.put("count", pair == null ? 0 : pair[1]);
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> zeroFill(int days, List<Map<String, Object>> dbList) {
        Date startDate = DateUtil.offsetDay(new Date(), -(days - 1));
        Map<String, Object> dbCountMap = new HashMap<>();
        for (Map<String, Object> row : dbList) {
            dbCountMap.put(String.valueOf(pick(row, "date")), pick(row, "count"));
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String date = DateUtil.formatDate(DateUtil.offsetDay(startDate, i));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date);
            item.put("count", dbCountMap.containsKey(date) ? num(dbCountMap.get(date)) : 0);
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> normalizeCount(List<Map<String, Object>> rows) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", pick(row, "id"));
            item.put("name", pick(row, "name"));
            item.put("count", num(pick(row, "count")));
            list.add(item);
        }
        return list;
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
