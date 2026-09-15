package com.example.exception;

import com.example.common.enums.ResultCodeEnum;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {
    private String code;
    private String msg;

    public BusinessException(ResultCodeEnum resultCodeEnum) {
        this.code = resultCodeEnum.code;
        this.msg = resultCodeEnum.msg;
    }

    public BusinessException(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
