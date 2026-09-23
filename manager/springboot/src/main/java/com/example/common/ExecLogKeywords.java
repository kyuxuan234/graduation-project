package com.example.common;

/**
 * 执行日志里用来归类失败原因的关键字，匹配时忽略大小写。
 */
public final class ExecLogKeywords {

    public static final String[] KEYWORDS = {
            "error", "exception", "failed", "timeout", "denied", "refused"
    };

    private ExecLogKeywords() {
    }
}
