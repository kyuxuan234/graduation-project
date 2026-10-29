package com.example.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.exception.BusinessException;
import com.example.service.PersonService;
import com.example.utils.TokenUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 登录、注册、改密。账号统一走 person 表。
 */
@RestController
public class WebController {

    @Resource
    private PersonService personService;

    @GetMapping("/")
    public Result<String> hello() {
        return Result.success("访问成功");
    }

    @PostMapping("/login")
    public Result<Account> login(@RequestBody Account account) {
        if (ObjectUtil.isEmpty(account.getUsername()) || ObjectUtil.isEmpty(account.getPassword())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        return Result.success(personService.login(account));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        personService.register(account);
        return Result.success();
    }

    @PutMapping("/updatePassword")
    public Result<Void> updatePassword(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPassword())
                || ObjectUtil.isEmpty(account.getNewPassword())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        personService.updatePassword(account);
        return Result.success();
    }

    @GetMapping("/currentUser")
    public Result<Account> currentUser() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (ObjectUtil.isEmpty(currentUser.getUsername())) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_LOGIN);
        }
        currentUser.setPassword(null);
        return Result.success(currentUser);
    }

}
