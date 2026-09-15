package com.example.common.enums;

public enum DeployActionEnum {
    // 提交申请
    APPLY,
    // 审批通过
    APPROVE,
    // 审批驳回
    REJECT,
    // 开始部署
    START_DEPLOY,
    // 部署成功
    FINISH,
    // 部署失败
    FAIL,
    // 回滚
    ROLLBACK,
}
