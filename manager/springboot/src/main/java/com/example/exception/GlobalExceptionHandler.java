package com.example.exception;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice(basePackages = "com.example.controller")
public class GlobalExceptionHandler {

    private static final Log log = LogFactory.get();

    /**
     * 系统异常处理，兜底
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result<Void> error(HttpServletRequest request, Exception e) {
        log.error(e, "未预期异常");
        return Result.error(ResultCodeEnum.SYSTEM_ERROR.code, "系统繁忙，请稍后重试");
    }

    /**
     * 业务异常处理，返回自定义错误码
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Result<Void> businessError(HttpServletRequest request, BusinessException e) {
        log.error("业务异常：[{}] {}", e.getCode(), e.getMsg());
        return Result.error(e.getCode(), e.getMsg());
    }

    /**
     * 唯一索引冲突不把库表信息返回前端
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public Result<Void> duplicateKeyError(HttpServletRequest request, DuplicateKeyException e) {
        log.error(e, "未预期异常");
        return Result.error(ResultCodeEnum.SYSTEM_ERROR.code, "系统繁忙，请稍后重试");
    }

    /**
     * 参数校验失败不把字段细节返回前端
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Result<Void> validError(HttpServletRequest request, MethodArgumentNotValidException e) {
        log.error(e, "未预期异常");
        return Result.error(ResultCodeEnum.SYSTEM_ERROR.code, "系统繁忙，请稍后重试");
    }
}
