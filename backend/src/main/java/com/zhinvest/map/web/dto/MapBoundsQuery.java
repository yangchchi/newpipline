package com.zhinvest.map.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MapBoundsQuery {

    @NotNull(message = "minLng 不能为空")
    private BigDecimal minLng;

    @NotNull(message = "maxLng 不能为空")
    private BigDecimal maxLng;

    @NotNull(message = "minLat 不能为空")
    private BigDecimal minLat;

    @NotNull(message = "maxLat 不能为空")
    private BigDecimal maxLat;

    private List<String> statusList;
}
