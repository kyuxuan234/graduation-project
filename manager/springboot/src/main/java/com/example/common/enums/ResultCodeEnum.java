package com.example.common.enums;

public enum ResultCodeEnum {
    SUCCESS("200", "成功"),

    PARAM_ERROR("400", "参数异常"),
    TOKEN_INVALID_ERROR("401", "无效的token"),
    TOKEN_CHECK_ERROR("401", "token验证失败，请重新登录"),
    PARAM_LOST_ERROR("4001", "参数缺失"),

    SYSTEM_ERROR("500", "系统异常"),
    USER_EXIST_ERROR("5001", "用户名已存在"),
    USER_NOT_LOGIN("5002", "用户未登录"),
    USER_ACCOUNT_ERROR("5003", "账号或密码错误"),
    USER_NOT_EXIST_ERROR("5004", "用户不存在"),
    PARAM_PASSWORD_ERROR("5005", "原密码输入错误"),
    USER_DISABLED_ERROR("5006", "账号已被禁用，请联系超级管理员"),
    ROLE_NOT_SUPPORT_ERROR("5007", "不支持的角色类型"),
    NO_PERMISSION_ERROR("5008", "当前角色无权访问该功能"),
    DATA_EXIST_ERROR("5009", "数据已存在，请检查编号、IP、用户名等唯一字段"),

    PROJECT_NOT_EXIST_ERROR("6001", "项目不存在"),
    SERVER_NOT_EXIST_ERROR("6002", "服务器不存在"),
    APP_NOT_EXIST_ERROR("6003", "应用不存在"),
    APP_CODE_EXIST_ERROR("6004", "应用编码已存在"),
    DEPLOY_NOT_EXIST_ERROR("6005", "部署单不存在"),
    DEPLOY_STATUS_ERROR("6006", "非法的部署状态流转"),
    DEPLOY_NOT_PENDING_ERROR("6007", "只有待审批状态才能审批"),
    DEPLOY_APPLY_NO_PERMISSION("6008", "只有DEV角色才能发起部署申请"),
    DEPLOY_APPROVE_NO_PERMISSION("6009", "只有OPS角色才能审批部署"),
    ALARM_NOT_EXIST_ERROR("6010", "告警不存在"),
    SERVER_HAS_APPS("6011", "该服务器下仍有应用，无法删除"),
    PROJECT_HAS_APPS("6012", "该项目下仍有应用，无法删除"),
    ALARM_STATUS_ERROR("6013", "非法的告警状态流转"),
    PROJECT_CODE_EXIST_ERROR("6014", "项目编号已存在"),
    SERVER_HOSTNAME_EXIST_ERROR("6015", "服务器主机名已存在"),
    SERVER_IP_EXIST_ERROR("6016", "服务器IP已存在"),
    APP_HAS_DEPLOYS("6017", "该应用下仍有部署单，无法删除"),
    DEPLOY_EXECUTE_NO_PERMISSION("6018", "只有OPS角色才能执行部署操作"),
    DEPLOY_NOT_OWNER_ERROR("6019", "只能操作自己提交且待技术审查的发布单"),
    DEPLOY_GATE_NO_PERMISSION("6028", "当前角色不能处理这一审查或验收环节"),
    STATUS_LOCKED_ERROR("6020", "当前状态不允许该操作"),
    ALARM_HANDLE_NO_PERMISSION("6021", "只有OPS角色才能处置告警"),
    ALARM_NOT_HANDLER_ERROR("6022", "只有接手人才能标记解决"),
    FILE_TYPE_ERROR("6023", "不支持的文件类型"),
    FILE_NAME_INVALID_ERROR("6024", "非法的文件名"),
    MAIL_NOT_CONFIGURED_ERROR("6025", "邮件服务未配置"),
    MAIL_PERSON_EMAIL_MISSING("6026", "当前账号未在人员资料中配置邮箱"),
    MAIL_SEND_ERROR("6027", "邮件发送失败"),
    DEPLOY_BUSY("6035", "该应用有正在进行的部署，请等待完成"),
    DEPLOY_CONFLICT("6036", "部署状态已被其他人更新，请刷新后重试"),
    ;

    public String code;
    public String msg;

    ResultCodeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
