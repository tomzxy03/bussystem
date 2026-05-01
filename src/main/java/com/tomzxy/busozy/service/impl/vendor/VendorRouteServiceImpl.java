package com.tomzxy.busozy.service.impl.vendor;

import com.tomzxy.busozy.dto.request.RoutePriceReqDTO;
import com.tomzxy.busozy.dto.request.RouteReqDTO;
import com.tomzxy.busozy.dto.request.RouteStopReqDTO;
import com.tomzxy.busozy.dto.response.RouteDetailResDTO;
import com.tomzxy.busozy.dto.response.RouteResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.impl.AccessScopeService;
import com.tomzxy.busozy.service.interfaces.RouteService;
import com.tomzxy.busozy.service.interfaces.admin.AdminRouteService;
import com.tomzxy.busozy.service.interfaces.vendor.VendorRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorRouteServiceImpl implements VendorRouteService {

    private final RouteService routeService;
    private final AdminRouteService adminRouteService;
    private final AccessScopeService accessScopeService;

    @Override
    public Page<RouteResDTO> searchRoutes(User currentUser, String keyword, Pageable pageable) {
        return routeService.searchRoutes(keyword, accessScopeService.requireVendorCompanyId(currentUser), null, null,
                pageable);
    }

    @Override
    @Transactional
    public RouteResDTO createRoute(User currentUser, RouteReqDTO req) {
        req.setCompanyId(accessScopeService.requireVendorCompanyId(currentUser));
        return adminRouteService.createRoute(req);
    }

    @Override
    @Transactional
    public RouteResDTO updateRoute(User currentUser, Long routeId, RouteReqDTO req) {
        accessScopeService.assertRouteBelongsToVendor(currentUser, routeId);
        req.setCompanyId(accessScopeService.requireVendorCompanyId(currentUser));
        return adminRouteService.updateRoute(routeId, req);
    }

    @Override
    @Transactional
    public RouteDetailResDTO replaceRouteStops(User currentUser, Long routeId, List<RouteStopReqDTO> stops) {
        accessScopeService.assertRouteBelongsToVendor(currentUser, routeId);
        return adminRouteService.replaceStops(routeId, stops);
    }

    @Override
    @Transactional
    public RouteDetailResDTO replaceRoutePrices(User currentUser, Long routeId, List<RoutePriceReqDTO> prices) {
        accessScopeService.assertRouteBelongsToVendor(currentUser, routeId);
        return adminRouteService.replacePrices(routeId, prices);
    }

    @Override
    @Transactional
    public void deleteRoute(User currentUser, Long routeId) {
        accessScopeService.assertRouteBelongsToVendor(currentUser, routeId);
        adminRouteService.deleteRoute(routeId);
    }
}
