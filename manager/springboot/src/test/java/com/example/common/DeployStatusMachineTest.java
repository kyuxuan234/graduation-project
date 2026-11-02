package com.example.common;

import com.example.common.enums.DeployStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.ServerEnvEnum;
import com.example.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeployStatusMachineTest {

    @Test
    void approveFromPending() {
        DeployStatusMachine.assertApprove(DeployStatusEnum.PENDING.name(), DeployStatusEnum.APPROVED.name());
        DeployStatusMachine.assertApprove(DeployStatusEnum.PENDING.name(), DeployStatusEnum.REJECTED.name());
    }

    @Test
    void approveNotPending() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DeployStatusMachine.assertApprove(DeployStatusEnum.APPROVED.name(), DeployStatusEnum.APPROVED.name()));
        assertEquals(ResultCodeEnum.DEPLOY_NOT_PENDING_ERROR.code, ex.getCode());
    }

    @Test
    void approveIllegalResult() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DeployStatusMachine.assertApprove(DeployStatusEnum.PENDING.name(), DeployStatusEnum.SUCCESS.name()));
        assertEquals(ResultCodeEnum.DEPLOY_STATUS_ERROR.code, ex.getCode());
    }

    @Test
    void startOnlyFromApproved() {
        DeployStatusMachine.assertStart(DeployStatusEnum.APPROVED.name());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DeployStatusMachine.assertStart(DeployStatusEnum.PENDING.name()));
        assertEquals(ResultCodeEnum.DEPLOY_STATUS_ERROR.code, ex.getCode());
    }

    @Test
    void finishOnlyFromDeploying() {
        DeployStatusMachine.assertFinish(DeployStatusEnum.DEPLOYING.name(), DeployStatusEnum.SUCCESS.name());
        DeployStatusMachine.assertFinish(DeployStatusEnum.DEPLOYING.name(), DeployStatusEnum.FAILED.name());
        BusinessException illegalFrom = assertThrows(BusinessException.class,
                () -> DeployStatusMachine.assertFinish(DeployStatusEnum.APPROVED.name(), DeployStatusEnum.SUCCESS.name()));
        assertEquals(ResultCodeEnum.DEPLOY_STATUS_ERROR.code, illegalFrom.getCode());
        BusinessException illegalResult = assertThrows(BusinessException.class,
                () -> DeployStatusMachine.assertFinish(DeployStatusEnum.DEPLOYING.name(), DeployStatusEnum.APPROVED.name()));
        assertEquals(ResultCodeEnum.DEPLOY_STATUS_ERROR.code, illegalResult.getCode());
    }

    @Test
    void reviewGatesAdvanceInOrder() {
        assertEquals(DeployStatusEnum.ACCEPTING.name(),
                DeployStatusMachine.passTarget(DeployStatusEnum.REVIEW.name(), ServerEnvEnum.PROD.name()));
        assertEquals(DeployStatusEnum.ACCEPTING.name(),
                DeployStatusMachine.passTarget(DeployStatusEnum.TESTING.name(), ServerEnvEnum.PROD.name()));
        assertEquals(DeployStatusEnum.PENDING.name(),
                DeployStatusMachine.passTarget(DeployStatusEnum.TESTING.name(), ServerEnvEnum.TEST.name()));
        assertEquals(DeployStatusEnum.PENDING.name(),
                DeployStatusMachine.passTarget(DeployStatusEnum.ACCEPTING.name(), ServerEnvEnum.PROD.name()));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DeployStatusMachine.passTarget(DeployStatusEnum.PENDING.name(), ServerEnvEnum.PROD.name()));
        assertEquals(ResultCodeEnum.DEPLOY_STATUS_ERROR.code, ex.getCode());
    }

    @Test
    void rollbackOnlyFromFailed() {
        DeployStatusMachine.assertRollback(DeployStatusEnum.FAILED.name());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DeployStatusMachine.assertRollback(DeployStatusEnum.SUCCESS.name()));
        assertEquals(ResultCodeEnum.DEPLOY_STATUS_ERROR.code, ex.getCode());
    }
}
