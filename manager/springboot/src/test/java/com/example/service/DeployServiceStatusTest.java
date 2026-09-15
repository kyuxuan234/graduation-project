package com.example.service;

import com.example.common.enums.AppStatusEnum;
import com.example.common.enums.DeployActionEnum;
import com.example.common.enums.DeployStatusEnum;
import com.example.common.enums.DeployTypeEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.dto.DeployTimelineVO;
import com.example.entity.Account;
import com.example.entity.App;
import com.example.entity.Deploy;
import com.example.entity.DeployLog;
import com.example.exception.BusinessException;
import com.example.mapper.AppMapper;
import com.example.mapper.DeployLogMapper;
import com.example.mapper.DeployMapper;
import com.example.utils.TokenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 部署状态一致性与回滚版本语义回归。用 Mockito 避免污染本地库。
 */
@ExtendWith(MockitoExtension.class)
class DeployServiceStatusTest {

    @Mock
    private DeployMapper deployMapper;
    @Mock
    private DeployLogMapper deployLogMapper;
    @Mock
    private AppMapper appMapper;

    @InjectMocks
    private DeployService deployService;

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
    void startDeploy_setsAppDeploying_withoutWritingVersion() {
        Deploy db = deploy(1, DeployStatusEnum.APPROVED, "v2.0.0", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);
        whenCasOk();

        deployService.startDeploy(1);

        App app = captureAppUpdate();
        assertEquals(Integer.valueOf(10), app.getId());
        assertEquals(AppStatusEnum.DEPLOYING.name(), app.getStatus());
        assertNull(app.getVersion());
        verify(deployMapper).casUpdateStatus(eq(1), eq(DeployStatusEnum.APPROVED.name()),
                eq(DeployStatusEnum.DEPLOYING.name()), eq(0), any(Deploy.class), eq(false));
    }

    @Test
    void finishDeploy_success_setsOnlineAndTargetVersion() {
        Deploy db = deploy(1, DeployStatusEnum.DEPLOYING, "v2.0.0", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);
        whenCasOk();

        deployService.finishDeploy(1, DeployStatusEnum.SUCCESS.name());

        App app = captureAppUpdate();
        assertEquals(AppStatusEnum.ONLINE.name(), app.getStatus());
        assertEquals("v2.0.0", app.getVersion());
    }

    @Test
    void finishDeploy_failed_setsOffline_withoutWritingVersion() {
        Deploy db = deploy(1, DeployStatusEnum.DEPLOYING, "v2.0.0", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);
        whenCasOk();

        deployService.finishDeploy(1, DeployStatusEnum.FAILED.name());

        App app = captureAppUpdate();
        assertEquals(AppStatusEnum.OFFLINE.name(), app.getStatus());
        assertNull(app.getVersion());
    }

    @Test
    void finishDeploy_fromPending_throws() {
        Deploy db = deploy(1, DeployStatusEnum.PENDING, "v2.0.0", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deployService.finishDeploy(1, DeployStatusEnum.SUCCESS.name()));
        assertEquals(ResultCodeEnum.DEPLOY_STATUS_ERROR.code, ex.getCode());
        verify(deployMapper, never()).casUpdateStatus(anyInt(), anyString(), anyString(), anyInt(), any(), anyBoolean());
        verify(appMapper, never()).updateById(any(App.class));
    }

    @Test
    void finishDeploy_rollbackSuccess_writesRollbackVersion() {
        Deploy db = deploy(1, DeployStatusEnum.DEPLOYING, "v2.3.1", DeployTypeEnum.ROLLBACK);
        db.setRollbackVersion("v2.3.0");
        when(deployMapper.selectById(1)).thenReturn(db);
        whenCasOk();

        deployService.finishDeploy(1, DeployStatusEnum.SUCCESS.name());

        App app = captureAppUpdate();
        assertEquals(AppStatusEnum.ONLINE.name(), app.getStatus());
        assertEquals("v2.3.0", app.getVersion());
        DeployLog log = captureLogInsert();
        assertEquals(DeployActionEnum.DEPLOY_SUCCESS.name(), log.getAction());
        assertEquals("v2.3.0", log.getToVersion());
    }

    @Test
    void rollback_requiresHistoricalSuccessVersion() {
        Deploy db = deploy(1, DeployStatusEnum.SUCCESS, "v2.3.1", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);
        when(deployMapper.countSuccessVersion(10, "v9.9.9", 1)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deployService.rollback(1, "v9.9.9"));
        assertEquals(ResultCodeEnum.ROLLBACK_VERSION_NOT_FOUND_ERROR.code, ex.getCode());
        verify(deployMapper, never()).casUpdateStatus(anyInt(), anyString(), anyString(), anyInt(), any(), anyBoolean());
    }

    @Test
    void rollback_blankVersion_throws() {
        Deploy db = deploy(1, DeployStatusEnum.SUCCESS, "v2.3.1", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deployService.rollback(1, "  "));
        assertEquals(ResultCodeEnum.ROLLBACK_VERSION_REQUIRED_ERROR.code, ex.getCode());
    }

