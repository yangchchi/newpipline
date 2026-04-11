package com.zhinvest.map.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhinvest.map.domain.ZhProperty;
import com.zhinvest.map.web.dto.MapPropertyItemVO;
import com.zhinvest.map.web.dto.PropertyDetailVO;
import com.zhinvest.map.web.dto.SearchItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ZhPropertyMapper extends BaseMapper<ZhProperty> {

    List<MapPropertyItemVO> selectByBounds(
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("statusList") List<String> statusList
    );

    List<SearchItemVO> searchByName(@Param("keyword") String keyword);

    PropertyDetailVO selectDetailById(@Param("id") Long id);
}
