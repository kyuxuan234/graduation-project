package com.example.common.config;

import com.example.common.SshTerminalHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class SshWebSocketConfig implements WebSocketConfigurer {

    @Resource
    private SshTerminalHandler sshTerminalHandler;

    @Resource
    private SshHandshakeInterceptor sshHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sshTerminalHandler, "/ssh/terminal")
                .addInterceptors(sshHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
