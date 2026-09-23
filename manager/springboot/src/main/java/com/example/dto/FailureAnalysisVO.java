package com.example.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 失败分布：按应用、按服务器、按天。
 */
public class FailureAnalysisVO {

    private List<Map<String, Object>> byApp = new ArrayList<Map<String, Object>>();
    private List<Map<String, Object>> byServer = new ArrayList<Map<String, Object>>();
    private List<Map<String, Object>> byDay = new ArrayList<Map<String, Object>>();

    public List<Map<String, Object>> getByApp() {
        return byApp;
    }

    public void setByApp(List<Map<String, Object>> byApp) {
        this.byApp = byApp;
    }

    public List<Map<String, Object>> getByServer() {
        return byServer;
    }

    public void setByServer(List<Map<String, Object>> byServer) {
        this.byServer = byServer;
    }

    public List<Map<String, Object>> getByDay() {
        return byDay;
    }

    public void setByDay(List<Map<String, Object>> byDay) {
        this.byDay = byDay;
    }
}
