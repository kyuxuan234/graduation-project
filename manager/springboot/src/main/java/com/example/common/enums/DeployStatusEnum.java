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
    // 待执行
    APPROVED,
    // 已驳回
    REJECTED,
    // 发布中
    DEPLOYING,
    // 发布成功
    SUCCESS,
    // 发布失败
    FAILED,
}
