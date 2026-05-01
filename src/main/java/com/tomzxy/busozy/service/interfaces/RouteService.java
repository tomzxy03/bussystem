package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.dto.request.RoutePriceReqDTO;
import com.tomzxy.busozy.dto.request.RouteReqDTO;
import com.tomzxy.busozy.dto.request.RouteStopReqDTO;
import com.tomzxy.busozy.dto.response.RouteDetailResDTO;
import com.tomzxy.busozy.dto.response.RouteResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RouteService {

    // Public
    Page<RouteResDTO> searchRoutes(String keyword, Long companyId, Long originStopId, Long destStopId,
            Pageable pageable);

    RouteDetailResDTO getRouteDetail(Long id);

    List<RouteDetailResDTO.RoutePriceResDTO> getRoutePrices(Long id);

}
