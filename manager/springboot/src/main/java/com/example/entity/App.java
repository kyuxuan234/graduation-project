package com.example.entity;

import java.io.Serializable;

/**
 * 应用
*/
public class App implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 应用名 */
    private String name;
    /** 应用编号 */
    private String code;
    /** 所属项目id */
    private Integer projectId;
    /** 部署服务器id */
    private Integer serverId;
    /** 端口 */
    private Integer port;
    /** 当前版本 */
    private String version;
    /** 技术栈 */
    private String techStack;
    /** 代码仓库地址 */
    private String repoUrl;
    /** 状态：ONLINE-在线 DEPLOYING-部署中 OFFLINE-离线 */
    private String status;
    /** 创建时间 */
    private String createTime;
    /** 更新时间 */
    private String updateTime;
    /** 逻辑删除：0-未删除 1-已删除 */
    private Integer deleted;

    /** 联查字段：所属项目名 */
    private String projectName;
    /** 联查字段：部署服务器主机名 */
    private String serverName;
    /** 联查字段：部署服务器环境 */
    private String serverEnv;
    /** 联查字段：该应用的部署单数 */
    private Integer deployCount;
    /** 联查字段：该应用的告警数 */
    private Integer alarmCount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerEnv() {
        return serverEnv;
    }

    public void setServerEnv(String serverEnv) {
        this.serverEnv = serverEnv;
    }

    public Integer getDeployCount() {
        return deployCount;
    }

    public void setDeployCount(Integer deployCount) {
        this.deployCount = deployCount;
    }

    public Integer getAlarmCount() {
        return alarmCount;
    }

    public void setAlarmCount(Integer alarmCount) {
        this.alarmCount = alarmCount;
    }
}
