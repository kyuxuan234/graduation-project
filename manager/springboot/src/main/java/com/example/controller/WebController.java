package com.example.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.exception.BusinessException;
import com.example.service.AdminService;
import com.example.service.DevService;
import com.example.service.OpsService;
import com.example.utils.TokenUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 基础前端接口
 */
@RestController
public class WebController {

    @Resource
    private AdminService adminService;

    @Resource
    private DevService devService;

    @Resource
    private OpsService opsService;

    @GetMapping("/")
    public Result<String> hello() {
        return Result.success("访问成功");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Account> login(@RequestBody Account account) {
        if (ObjectUtil.isEmpty(account.getUsername()) || ObjectUtil.isEmpty(account.getPassword())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        // 未传角色时按用户名自动识别，兼容旧前端仍带 role 的请求
        String role = account.getRole();
        if (ObjectUtil.isEmpty(role)) {
            role = resolveLoginRole(account.getUsername());
            account.setRole(role);
        }
        if (RoleEnum.ADMIN.name().equals(role)) {
            account = adminService.login(account);
        } else if (RoleEnum.DEV.name().equals(role)) {
            account = devService.login(account);
        } else if (RoleEnum.OPS.name().equals(role)) {
            account = opsService.login(account);
        } else {
            throw new BusinessException(ResultCodeEnum.ROLE_NOT_SUPPORT_ERROR);
        }
        return Result.success(account);
    }

    /**
     * 按用户名依次在管理员、开发、运维表中查找登录角色
     */
    private String resolveLoginRole(String username) {
        if (ObjectUtil.isNotNull(adminService.selectByUsername(username))) {
            return RoleEnum.ADMIN.name();
        }
        if (ObjectUtil.isNotNull(devService.selectByUsername(username))) {
            return RoleEnum.DEV.name();
        }
        if (ObjectUtil.isNotNull(opsService.selectByUsername(username))) {
            return RoleEnum.OPS.name();
        }
        throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.register(account);
        } else if (RoleEnum.DEV.name().equals(account.getRole())) {
            devService.register(account);
        } else if (RoleEnum.OPS.name().equals(account.getRole())) {
            opsService.register(account);
        } else {
            throw new BusinessException(ResultCodeEnum.ROLE_NOT_SUPPORT_ERROR);
        }
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/updatePassword")
    public Result<Void> updatePassword(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPassword())
                || ObjectUtil.isEmpty(account.getNewPassword())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.updatePassword(account);
        } else if (RoleEnum.DEV.name().equals(account.getRole())) {
            devService.updatePassword(account);
        } else if (RoleEnum.OPS.name().equals(account.getRole())) {
            opsService.updatePassword(account);
        } else {
            throw new BusinessException(ResultCodeEnum.ROLE_NOT_SUPPORT_ERROR);
        }
        return Result.success();
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/currentUser")
    public Result<Account> currentUser() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (ObjectUtil.isEmpty(currentUser.getUsername())) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_LOGIN);
        }
        // 密码密文不返回给前端
        currentUser.setPassword(null);
        return Result.success(currentUser);
    }

}
