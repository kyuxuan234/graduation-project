package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Alarm;
import com.example.entity.Deploy;
import com.example.exception.BusinessException;
import com.example.mapper.AlarmMapper;
import com.example.mapper.DeployMapper;
import com.example.utils.TokenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 告警关联部署单：登记时校验部署单存在。
 */
@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    @Mock
    private AlarmMapper alarmMapper;
    @Mock
    private DeployMapper deployMapper;
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
    void add_invalidDeployId_throws() {
        Alarm alarm = new Alarm();
        alarm.setContent("CPU 过高");
        alarm.setLevel("CRITICAL");
        alarm.setDeployId(99);
        when(deployMapper.selectById(99)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> alarmService.add(alarm));
        assertEquals(ResultCodeEnum.DEPLOY_NOT_EXIST_ERROR.code, ex.getCode());
        verify(alarmMapper, never()).insert(any());
    }

    @Test
    void add_validDeployId_fillsAppAndServer() {
        Deploy deploy = new Deploy();
        deploy.setId(8);
        deploy.setAppId(7);
        deploy.setServerId(2);
        when(deployMapper.selectById(8)).thenReturn(deploy);

        Alarm alarm = new Alarm();
        alarm.setContent("灰度流量异常");
        alarm.setLevel("WARNING");
        alarm.setDeployId(8);
        alarmService.add(alarm);

        assertEquals(Integer.valueOf(7), alarm.getAppId());
        assertEquals(Integer.valueOf(2), alarm.getServerId());
        verify(alarmMapper).insert(alarm);
        verify(alarmMailService).notifyNewAlarm(any(), any());
    }

    @Test
    void sendTestMail_ops_forbidden() {
        BusinessException ex = assertThrows(BusinessException.class, () -> alarmService.sendTestMail());
        assertEquals(ResultCodeEnum.MAIL_TEST_ADMIN_ONLY_ERROR.code, ex.getCode());
        verify(alarmMailService, never()).sendTest(any());
    }

    @Test
    void sendTestMail_admin_allowed() {
        Account admin = new Account();
        admin.setUsername("admin");
        admin.setName("超级管理员");
        admin.setRole(RoleEnum.ADMIN.name());
        TokenUtils.bindCurrentUser(admin);

        alarmService.sendTestMail();
        verify(alarmMailService).sendTest(admin);
    }
}
