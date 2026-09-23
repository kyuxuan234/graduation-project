package com.example.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.common.Constants;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.service.AdminService;
import com.example.service.DevService;
import com.example.service.OpsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private static String staticJwtSecret;
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

    @Value("${jwt.secret:devops-jwt-local-secret-2026}")
    private String jwtSecret;

    @PostConstruct
    public void setUserService() {
        staticAdminService = adminService;
        staticDevService = devService;
        staticOpsService = opsService;
        staticJwtSecret = jwtSecret;
    }

    /**
     * JWT 签名密钥（配置项 jwt.secret，不再使用用户密码哈希）
     */
    public static String jwtSecret() {
        return staticJwtSecret;
    }

    /**
     * 生成token。sign 参数保留兼容旧调用，实际签名使用 jwt.secret。
     */
    public static String createToken(String data, String sign) {
        return JWT.create().withAudience(data) // 将 userId-role 保存到 token 里面,作为载荷
                .withExpiresAt(DateUtil.offsetHour(new Date(), 2)) // 2小时后token过期
                .sign(Algorithm.HMAC256(staticJwtSecret));
    }

    /**
     * 校验 token 并查出账号。握手请求不在业务线程里，调用方自己传入 token。
     */
    public static Account verifyToken(String token) {
        if (ObjectUtil.isEmpty(token) || staticAdminService == null) {
            return null;
        }
        try {
            String userRole = JWT.decode(token).getAudience().get(0);
            String userId = userRole.split("-")[0];
            String role = userRole.split("-")[1];
            Account account = null;
            if (RoleEnum.ADMIN.name().equals(role)) {
                account = staticAdminService.selectById(Integer.valueOf(userId));
            } else if (RoleEnum.DEV.name().equals(role)) {
                account = staticDevService.selectById(Integer.valueOf(userId));
            } else if (RoleEnum.OPS.name().equals(role)) {
                account = staticOpsService.selectById(Integer.valueOf(userId));
            }
            if (account == null) {
                return null;
            }
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(staticJwtSecret)).build();
            jwtVerifier.verify(token);
            return account;
        } catch (JWTVerificationException e) {
            return null;
        } catch (Exception e) {
            log.error("校验 token 出错", e);
            return null;
        }
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
