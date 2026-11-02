package com.example.common.enums;

public enum RoleEnum {
    // 超级管理员
    ADMIN,
    // 开发人员
    DEV,
    // 技术负责人，审查生产发布，通过后进入产品验收
    TL,
    // 测试，验收测试环境
    QA,
    // 产品经理，验收生产环境的业务结果
    PM,
    // 运维人员
    OPS,
}
