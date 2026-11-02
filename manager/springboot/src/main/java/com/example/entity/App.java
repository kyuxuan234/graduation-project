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
    /** 技术栈 */
    private String techStack;
    /** 代码仓库地址 */
    private String repoUrl;
    /** 状态：ONLINE-在线 DEPLOYING-部署中 OFFLINE-离线（兼容旧字段，实例状态以 app_instance 为准） */
    private String status;
    /** 创建时间 */
    private String createTime;
    /** 更新时间 */
    private String updateTime;
    /** 逻辑删除：0-未删除 1-已删除 */
    private Integer deleted;

    /** 联查字段：所属项目名 */
    private String projectName;
    /** 联查字段：部署服务器环境 */
    private String serverEnv;
    /** 联查字段：该应用的部署单数 */
    private Integer deployCount;
    /** 仅用于生成脚本的请求字段，不入库 */
    private String version;
    /** 仅用于按实例所在服务器筛选 / 生成脚本，不入库 */
    private Integer serverId;
    /** 在线实例数 */
    private Integer onlineCount;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public Integer getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(Integer onlineCount) {
        this.onlineCount = onlineCount;
    }

    public Integer getAlarmCount() {
        return alarmCount;
    }

    public void setAlarmCount(Integer alarmCount) {
        this.alarmCount = alarmCount;
    }
}
