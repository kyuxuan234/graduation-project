package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class DeployExecutorConfig {

    @Value("${deploy.execution.pool-size:2}")
    private int poolSize;

    @Value("${deploy.execution.queue-capacity:50}")
    private int queueCapacity;

    @Bean(name = "deployExecutor")
    public ThreadPoolTaskExecutor deployExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("deploy-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService deployRetryScheduler() {
        return Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "deploy-retry-");
            thread.setDaemon(true);
            return thread;
        });
    }
}
