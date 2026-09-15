package com.example.service;

import com.example.entity.Alarm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlarmMailServiceTest {

    @Test
    void subjectAndBody_includeLevelAndAlarmNo() {
        Alarm alarm = new Alarm();
        alarm.setAlarmNo("A202609150001");
        alarm.setLevel("CRITICAL");
        alarm.setAppName("订单服务");
        alarm.setServerName("prod-01");
        alarm.setDeployNo("DP-001");
        alarm.setContent("CPU 持续超过 90%");
        alarm.setCreateTime("2026-09-15 11:40:00");

        String subject = AlarmMailService.subject(alarm);
        String body = AlarmMailService.body(alarm);

        assertEquals("【DevOps 告警】严重 订单服务", subject);
        assertTrue(body.contains("A202609150001"));
        assertTrue(body.contains("严重"));
        assertTrue(body.contains("订单服务"));
        assertTrue(body.contains("CPU 持续超过 90%"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("发件人"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("收件人"));
    }
}
