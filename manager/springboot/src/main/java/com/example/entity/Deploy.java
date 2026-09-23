package com.example.entity;

import java.io.Serializable;

/**
 * 部署单
*/
public class Deploy implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 部署单号 */
    private String deployNo;
    /** 应用id */
    private Integer appId;
    /** 目标服务器id */
    private Integer serverId;
    /** 目标版本 */
    private String version;
    /** 类型：FULL-全量 INCREMENT-增量 ROLLBACK-回滚 */
    private String deployType;
    /** 状态：PENDING-待审批 APPROVED-已批准 REJECTED-已驳回 DEPLOYING-部署中 SUCCESS-成功 FAILED-失败 */
    private String status;
    /** 申请人 */
    private String applicant;
    /** 审批人 */
    private String approver;
    /** 申请时间 */
    private String applyTime;
    /** 审批时间 */
    private String approveTime;
    /** 完成时间 */
    private String finishTime;
    /** 申请原因 */
    private String reason;
    /** 备注 */
    private String remark;
    /** 回滚目标版本 */
    private String rollbackVersion;
    /** 乐观锁 */
    private Integer rowVersion;
    /** 创建时间 */
    private String createTime;
    /** 更新时间 */
    private String updateTime;
    /** 逻辑删除：0-未删除 1-已删除 */
    private Integer deleted;

    /** 联查字段：应用名 */
    private String appName;
    /** 联查字段：应用编号 */
    private String appCode;
    /** 联查字段：目标服务器主机名 */
    private String serverName;
    /** 联查字段：申请人角色 */
    private String applicantRole;
    /** 联查字段：审批人角色 */
    private String approverRole;

    /** 查询条件：申请时间范围-开始 */
    private String startTime;
    /** 查询条件：申请时间范围-结束 */
    private String endTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDeployNo() {
        return deployNo;
    }

    public void setDeployNo(String deployNo) {
        this.deployNo = deployNo;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDeployType() {
        return deployType;
    }

    public void setDeployType(String deployType) {
        this.deployType = deployType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(String applyTime) {
        this.applyTime = applyTime;
    }

    public String getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(String approveTime) {
        this.approveTime = approveTime;
    }

    public String getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(String finishTime) {
        this.finishTime = finishTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getApplicantRole() {
        return applicantRole;
    }

    public void setApplicantRole(String applicantRole) {
        this.applicantRole = applicantRole;
    }

    public String getApproverRole() {
        return approverRole;
    }

    public void setApproverRole(String approverRole) {
        this.approverRole = approverRole;
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

    public String getRollbackVersion() {
        return rollbackVersion;
    }

    public void setRollbackVersion(String rollbackVersion) {
        this.rollbackVersion = rollbackVersion;
    }

    public Integer getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(Integer rowVersion) {
        this.rowVersion = rowVersion;
    }
}
