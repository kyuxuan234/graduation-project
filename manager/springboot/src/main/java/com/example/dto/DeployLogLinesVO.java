package com.example.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行日志分页。cursor 是已读到的最大 seq。
 */
public class DeployLogLinesVO {

    private List<String> lines = new ArrayList<String>();
    private boolean hasMore;
    private int nextCursor;

    public static DeployLogLinesVO empty(int cursor) {
        DeployLogLinesVO vo = new DeployLogLinesVO();
        vo.setHasMore(false);
        vo.setNextCursor(cursor);
        return vo;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public int getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(int nextCursor) {
        this.nextCursor = nextCursor;
    }
}
