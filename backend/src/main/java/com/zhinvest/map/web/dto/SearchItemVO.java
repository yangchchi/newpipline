package com.zhinvest.map.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SearchItemVO {

    private Long id;
    private String name;
    private String region;
    private BigDecimal longitude;
    private BigDecimal latitude;
}
