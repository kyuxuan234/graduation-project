package com.example.common;

import com.example.common.enums.DeployStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.exception.BusinessException;

/**
 * 部署单状态机：只校验允许的流转，不碰数据库。
 * PENDING → APPROVED / REJECTED
 * APPROVED → DEPLOYING
 * DEPLOYING → SUCCESS / FAILED
 * SUCCESS → DEPLOYING（回滚后再走完成）
 */
public final class DeployStatusMachine {

    private DeployStatusMachine() {
    }

    public static void assertApprove(String currentStatus, String result) {
        if (!DeployStatusEnum.PENDING.name().equals(currentStatus)) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_NOT_PENDING_ERROR);
        }
        if (!DeployStatusEnum.APPROVED.name().equals(result)
                && !DeployStatusEnum.REJECTED.name().equals(result)) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_STATUS_ERROR);
        }
    }

    public static void assertStart(String currentStatus) {
        assertCurrent(currentStatus, DeployStatusEnum.APPROVED);
    }

    public static void assertFinish(String currentStatus, String result) {
        assertCurrent(currentStatus, DeployStatusEnum.DEPLOYING);
        if (!DeployStatusEnum.SUCCESS.name().equals(result)
                && !DeployStatusEnum.FAILED.name().equals(result)) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_STATUS_ERROR);
        }
    }

    public static void assertRollback(String currentStatus) {
        assertCurrent(currentStatus, DeployStatusEnum.SUCCESS);
    }

    private static void assertCurrent(String currentStatus, DeployStatusEnum expect) {
        if (!expect.name().equals(currentStatus)) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_STATUS_ERROR);
        }
    }
}
