package com.example.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行日志摘要。本轮恒空，字段结构先定死，接执行引擎时只填数据。
 */
public class DeployExecSummaryVO {

    private int totalLines;
    private String status;
    private List<Object> head = new ArrayList<Object>();
    private List<Object> tail = new ArrayList<Object>();

    public static DeployExecSummaryVO empty() {
        DeployExecSummaryVO vo = new DeployExecSummaryVO();
        vo.setTotalLines(0);
        vo.setStatus("NONE");
        return vo;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Object> getHead() {
        return head;
    }

    public void setHead(List<Object> head) {
        this.head = head;
    }

    public List<Object> getTail() {
        return tail;
    }

    public void setTail(List<Object> tail) {
        this.tail = tail;
    }
}
