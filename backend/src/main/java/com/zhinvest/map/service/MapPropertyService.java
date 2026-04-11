package com.zhinvest.map.service;

import com.zhinvest.map.exception.BusinessException;
import com.zhinvest.map.mapper.ZhPropertyMapper;
import com.zhinvest.map.web.dto.MapBoundsQuery;
import com.zhinvest.map.web.dto.MapPropertyItemVO;
import com.zhinvest.map.web.dto.PropertyDetailVO;
import com.zhinvest.map.web.dto.SearchItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapPropertyService {

    private final ZhPropertyMapper zhPropertyMapper;

    @Transactional(readOnly = true)
    public List<MapPropertyItemVO> listInBounds(MapBoundsQuery query) {
        return zhPropertyMapper.selectByBounds(
                query.getMinLng(),
                query.getMaxLng(),
                query.getMinLat(),
                query.getMaxLat(),
                query.getStatusList()
        );
    }

    @Transactional(readOnly = true)
    public List<SearchItemVO> search(String keyword) {
        return zhPropertyMapper.searchByName(keyword.trim());
    }

    @Transactional(readOnly = true)
    public PropertyDetailVO getDetail(Long id) {
        PropertyDetailVO detail = zhPropertyMapper.selectDetailById(id);
        if (detail == null) {
            throw new BusinessException(404, "楼盘不存在");
        }
        return detail;
    }
}
