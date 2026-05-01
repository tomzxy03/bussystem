package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.StopType;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.StopReqDTO;
import com.tomzxy.busozy.dto.response.DistrictResDTO;
import com.tomzxy.busozy.dto.response.ProvinceResDTO;
import com.tomzxy.busozy.dto.response.StopResDTO;
import com.tomzxy.busozy.entity.District;
import com.tomzxy.busozy.entity.Province;
import com.tomzxy.busozy.entity.Stop;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ConflictException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.LocationMapper;
import com.tomzxy.busozy.repository.DistrictRepository;
import com.tomzxy.busozy.repository.ProvinceRepository;
import com.tomzxy.busozy.repository.StopRepository;
import com.tomzxy.busozy.service.interfaces.LocationService;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationServiceImpl.class);

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final StopRepository stopRepository;
    private final LocationMapper locationMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    // Redis key patterns (spec: busozy:{env}:location:...)
    private static final String KEY_PROVINCES = "location:provinces";
    private static final String KEY_DISTRICTS = "location:districts:";
    private static final String KEY_STOP = "location:stop:";

    private static final Duration TTL_24H = Duration.ofHours(24);
    private static final Duration TTL_1H = Duration.ofHours(1);

    // ─────────────────────────────────────────────
    // PROVINCES
    // ─────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<ProvinceResDTO> getProvinces(String keyword) {
        // Cache only when no keyword filter (list all)
        if (keyword == null || keyword.isBlank()) {
            String key = redisConfig.keyPrefix() + KEY_PROVINCES;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List<?> list) {
                log.debug("Cache hit: provinces list");
                return (List<ProvinceResDTO>) list;
            }
            List<ProvinceResDTO> result = provinceRepository.findAll().stream()
                    .map(locationMapper::toProvinceRes)
                    .toList();
            redisTemplate.opsForValue().set(key, result, TTL_24H);
            return result;
        }
        // Keyword search – do not cache paginated results
        return provinceRepository.searchByKeyword(keyword).stream()
                .map(locationMapper::toProvinceRes)
                .toList();
    }

    // ─────────────────────────────────────────────
    // DISTRICTS
    // ─────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<DistrictResDTO> getDistrictsByProvince(Long provinceId) {
        validateProvinceExists(provinceId);
        String key = redisConfig.keyPrefix() + KEY_DISTRICTS + provinceId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?> list) {
            log.debug("Cache hit: districts for province {}", provinceId);
            return (List<DistrictResDTO>) list;
        }
        List<DistrictResDTO> result = districtRepository.findByProvinceIdOrderByNameAsc(provinceId).stream()
                .map(locationMapper::toDistrictRes)
                .toList();
        redisTemplate.opsForValue().set(key, result, TTL_24H);
        return result;
    }

    // ─────────────────────────────────────────────
    // STOPS – public
    // ─────────────────────────────────────────────

    @Override
    public Page<StopResDTO> searchStops(Long provinceId, Long districtId, StopType type,
            Boolean isMajor, String keyword, Pageable pageable) {
        return stopRepository.searchStops(provinceId, districtId, type, isMajor, keyword, pageable)
                .map(locationMapper::toStopRes);
    }

    @Override
    public StopResDTO getStopById(Long id) {
        String key = redisConfig.keyPrefix() + KEY_STOP + id;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof StopResDTO dto) {
            log.debug("Cache hit: stop {}", id);
            return dto;
        }
        Stop stop = stopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STOP_NOT_FOUND));
        StopResDTO dto = locationMapper.toStopRes(stop);
        redisTemplate.opsForValue().set(key, dto, TTL_1H);
        return dto;
    }

    // ─────────────────────────────────────────────
    // STOPS – admin CRUD
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public StopResDTO createStop(StopReqDTO req, String idempotencyKey) {
        // Validate province & district consistency
        Province province = validateProvinceExists(req.getProvinceId());
        District district = validateDistrictBelongsToProvince(req.getDistrictId(), req.getProvinceId());

        // Unique code check (partial index handles DB level, but guard here too)
        if (req.getCode() != null && stopRepository.existsByCode(req.getCode())) {
            throw new ConflictException(ErrorCode.STOP_CODE_EXISTS);
        }

        Stop stop = new Stop();
        mapRequestToStop(req, stop, province, district);
        stopRepository.save(stop);

        StopResDTO result = locationMapper.toStopRes(stop);

        // Invalidate province/district cache if needed (stop added)
        evictStopRelatedCaches(stop, null);

        log.info("Stop created: id={}, code={}", stop.getId(), stop.getCode());
        return result;
    }

    @Override
    @Transactional
    public StopResDTO updateStop(Long id, StopReqDTO req) {
        Stop existing = stopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STOP_NOT_FOUND));

        Province province = validateProvinceExists(req.getProvinceId());
        District district = validateDistrictBelongsToProvince(req.getDistrictId(), req.getProvinceId());

        // Unique code check (exclude current stop)
        if (req.getCode() != null && !req.getCode().equals(existing.getCode())
                && stopRepository.existsByCode(req.getCode())) {
            throw new ConflictException(ErrorCode.STOP_CODE_EXISTS);
        }

        Long oldProvinceId = existing.getProvince() != null ? existing.getProvince().getId() : null;
        Long oldDistrictId = existing.getDistrict() != null ? existing.getDistrict().getId() : null;

        mapRequestToStop(req, existing, province, district);
        stopRepository.save(existing);

        // Invalidate stop-level cache + old/new province-district caches
        evictStopCache(id);
        evictLocationListCaches(oldProvinceId, oldDistrictId);
        evictLocationListCaches(req.getProvinceId(), req.getDistrictId());

        return locationMapper.toStopRes(existing);
    }

    @Override
    @Transactional
    public void deleteStop(Long id) {
        Stop stop = stopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STOP_NOT_FOUND));

        Long provinceId = stop.getProvince() != null ? stop.getProvince().getId() : null;
        Long districtId = stop.getDistrict() != null ? stop.getDistrict().getId() : null;

        // Soft delete via BaseEntity deletedAt
        stop.setDeletedAt(OffsetDateTime.now());
        stop.setIsActive(false);
        stopRepository.save(stop);

        evictStopCache(id);
        evictLocationListCaches(provinceId, districtId);
        log.info("Stop soft-deleted: id={}", id);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private Province validateProvinceExists(Long provinceId) {
        return provinceRepository.findById(provinceId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROVINCE_NOT_FOUND));
    }

    private District validateDistrictBelongsToProvince(Long districtId, Long provinceId) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DISTRICT_NOT_FOUND));
        if (!district.getProvince().getId().equals(provinceId)) {
            throw new BusinessException(ErrorCode.DISTRICT_PROVINCE_MISMATCH);
        }
        return district;
    }

    private void mapRequestToStop(StopReqDTO req, Stop stop, Province province, District district) {
        stop.setCode(req.getCode());
        stop.setName(req.getName());
        stop.setType(req.getType());
        stop.setProvince(province);
        stop.setDistrict(district);
        stop.setAddress(req.getAddress());
        stop.setLatitude(req.getLatitude());
        stop.setLongitude(req.getLongitude());
        stop.setIsMajor(req.getIsMajor() != null ? req.getIsMajor() : false);
        stop.setIsActive(true);
    }

    private void evictStopCache(Long stopId) {
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_STOP + stopId);
    }

    private void evictLocationListCaches(Long provinceId, Long districtId) {
        if (provinceId != null) {
            redisTemplate.delete(redisConfig.keyPrefix() + KEY_DISTRICTS + provinceId);
        }
        if (districtId != null) {
            // Province list cache is stable; only evict if needed
            redisTemplate.delete(redisConfig.keyPrefix() + KEY_PROVINCES);
        }
    }

    private void evictStopRelatedCaches(Stop stop, Long oldProvinceId) {
        if (stop.getId() != null) {
            evictStopCache(stop.getId());
        }
    }
}
