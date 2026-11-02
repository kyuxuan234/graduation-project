package com.example.utils;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.exception.BusinessException;

/**
 * 角色权限校验工具类
 */
public class AuthUtils {

    private AuthUtils() {
    }

    /**
     * 校验当前登录用户是否为指定角色之一，不满足则抛出无权限异常
     */
    public static Account requireLogin() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null || StrUtil.isBlank(currentUser.getRole())) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_LOGIN);
        }
        return currentUser;
    }

    public static Account requireRole(RoleEnum... roles) {
        return requireRole(ResultCodeEnum.NO_PERMISSION_ERROR, roles);
    }

    /**
     * 校验当前登录用户角色，允许指定不满足时抛出的业务错误码
     */
    public static Account requireRole(ResultCodeEnum errorCode, RoleEnum... roles) {
        Account currentUser = TokenUtils.getCurrentUser();
        String role = currentUser.getRole();
        if (RoleEnum.ADMIN.name().equals(role)) {
            return currentUser;
        }
        for (RoleEnum item : roles) {
            if (item.name().equals(role)) {
                return currentUser;
            }
        }
        throw new BusinessException(errorCode);
    }
}
