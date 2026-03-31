package com.example.javarag;

import com.example.javarag.config.RagProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RagProperties.class)
@MapperScan("com.example.javarag.mapper")
public class JavaRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaRagApplication.class, args);
    }
}