    @Test
    void rollback_setsDeploying_andRecordsFromToVersion() {
        Deploy db = deploy(1, DeployStatusEnum.SUCCESS, "v2.3.1", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);
        when(deployMapper.countSuccessVersion(10, "v2.3.0", 1)).thenReturn(1);
        App app = new App();
        app.setId(10);
        app.setVersion("v2.3.1");
        app.setStatus(AppStatusEnum.ONLINE.name());
        when(appMapper.selectById(10)).thenReturn(app);
        whenCasOk();

        deployService.rollback(1, "v2.3.0");

        App updated = captureAppUpdate();
        assertEquals(AppStatusEnum.DEPLOYING.name(), updated.getStatus());
        assertNull(updated.getVersion());
        DeployLog log = captureLogInsert();
        assertEquals(DeployActionEnum.ROLLBACK.name(), log.getAction());
        assertEquals("v2.3.1", log.getFromVersion());
        assertEquals("v2.3.0", log.getToVersion());
        verify(deployMapper).casUpdateStatus(eq(1), eq(DeployStatusEnum.SUCCESS.name()),
                eq(DeployStatusEnum.DEPLOYING.name()), eq(0), any(Deploy.class), eq(true));
    }

    @Test
    void casConflict_throwsConcurrentError() {
        Deploy db = deploy(1, DeployStatusEnum.APPROVED, "v2.0.0", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);
        when(deployMapper.casUpdateStatus(anyInt(), anyString(), anyString(), anyInt(), any(), anyBoolean()))
                .thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> deployService.startDeploy(1));
        assertEquals(ResultCodeEnum.DEPLOY_CONCURRENT_ERROR.code, ex.getCode());
    }

    @Test
    void timeline_returnsStateListAndEmptyExecSummary() {
        Deploy db = deploy(1, DeployStatusEnum.SUCCESS, "v2.3.1", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);
        DeployLog log = new DeployLog();
        log.setId(7);
        log.setLogType("STATE");
        log.setAction(DeployActionEnum.ROLLBACK.name());
        log.setOperator("ops1");
        log.setDetail("回滚");
        log.setFromVersion("v2.3.1");
        log.setToVersion("v2.3.0");
        log.setCreateTime("2026-05-01 11:30:00");
        when(deployLogMapper.selectByDeployId(1)).thenReturn(Collections.singletonList(log));

        DeployTimelineVO vo = deployService.timeline(1);

        assertEquals(1, vo.getStates().size());
        assertEquals("ROLLBACK", vo.getStates().get(0).getAction());
        assertEquals("回滚", vo.getStates().get(0).getContent());
        assertEquals("v2.3.1", vo.getStates().get(0).getFromVersion());
        assertEquals("v2.3.0", vo.getStates().get(0).getToVersion());
        assertEquals("NONE", vo.getExecSummary().getStatus());
        assertEquals(0, vo.getExecSummary().getTotalLines());
    }

    @Test
    void approve_rejectWithoutRemark_throws() {
        Deploy db = deploy(1, DeployStatusEnum.PENDING, "v1.0.0", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deployService.approve(1, DeployStatusEnum.REJECTED.name(), "  "));
        assertEquals(ResultCodeEnum.REMARK_REQUIRED_ERROR.code, ex.getCode());
        verify(deployMapper, never()).casUpdateStatus(anyInt(), anyString(), anyString(), anyInt(), any(), anyBoolean());
    }

    @Test
    void approve_rejectWithRemark_writesLog() {
        Deploy db = deploy(1, DeployStatusEnum.PENDING, "v1.0.0", DeployTypeEnum.FULL);
        when(deployMapper.selectById(1)).thenReturn(db);
        whenCasOk();

        deployService.approve(1, DeployStatusEnum.REJECTED.name(), "缺少回滚方案");

        ArgumentCaptor<DeployLog> captor = ArgumentCaptor.forClass(DeployLog.class);
        verify(deployLogMapper).insert(captor.capture());
        assertEquals(DeployActionEnum.REJECT.name(), captor.getValue().getAction());
    }

    private void whenCasOk() {
        when(deployMapper.casUpdateStatus(anyInt(), anyString(), anyString(), anyInt(), any(), anyBoolean()))
                .thenReturn(1);
    }

    private Deploy deploy(Integer id, DeployStatusEnum status, String version, DeployTypeEnum type) {
        Deploy db = new Deploy();
        db.setId(id);
        db.setAppId(10);
        db.setStatus(status.name());
        db.setVersion(version);
        db.setDeployType(type.name());
        db.setRowVersion(0);
        return db;
    }

    private App captureAppUpdate() {
        ArgumentCaptor<App> captor = ArgumentCaptor.forClass(App.class);
        verify(appMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private DeployLog captureLogInsert() {
        ArgumentCaptor<DeployLog> captor = ArgumentCaptor.forClass(DeployLog.class);
        verify(deployLogMapper).insert(captor.capture());
        return captor.getValue();
    }
}
