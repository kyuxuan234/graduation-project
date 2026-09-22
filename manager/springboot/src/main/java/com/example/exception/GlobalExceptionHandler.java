package com.example.exception;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
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
     * 唯一索引冲突兜底处理，避免直接向前端抛出系统异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public Result<Void> duplicateKeyError(HttpServletRequest request, DuplicateKeyException e) {
        log.error("唯一索引冲突：", e);
        return Result.error(ResultCodeEnum.DATA_EXIST_ERROR);
    }

    /**
     * 参数校验异常处理，取第一条校验失败信息返回
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Result<Void> validError(HttpServletRequest request, MethodArgumentNotValidException e) {
        String msg = ResultCodeEnum.PARAM_ERROR.msg;
        BindingResult bindingResult = e.getBindingResult();
        if (bindingResult.hasFieldErrors()) {
            FieldError fieldError = bindingResult.getFieldErrors().get(0);
            msg = fieldError.getField() + " " + fieldError.getDefaultMessage();
        }
        log.error("参数校验异常：{}", msg);
        return Result.error(ResultCodeEnum.PARAM_ERROR.code, msg);
    }
}
