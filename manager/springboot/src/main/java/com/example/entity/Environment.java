package com.example.entity;

import java.io.Serializable;

/** 运行环境。开发、测试、生产各一条，下面挂多台服务器。 */
public class Environment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String code;
    private String name;
    private Integer sortNo;
    private String remark;
    private String createTime;
    private String updateTime;
    private Integer deleted;
    private Integer serverCount;
    private Integer instanceCount;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Integer getServerCount() { return serverCount; }
    public void setServerCount(Integer serverCount) { this.serverCount = serverCount; }
    public Integer getInstanceCount() { return instanceCount; }
    public void setInstanceCount(Integer instanceCount) { this.instanceCount = instanceCount; }
}
