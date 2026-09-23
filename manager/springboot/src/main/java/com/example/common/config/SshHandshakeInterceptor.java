package com.example.common.config;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.utils.TokenUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * SSH 窗口只给运维和超级管理员，并且必须挂在一张部署单上。
 */
@Component
public class SshHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = null;
        if (request instanceof ServletServerHttpRequest) {
            token = ((ServletServerHttpRequest) request).getServletRequest().getParameter("token");
        }
        if (StrUtil.isBlank(token)) {
            token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        }
        Account account = TokenUtils.verifyToken(token);
        if (account == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        String role = account.getRole();
        if (!RoleEnum.OPS.name().equals(role) && !RoleEnum.ADMIN.name().equals(role)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        String deployId = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("deployId");
        if (StrUtil.isBlank(deployId)) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        try {
            attributes.put("deployId", Integer.valueOf(deployId));
        } catch (NumberFormatException e) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        attributes.put("username", account.getUsername());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
