package com.example.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.common.Constants;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.service.AdminService;
import com.example.service.DevService;
import com.example.service.OpsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Token工具类
 */
@Component
public class TokenUtils {

    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    private static AdminService staticAdminService;
    private static DevService staticDevService;
    private static OpsService staticOpsService;
    private static final ThreadLocal<Account> BOUND_USER = new ThreadLocal<Account>();

    /**
     * 单测绑定当前用户。未绑定时仍从请求头里的 token 解析。
     */
    public static void bindCurrentUser(Account account) {
        BOUND_USER.set(account);
    }

    public static void unbindCurrentUser() {
        BOUND_USER.remove();
    }

    @Resource
    AdminService adminService;

    @Resource
    DevService devService;

    @Resource
    OpsService opsService;

    @PostConstruct
    public void setUserService() {
        staticAdminService = adminService;
        staticDevService = devService;
        staticOpsService = opsService;
    }

    /**
     * 生成token
     */
    public static String createToken(String data, String sign) {
        return JWT.create().withAudience(data) // 将 userId-role 保存到 token 里面,作为载荷
                .withExpiresAt(DateUtil.offsetHour(new Date(), 2)) // 2小时后token过期
                .sign(Algorithm.HMAC256(sign)); // 以 password 作为 token 的密钥
    }

    /**
     * 获取当前登录的用户信息
     */
    public static Account getCurrentUser() {
        Account bound = BOUND_USER.get();
        if (bound != null) {
            return bound;
        }
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader(Constants.TOKEN);
            if (ObjectUtil.isNotEmpty(token)) {
                String userRole = JWT.decode(token).getAudience().get(0);
                String userId = userRole.split("-")[0];  // 获取用户id
                String role = userRole.split("-")[1];    // 获取角色
                if (RoleEnum.ADMIN.name().equals(role)) {
                    return staticAdminService.selectById(Integer.valueOf(userId));
                } else if (RoleEnum.DEV.name().equals(role)) {
                    return staticDevService.selectById(Integer.valueOf(userId));
                } else if (RoleEnum.OPS.name().equals(role)) {
                    return staticOpsService.selectById(Integer.valueOf(userId));
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户信息出错", e);
        }
        return new Account();  // 返回空的账号对象
    }
}
