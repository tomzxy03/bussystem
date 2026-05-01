package com.tomzxy.busozy.service.impl;

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
import com.tomzxy.busozy.service.interfaces.RouteService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteServiceImpl.class);

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final RoutePriceRepository routePriceRepository;
    private final StopRepository stopRepository;
    private final CompanyRepository companyRepository;
    private final RouteMapper routeMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    // Redis key: busozy:{env}:route:{id} → RouteDetailResDTO, TTL 2h
    private static final String KEY_ROUTE = "route:";
    private static final Duration TTL_2H = Duration.ofHours(2);

    // ─────────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────────

    @Override
    public Page<RouteResDTO> searchRoutes(String keyword, Long companyId,
            Long originStopId, Long destStopId, Pageable pageable) {
        if (originStopId != null && destStopId != null) {
            return routeRepository.findByOriginAndDestination(originStopId, destStopId, pageable)
                    .map(routeMapper::toRouteRes);
        }
        return routeRepository.searchRoutes(keyword, companyId, pageable)
                .map(routeMapper::toRouteRes);
    }

    @Override
    public RouteDetailResDTO getRouteDetail(Long id) {
        String key = redisConfig.keyPrefix() + KEY_ROUTE + id;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof RouteDetailResDTO dto) {
            log.debug("Cache hit: route {}", id);
            return dto;
        }
        Route route = requireActiveRoute(id);
        RouteDetailResDTO dto = assembleDetail(route);
        redisTemplate.opsForValue().set(key, dto, TTL_2H);
        return dto;
    }

    @Override
    public List<RouteDetailResDTO.RoutePriceResDTO> getRoutePrices(Long id) {
        requireActiveRoute(id);
        return routePriceRepository.findByRouteIdOrderByPickupOrderAscDropoffOrderAsc(id)
                .stream().map(routeMapper::toRoutePriceRes).toList();
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
    
    private Route requireActiveRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND));
    }
}
