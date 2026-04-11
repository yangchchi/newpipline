package com.zhinvest.map.web;

import com.zhinvest.map.common.ApiResponse;
import com.zhinvest.map.service.MapPropertyService;
import com.zhinvest.map.web.dto.MapBoundsQuery;
import com.zhinvest.map.web.dto.MapPropertyItemVO;
import com.zhinvest.map.web.dto.PropertyDetailVO;
import com.zhinvest.map.web.dto.SearchItemVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/map")
@Validated
@RequiredArgsConstructor
public class MapPropertyController {

    private final MapPropertyService mapPropertyService;

    @GetMapping("/properties")
    public ApiResponse<List<MapPropertyItemVO>> listProperties(@Valid @ModelAttribute MapBoundsQuery query) {
        return ApiResponse.ok(mapPropertyService.listInBounds(query));
    }

    @GetMapping("/properties/search")
    public ApiResponse<List<SearchItemVO>> search(
            @RequestParam @NotBlank(message = "关键词不能为空") String keyword
    ) {
        return ApiResponse.ok(mapPropertyService.search(keyword));
    }

    @GetMapping("/properties/{id}")
    public ApiResponse<PropertyDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(mapPropertyService.getDetail(id));
    }
}
