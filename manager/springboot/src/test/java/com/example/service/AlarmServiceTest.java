package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Alarm;
import com.example.mapper.AlarmMapper;
import com.example.utils.TokenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * 登记告警会落库并通知邮件。当前告警实体没有 deployId。
 */
@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    @Mock
    private AlarmMapper alarmMapper;
    @Mock
    private AlarmMailService alarmMailService;

    @InjectMocks
    private AlarmService alarmService;

    @BeforeEach
    void loginOps() {
        Account ops = new Account();
        ops.setUsername("ops1");
        ops.setRole(RoleEnum.OPS.name());
        TokenUtils.bindCurrentUser(ops);
    }

    @AfterEach
    void clearUser() {
        TokenUtils.unbindCurrentUser();
    }

    @Test
    void add_insertsAndNotifies() {
        Alarm alarm = new Alarm();
        alarm.setServerId(1);
        alarm.setContent("CPU 过高");
        alarm.setLevel("CRITICAL");

        alarmService.add(alarm);

        verify(alarmMapper).insert(alarm);
        verify(alarmMailService).notifyNewAlarm(eq(alarm), org.mockito.ArgumentMatchers.any());
    }
}
