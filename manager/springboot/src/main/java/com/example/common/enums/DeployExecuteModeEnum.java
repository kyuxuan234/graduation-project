package com.example.common.enums;

public enum DeployExecuteModeEnum {
    /** 批准后通过 SSH 自动执行 */
    AUTO,
    /** 批准后只进入部署中，由运维在页面确认结果 */
    MANUAL
}
