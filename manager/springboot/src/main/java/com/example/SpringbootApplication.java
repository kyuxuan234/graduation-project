package com.example;

import com.example.common.EnvFileLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.mapper")
public class SpringbootApplication {

    public static void main(String[] args) {
        EnvFileLoader.load();
        SpringApplication.run(SpringbootApplication.class, args);
    }

}
