package com.example.common.enums;

public enum DeployStatusEnum {
    // 待技术审查
    REVIEW,
    // 待测试验收
    TESTING,
    // 待产品验收
    ACCEPTING,
    // 待运维审批
    PENDING,
    // 已批准
    APPROVED,
    // 已驳回
    REJECTED,
    // 部署中
    DEPLOYING,
    // 成功
    SUCCESS,
    // 失败
    FAILED,
}
