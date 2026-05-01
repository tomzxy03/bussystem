package com.tomzxy.busozy.service.interfaces.admin;

import com.tomzxy.busozy.dto.request.RoutePriceReqDTO;
import com.tomzxy.busozy.dto.request.RouteReqDTO;
import com.tomzxy.busozy.dto.request.RouteStopReqDTO;
import com.tomzxy.busozy.dto.response.RouteDetailResDTO;
import com.tomzxy.busozy.dto.response.RouteResDTO;

import java.util.List;

public interface AdminRouteService {
    RouteResDTO createRoute(RouteReqDTO req);

    RouteResDTO updateRoute(Long id, RouteReqDTO req);

    RouteDetailResDTO replaceStops(Long id, List<RouteStopReqDTO> stops);

    RouteDetailResDTO replacePrices(Long id, List<RoutePriceReqDTO> prices);

    void deleteRoute(Long id);
}
