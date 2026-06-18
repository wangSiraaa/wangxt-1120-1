package com.semiconductor.mask;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.semiconductor.mask.mapper")
public class MaskBorrowApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaskBorrowApplication.class, args);
    }
}
