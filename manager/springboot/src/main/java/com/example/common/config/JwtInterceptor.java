package com.example.common.config;

import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.AccountStatusEnum;
import com.example.entity.Person;
import com.example.exception.BusinessException;
import com.example.service.PersonService;
import com.example.utils.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jwt拦截器
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Resource
    private PersonService personService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从http请求的header中获取token
        String token = request.getHeader(Constants.TOKEN);
        if (ObjectUtil.isEmpty(token)) {
            // 如果没拿到，从参数里再拿一次
            token = request.getParameter(Constants.TOKEN);
        }
        // 2. 开始执行认证
        if (ObjectUtil.isEmpty(token)) {
            throw new BusinessException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        Person person;
        try {
            String userRole = JWT.decode(token).getAudience().get(0);
            String userId = userRole.split("-")[0];
            String role = userRole.split("-")[1];
            person = personService.selectById(Integer.valueOf(userId));
            if (person != null && !role.equals(person.getRole())) {
                person = null;
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        if (ObjectUtil.isNull(person)) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (AccountStatusEnum.DISABLED.name().equals(person.getStatus())) {
            throw new BusinessException(ResultCodeEnum.USER_DISABLED_ERROR);
        }
        try {
            // 使用配置的 jwt.secret 验签（与 TokenUtils.createToken 一致）
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(TokenUtils.jwtSecret())).build();
            jwtVerifier.verify(token); // 验证token
        } catch (JWTVerificationException e) {
            throw new BusinessException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        return true;
    }
}
