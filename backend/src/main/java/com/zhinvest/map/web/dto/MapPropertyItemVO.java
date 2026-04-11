package com.zhinvest.map.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MapPropertyItemVO {

    private Long id;
    private String name;
    private String status;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String region;
}
