package com.example.entity;

import java.io.Serializable;

/**
 * 部署流转记录
*/
public class DeployLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 部署单id */
    private Integer deployId;
    /** 动作：APPLY-提交申请 APPROVE-审批通过 REJECT-审批驳回 START_DEPLOY-开始部署 FINISH-部署成功 FAIL-部署失败 ROLLBACK-回滚 */
    private String action;
    /** 操作人 */
    private String operator;
    /** 操作说明 */
    private String detail;
    /** 创建时间 */
    private String createTime;
    /** 更新时间 */
    private String updateTime;
    /** 逻辑删除：0-未删除 1-已删除 */
    private Integer deleted;
    /** STATE 或 EXEC */
    private String logType;
    /** 行序 */
    private Integer seq;
    /** MANUAL 或 AUTO */
    private String source;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDeployId() {
        return deployId;
    }

    public void setDeployId(Integer deployId) {
        this.deployId = deployId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
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

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
