package com.example.service;

import com.example.common.enums.AppStatusEnum;
import com.example.common.enums.DeployActionEnum;
import com.example.common.enums.DeployStatusEnum;
import com.example.common.enums.DeployTypeEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 部署状态变更必须写流转日志。用 Mockito 避免污染本地库。
 * 当前日志只有 action，没有 log_type 列。
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
        lenient().when(deployMapper.casUpdateStatus(
                nullable(Integer.class), nullable(String.class), nullable(Integer.class),
                nullable(String.class), nullable(String.class), nullable(String.class)
        )).thenReturn(1);
    }

    @AfterEach
    void clearUser() {
        TokenUtils.unbindCurrentUser();
    }

    @Test
    void startDeploy_setsAppDeploying_withoutWritingVersion() {
        when(deployMapper.selectById(1)).thenReturn(deploy(1, DeployStatusEnum.APPROVED, "v2.0.0", DeployTypeEnum.FULL));

        deployService.startDeploy(1);

        App app = captureAppUpdate();
        assertEquals(Integer.valueOf(10), app.getId());
        assertEquals(AppStatusEnum.DEPLOYING.name(), app.getStatus());
        assertNull(app.getVersion());
    }

    @Test
    void finishDeploy_success_setsOnlineAndTargetVersion() {
        when(deployMapper.selectById(1)).thenReturn(deploy(1, DeployStatusEnum.DEPLOYING, "v2.0.0", DeployTypeEnum.FULL));

        deployService.finishDeploy(1, DeployStatusEnum.SUCCESS.name());

        App app = captureAppUpdate();
        assertEquals(AppStatusEnum.ONLINE.name(), app.getStatus());
        assertEquals("v2.0.0", app.getVersion());
    }

    @Test
    void finishDeploy_failed_setsOffline_withoutWritingVersion() {
        when(deployMapper.selectById(1)).thenReturn(deploy(1, DeployStatusEnum.DEPLOYING, "v2.0.0", DeployTypeEnum.FULL));

        deployService.finishDeploy(1, DeployStatusEnum.FAILED.name());

        App app = captureAppUpdate();
        assertEquals(AppStatusEnum.OFFLINE.name(), app.getStatus());
        assertNull(app.getVersion());
    }

    @Test
    void finishDeploy_fromPending_throws() {
        when(deployMapper.selectById(1)).thenReturn(deploy(1, DeployStatusEnum.PENDING, "v2.0.0", DeployTypeEnum.FULL));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> deployService.finishDeploy(1, DeployStatusEnum.SUCCESS.name()));
        assertEquals(ResultCodeEnum.DEPLOY_STATUS_ERROR.code, ex.getCode());
        verify(deployMapper, never()).updateById(any(Deploy.class));
        verify(deployMapper, never()).casUpdateStatus(
                nullable(Integer.class), nullable(String.class), nullable(Integer.class),
                nullable(String.class), nullable(String.class), nullable(String.class));
        verify(appMapper, never()).updateById(any(App.class));
        verify(deployLogMapper, never()).insert(any(DeployLog.class));
    }

    @Test
    void approve_writesApproveLogAfterStatusUpdate() {
        when(deployMapper.selectById(1)).thenReturn(deploy(1, DeployStatusEnum.PENDING, "v1.0.0", DeployTypeEnum.FULL));

        deployService.approve(1, DeployStatusEnum.APPROVED.name(), "可以发布");

        InOrder order = inOrder(deployMapper, deployLogMapper);
        order.verify(deployMapper).updateById(any(Deploy.class));
        ArgumentCaptor<DeployLog> captor = ArgumentCaptor.forClass(DeployLog.class);
        order.verify(deployLogMapper).insert(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getDeployId());
        assertEquals(DeployActionEnum.APPROVE.name(), captor.getValue().getAction());
        assertEquals("ops1", captor.getValue().getOperator());
        assertEquals("审批通过，意见：可以发布", captor.getValue().getDetail());
    }

    @Test
    void startDeploy_writesStartLogAfterStatusUpdate() {
        when(deployMapper.selectById(1)).thenReturn(deploy(1, DeployStatusEnum.APPROVED, "v2.0.0", DeployTypeEnum.FULL));

        deployService.startDeploy(1);

        InOrder order = inOrder(deployMapper, deployLogMapper);
        order.verify(deployMapper).casUpdateStatus(
                nullable(Integer.class), nullable(String.class), nullable(Integer.class),
                nullable(String.class), nullable(String.class), nullable(String.class));
        ArgumentCaptor<DeployLog> captor = ArgumentCaptor.forClass(DeployLog.class);
        order.verify(deployLogMapper).insert(captor.capture());
        assertEquals(DeployActionEnum.START_DEPLOY.name(), captor.getValue().getAction());
        assertEquals("ops1", captor.getValue().getOperator());
    }

    @Test
    void approve_rejectWithRemark_writesLog() {
        when(deployMapper.selectById(1)).thenReturn(deploy(1, DeployStatusEnum.PENDING, "v1.0.0", DeployTypeEnum.FULL));

        deployService.approve(1, DeployStatusEnum.REJECTED.name(), "缺少回滚方案");

        DeployLog log = captureLogInsert();
        assertEquals(DeployActionEnum.REJECT.name(), log.getAction());
        assertEquals("审批驳回，意见：缺少回滚方案", log.getDetail());
    }

    private Deploy deploy(Integer id, DeployStatusEnum status, String version, DeployTypeEnum type) {
        Deploy db = new Deploy();
        db.setId(id);
        db.setAppId(10);
        db.setStatus(status.name());
        db.setVersion(version);
        db.setDeployType(type.name());
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
