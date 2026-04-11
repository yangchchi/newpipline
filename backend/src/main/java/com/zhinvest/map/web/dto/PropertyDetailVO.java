package com.zhinvest.map.web.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PropertyDetailVO {

    private Long id;
    private String name;
    private String city;
    private String region;
    private String status;
    private String mainLayout;
    private BigDecimal avgPrice;
    private String address;
    private LocalDateTime dataUpdateTime;
}
