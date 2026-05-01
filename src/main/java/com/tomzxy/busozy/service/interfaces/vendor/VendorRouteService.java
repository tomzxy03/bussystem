package com.tomzxy.busozy.service.interfaces.vendor;

import com.tomzxy.busozy.dto.request.RoutePriceReqDTO;
import com.tomzxy.busozy.dto.request.RouteReqDTO;
import com.tomzxy.busozy.dto.request.RouteStopReqDTO;
import com.tomzxy.busozy.dto.response.RouteDetailResDTO;
import com.tomzxy.busozy.dto.response.RouteResDTO;
import com.tomzxy.busozy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VendorRouteService {

    Page<RouteResDTO> searchRoutes(User currentUser, String keyword, Pageable pageable);

    RouteResDTO createRoute(User currentUser, RouteReqDTO req);

    RouteResDTO updateRoute(User currentUser, Long routeId, RouteReqDTO req);

    RouteDetailResDTO replaceRouteStops(User currentUser, Long routeId, List<RouteStopReqDTO> stops);

    RouteDetailResDTO replaceRoutePrices(User currentUser, Long routeId, List<RoutePriceReqDTO> prices);

    void deleteRoute(User currentUser, Long routeId);
}
