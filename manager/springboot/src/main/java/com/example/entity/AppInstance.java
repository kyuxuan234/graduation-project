package com.example.entity;

import java.io.Serializable;

/** 应用在某一个环境上的部署实例。同一应用在开发、测试、生产各有一条。 */
public class AppInstance implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer appId;
    private String envCode;
    private Integer serverId;
    private Integer port;
    private String version;
    private String status;
    private String createTime;
    private String updateTime;
    private Integer deleted;

    private String appName;
    private String appCode;
    private String serverName;
    private String envName;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getAppId() { return appId; }
    public void setAppId(Integer appId) { this.appId = appId; }
    public String getEnvCode() { return envCode; }
    public void setEnvCode(String envCode) { this.envCode = envCode; }
    public Integer getServerId() { return serverId; }
    public void setServerId(Integer serverId) { this.serverId = serverId; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public String getAppCode() { return appCode; }
    public void setAppCode(String appCode) { this.appCode = appCode; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public String getEnvName() { return envName; }
    public void setEnvName(String envName) { this.envName = envName; }
}
