package com.example.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 部署单时间线：STATE 列表 + EXEC 空壳（本轮无执行引擎）
 */
public class DeployTimelineVO {

    private List<DeployTimelineStateVO> states = new ArrayList<DeployTimelineStateVO>();
    private DeployExecSummaryVO execSummary = DeployExecSummaryVO.empty();

    public List<DeployTimelineStateVO> getStates() {
        return states;
    }

    public void setStates(List<DeployTimelineStateVO> states) {
        this.states = states;
    }

    public DeployExecSummaryVO getExecSummary() {
        return execSummary;
    }

    public void setExecSummary(DeployExecSummaryVO execSummary) {
        this.execSummary = execSummary;
    }
}
