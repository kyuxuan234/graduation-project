package com.example.common;

import com.example.common.enums.DeployStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.ServerEnvEnum;
import com.example.exception.BusinessException;

/**
 * 部署单状态机：只校验允许的流转，不碰数据库。
 * REVIEW → ACCEPTING / REJECTED（生产环境，技术负责人审查，通过后直接产品验收）
 * TESTING → PENDING / REJECTED（测试环境，测试验收后交运维）
 * ACCEPTING → PENDING / REJECTED（生产环境，产品验收）
 * PENDING → APPROVED / REJECTED（运维审批；开发环境提交后直接进入）
 * APPROVED → DEPLOYING
 * DEPLOYING → SUCCESS / FAILED
 * FAILED → DEPLOYING（发布失败后回滚，再走完成）
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
        assertCurrent(currentStatus, DeployStatusEnum.FAILED);
    }

    /**
     * 审查或验收通过后的下一状态。生产环境不经过测试验收。
     * 测试环境的测试验收通过后直接交给运维。
     */
    public static String passTarget(String currentStatus, String env) {
        if (DeployStatusEnum.REVIEW.name().equals(currentStatus)) {
            return DeployStatusEnum.ACCEPTING.name();
        }
        if (DeployStatusEnum.TESTING.name().equals(currentStatus)) {
            if (ServerEnvEnum.TEST.name().equals(env)) {
                return DeployStatusEnum.PENDING.name();
            }
            return DeployStatusEnum.ACCEPTING.name();
        }
        if (DeployStatusEnum.ACCEPTING.name().equals(currentStatus)) {
            return DeployStatusEnum.PENDING.name();
        }
        throw new BusinessException(ResultCodeEnum.DEPLOY_STATUS_ERROR);
    }

    public static boolean isReviewGate(String currentStatus) {
        return DeployStatusEnum.REVIEW.name().equals(currentStatus)
                || DeployStatusEnum.TESTING.name().equals(currentStatus)
                || DeployStatusEnum.ACCEPTING.name().equals(currentStatus);
    }

    private static void assertCurrent(String currentStatus, DeployStatusEnum expect) {
        if (!expect.name().equals(currentStatus)) {
            throw new BusinessException(ResultCodeEnum.DEPLOY_STATUS_ERROR);
        }
    }
}
