package com.zhinvest.map;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhinvest.map.mapper")
public class MapZhPropertyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MapZhPropertyApplication.class, args);
    }
}
