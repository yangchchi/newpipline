package com.zhinvest.map.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("zh_property")
public class ZhProperty {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String city;
    private String region;
    private String status;
    private String mainLayout;
    private BigDecimal avgPrice;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private LocalDateTime dataUpdateTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
