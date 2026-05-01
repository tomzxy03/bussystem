package com.tomzxy.busozy.service.impl.admin;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.RoutePriceReqDTO;
import com.tomzxy.busozy.dto.request.RouteReqDTO;
import com.tomzxy.busozy.dto.request.RouteStopReqDTO;
import com.tomzxy.busozy.dto.response.RouteDetailResDTO;
import com.tomzxy.busozy.dto.response.RouteResDTO;
import com.tomzxy.busozy.entity.*;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ConflictException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.RouteMapper;
import com.tomzxy.busozy.repository.*;
import com.tomzxy.busozy.service.interfaces.admin.AdminRouteService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRouteServiceImpl implements AdminRouteService {

    private static final Logger log = LoggerFactory.getLogger(AdminRouteServiceImpl.class);

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final CompanyRepository companyRepository;
    private final RouteMapper routeMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    private static final String KEY_ROUTE = "route:";

    @Override
    @Transactional
    public RouteResDTO createRoute(RouteReqDTO req) {
        if (routeRepository.existsByCode(req.getCode())) {
            throw new ConflictException(ErrorCode.ROUTE_CODE_EXISTS);
        }
        Company company = requireActiveCompany(req.getCompanyId());

        Route route = new Route();
        mapRouteRequest(req, route, company);
        routeRepository.save(route);

        log.info("Route created: id={}, code={}", route.getId(), route.getCode());
        return routeMapper.toRouteRes(route);
    }

    @Override
    @Transactional
    public RouteResDTO updateRoute(Long id, RouteReqDTO req) {
        Route route = requireActiveRoute(id);

        if (!req.getCode().equals(route.getCode()) && routeRepository.existsByCode(req.getCode())) {
            throw new ConflictException(ErrorCode.ROUTE_CODE_EXISTS);
        }
        Company company = requireActiveCompany(req.getCompanyId());
        mapRouteRequest(req, route, company);
        routeRepository.save(route);

        evictRouteCache(id);
        return routeMapper.toRouteRes(route);
    }

    @Override
    @Transactional
    public RouteDetailResDTO replaceStops(Long id, List<RouteStopReqDTO> stopsReq) {
        Route route = requireActiveRoute(id);

        validateStopOrders(stopsReq);

        route.getStops().clear();

        for (RouteStopReqDTO dto : stopsReq) {
            Stop stop = stopRepository.findById(dto.getStopId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STOP_NOT_FOUND));

            RouteStop rs = new RouteStop();
            rs.setRoute(route);
            rs.setStop(stop);
            rs.setStopOrder(dto.getStopOrder());
            rs.setEstimatedMinutesFromOrigin(dto.getEstimatedMinutesFromOrigin());
            rs.setDistanceFromOrigin(dto.getDistanceFromOrigin());
            rs.setIsPickup(dto.getIsPickup() != null ? dto.getIsPickup() : true);
            rs.setIsDropoff(dto.getIsDropoff() != null ? dto.getIsDropoff() : true);
            route.getStops().add(rs);
        }

        routeRepository.save(route);
        evictRouteCache(id);

        return assembleDetail(route);
    }

    @Override
    @Transactional
    public RouteDetailResDTO replacePrices(Long id, List<RoutePriceReqDTO> pricesReq) {
        Route route = requireActiveRoute(id);

        Set<Integer> validOrders = route.getStops().stream()
                .map(RouteStop::getStopOrder)
                .collect(Collectors.toSet());

        for (RoutePriceReqDTO dto : pricesReq) {
            if (dto.getPickupOrder() >= dto.getDropoffOrder()) {
                throw new BusinessException(ErrorCode.INVALID_PRICE_SEGMENT);
            }
            if (!validOrders.contains(dto.getPickupOrder()) || !validOrders.contains(dto.getDropoffOrder())) {
                throw new BusinessException(ErrorCode.ORIGIN_DEST_NOT_IN_ROUTE);
            }
        }

        route.getPrices().clear();

        for (RoutePriceReqDTO dto : pricesReq) {
            RoutePrice rp = new RoutePrice();
            rp.setRoute(route);
            rp.setPickupOrder(dto.getPickupOrder());
            rp.setDropoffOrder(dto.getDropoffOrder());
            rp.setPrice(dto.getPrice());
            rp.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "VND");
            route.getPrices().add(rp);
        }

        routeRepository.save(route);
        evictRouteCache(id);

        return assembleDetail(route);
    }

    @Override
    @Transactional
    public void deleteRoute(Long id) {
        Route route = requireActiveRoute(id);
        route.setDeletedAt(OffsetDateTime.now());
        route.setIsActive(false);
        routeRepository.save(route);
        evictRouteCache(id);
        log.info("Route soft-deleted: id={}", id);
    }

    private Route requireActiveRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND));
    }

    private Company requireActiveCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND));
        if (!Boolean.TRUE.equals(company.getIsActive())) {
            throw new BusinessException(ErrorCode.ROUTE_COMPANY_NOT_ACTIVE);
        }
        return company;
    }

    private void mapRouteRequest(RouteReqDTO req, Route route, Company company) {
        route.setCode(req.getCode());
        route.setName(req.getName());
        route.setDistanceKm(req.getDistanceKm());
        route.setDurationMinutes(req.getDurationMinutes());
        route.setCompany(company);
        route.setIsActive(true);
    }

    private void validateStopOrders(List<RouteStopReqDTO> stops) {
        Set<Integer> orders = new HashSet<>();
        for (RouteStopReqDTO s : stops) {
            if (!orders.add(s.getStopOrder())) {
                throw new BusinessException(ErrorCode.INVALID_STOP_SEQUENCE);
            }
        }
        List<Integer> sorted = orders.stream().sorted().toList();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i) != i + 1) {
                throw new BusinessException(ErrorCode.INVALID_STOP_SEQUENCE);
            }
        }
    }

    private RouteDetailResDTO assembleDetail(Route route) {
        RouteResDTO routeRes = routeMapper.toRouteRes(route);
        List<RouteDetailResDTO.RouteStopResDTO> stops = route.getStops().stream()
                .map(routeMapper::toRouteStopRes)
                .toList();
        List<RouteDetailResDTO.RoutePriceResDTO> prices = route.getPrices().stream()
                .map(routeMapper::toRoutePriceRes)
                .toList();
        return new RouteDetailResDTO(routeRes, stops, prices);
    }

    private void evictRouteCache(Long id) {
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_ROUTE + id);
    }
}
