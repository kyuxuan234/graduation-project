package com.example.entity;

import java.io.Serializable;

/**
 * 项目
*/
public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 项目名 */
    private String name;
    /** 项目编号 */
    private String code;
    /** 技术栈 */
    private String techStack;
    /** 开发负责人，存开发人员用户名 */
    private String leader;
    /** 状态：NOT_STARTED-未开始 IN_PROGRESS-进行中 PAUSED-已暂停 FINISHED-已完结 */
    private String status;
    /** 开始日期 */
    private String startDate;
    /** 结束日期 */
    private String endDate;
    /** 项目描述 */
    private String description;
    /** 创建时间 */
    private String createTime;
    /** 更新时间 */
    private String updateTime;
    /** 逻辑删除：0-未删除 1-已删除 */
    private Integer deleted;

    /** 联查字段：该项目下的应用数 */
    private Integer appCount;
    /** 联查字段：开发负责人角色 */
    private String leaderRole;

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

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getAppCount() {
        return appCount;
    }

    public void setAppCount(Integer appCount) {
        this.appCount = appCount;
    }

    public String getLeaderRole() {
        return leaderRole;
    }

    public void setLeaderRole(String leaderRole) {
        this.leaderRole = leaderRole;
    }
}
