package com.example.entity;

import java.io.Serializable;

/**
 * 告警记录
*/
public class Alarm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 告警编号 */
    private String alarmNo;
    /** 关联应用id */
    private Integer appId;
    /** 关联服务器id */
    private Integer serverId;
    /** 关联部署单编号 */
    private String deployNo;
    /** 级别：INFO-提示 WARNING-警告 CRITICAL-严重 */
    private String level;
    /** 告警内容 */
    private String content;
    /** 状态：UNHANDLED-未处理 HANDLING-处理中 RESOLVED-已解决 */
    private String status;
    /** 处理人 */
    private String handler;
    /** 接手时间 */
    private String handleTime;
    /** 解决时间 */
    private String resolveTime;
    /** 创建时间 */
    private String createTime;
    /** 更新时间 */
    private String updateTime;
    /** 逻辑删除：0-未删除 1-已删除 */
    private Integer deleted;

    /** 联查字段：关联应用名 */
    private String appName;
    /** 联查字段：关联服务器主机名 */
    private String serverName;
    /** 联查字段：处理人角色 */
    private String handlerRole;

    /** 查询条件：告警时间范围-开始 */
    private String startTime;
    /** 查询条件：告警时间范围-结束 */
    private String endTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAlarmNo() {
        return alarmNo;
    }

    public void setAlarmNo(String alarmNo) {
        this.alarmNo = alarmNo;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public String getDeployNo() {
        return deployNo;
    }

    public void setDeployNo(String deployNo) {
        this.deployNo = deployNo;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(String handleTime) {
        this.handleTime = handleTime;
    }

    public String getResolveTime() {
        return resolveTime;
    }

    public void setResolveTime(String resolveTime) {
        this.resolveTime = resolveTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getHandlerRole() {
        return handlerRole;
    }

    public void setHandlerRole(String handlerRole) {
        this.handlerRole = handlerRole;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
