package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.BusStatus;
import com.tomzxy.busozy.common.enums.SeatType;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.BusReqDTO;
import com.tomzxy.busozy.dto.request.BusTypeReqDTO;
import com.tomzxy.busozy.dto.request.SeatReqDTO;
import com.tomzxy.busozy.dto.response.BusResDTO;
import com.tomzxy.busozy.dto.response.BusTypeResDTO;
import com.tomzxy.busozy.dto.response.SeatLayoutResDTO;
import com.tomzxy.busozy.dto.response.SeatResDTO;
import com.tomzxy.busozy.entity.*;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ConflictException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.BusMapper;
import com.tomzxy.busozy.repository.*;
import com.tomzxy.busozy.service.interfaces.BusService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BusServiceImpl implements BusService {

    private static final Logger log = LoggerFactory.getLogger(BusServiceImpl.class);

    private final BusTypeRepository busTypeRepository;
    private final SeatLayoutRepository seatLayoutRepository;
    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final CompanyRepository companyRepository;
    private final BusMapper busMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    // Redis keys: busozy:{env}:bus-types | busozy:{env}:bus:{id}:seats
    private static final String KEY_BUS_TYPES = "bus-types";
    private static final String KEY_BUS_SEATS = "bus:";
    private static final String KEY_SEATS_SUFFIX = ":seats";

    private static final Duration TTL_24H = Duration.ofHours(24);
    private static final Duration TTL_2H = Duration.ofHours(2);

    // ─────────────────────────────────────────────
    // BUS TYPES – PUBLIC
    // ─────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<BusTypeResDTO> getAllBusTypes() {
        String key = redisConfig.keyPrefix() + KEY_BUS_TYPES;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?> list) {
            log.debug("Cache hit: bus-types");
            return (List<BusTypeResDTO>) list;
        }
        List<BusTypeResDTO> result = busTypeRepository.findByIsActiveTrueOrderByNameAsc()
                .stream().map(busMapper::toBusTypeRes).toList();
        redisTemplate.opsForValue().set(key, result, TTL_24H);
        return result;
    }

    @Override
    public BusTypeResDTO getBusTypeById(Long id) {
        BusType bt = busTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUS_TYPE_NOT_FOUND));
        return busMapper.toBusTypeRes(bt);
    }

    // ─────────────────────────────────────────────
    // BUS TYPES – ADMIN
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public BusTypeResDTO createBusType(BusTypeReqDTO req) {
        if (busTypeRepository.existsByCode(req.getCode())) {
            throw new ConflictException(ErrorCode.ROUTE_CODE_EXISTS); // reuse generic conflict
        }
        BusType bt = new BusType();
        bt.setCode(req.getCode());
        bt.setName(req.getName());
        bt.setDescription(req.getDescription());
        bt.setAmenities(req.getAmenities());
        bt.setBasePricePerKm(req.getBasePricePerKm() != null ? req.getBasePricePerKm() : BigDecimal.ZERO);
        bt.setIsActive(true);
        busTypeRepository.save(bt);

        // Invalidate bus-types cache
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_BUS_TYPES);
        return busMapper.toBusTypeRes(bt);
    }

    // ─────────────────────────────────────────────
    // SEAT LAYOUTS – ADMIN
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public SeatLayoutResDTO createSeatLayout(Long busTypeId, String name, Map<String, Object> layoutData) {
        BusType busType = busTypeRepository.findById(busTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUS_TYPE_NOT_FOUND));

        // Validate layout structure: must have "seats" array
        if (!layoutData.containsKey("seats") || !(layoutData.get("seats") instanceof List)) {
            throw new BusinessException(ErrorCode.INVALID_SEAT_LAYOUT);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> seatsData = (List<Map<String, Object>>) layoutData.get("seats");
        int totalSeats = seatsData.size();

        SeatLayout layout = new SeatLayout();
        layout.setBusType(busType);
        layout.setName(name);
        layout.setLayoutData(layoutData);
        layout.setTotalSeats(totalSeats);
        seatLayoutRepository.save(layout);

        return busMapper.toSeatLayoutRes(layout);
    }

    // ─────────────────────────────────────────────
    // BUSES – ADMIN
    // ─────────────────────────────────────────────

    @Override
    public Page<BusResDTO> searchBuses(Long companyId, BusStatus status, String keyword, Pageable pageable) {
        return busRepository.searchBuses(companyId, status, keyword, pageable)
                .map(busMapper::toBusRes);
    }

    @Override
    @Transactional
    public BusResDTO createBus(BusReqDTO req) {
        if (busRepository.existsByLicensePlate(req.getLicensePlate())) {
            throw new ConflictException(ErrorCode.LICENSE_PLATE_EXISTS);
        }

        Company company = companyRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND));
        if (!Boolean.TRUE.equals(company.getIsActive())) {
            throw new BusinessException(ErrorCode.COMPANY_NOT_ACTIVE);
        }

        BusType busType = busTypeRepository.findById(req.getBusTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUS_TYPE_NOT_FOUND));

        Bus bus = new Bus();
        mapBusRequest(req, bus, company, busType);

        // Auto-generate seats from layout if provided
        if (req.getSeatLayoutId() != null) {
            SeatLayout layout = seatLayoutRepository.findById(req.getSeatLayoutId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SEAT_LAYOUT_NOT_FOUND));

            // Validate layout belongs to chosen bus type
            if (!layout.getBusType().getId().equals(busType.getId())) {
                throw new BusinessException(ErrorCode.SEAT_LAYOUT_MISMATCH);
            }
            bus.setSeatLayout(layout);
            generateSeatsFromLayout(bus, layout);
        }

        busRepository.save(bus);
        log.info("Bus created: id={}, plate={}", bus.getId(), bus.getLicensePlate());
        return busMapper.toBusRes(bus);
    }

    @Override
    @Transactional
    public BusResDTO updateBus(Long id, BusReqDTO req) {
        Bus bus = requireBus(id);

        // License plate uniqueness check (exclude current)
        if (!req.getLicensePlate().equals(bus.getLicensePlate())
                && busRepository.existsByLicensePlate(req.getLicensePlate())) {
            throw new ConflictException(ErrorCode.LICENSE_PLATE_EXISTS);
        }

        Company company = companyRepository.findById(req.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND));
        BusType busType = busTypeRepository.findById(req.getBusTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUS_TYPE_NOT_FOUND));

        mapBusRequest(req, bus, company, busType);

        // Update layout if changed
        if (req.getSeatLayoutId() != null) {
            SeatLayout layout = seatLayoutRepository.findById(req.getSeatLayoutId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SEAT_LAYOUT_NOT_FOUND));
            if (!layout.getBusType().getId().equals(busType.getId())) {
                throw new BusinessException(ErrorCode.SEAT_LAYOUT_MISMATCH);
            }
            bus.setSeatLayout(layout);
        } else {
            bus.setSeatLayout(null);
        }

        busRepository.save(bus);
        evictSeatsCache(id);
        return busMapper.toBusRes(bus);
    }

    @Override
    @Transactional
    public BusResDTO updateBusStatus(Long id, BusStatus status) {
        Bus bus = requireBus(id);
        bus.setStatus(status);
        busRepository.save(bus);
        return busMapper.toBusRes(bus);
    }

    // ─────────────────────────────────────────────
    // SEATS
    // ─────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<SeatResDTO> getBusSeats(Long busId) {
        requireBus(busId);
        String key = redisConfig.keyPrefix() + KEY_BUS_SEATS + busId + KEY_SEATS_SUFFIX;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?> list) {
            log.debug("Cache hit: seats of bus {}", busId);
            return (List<SeatResDTO>) list;
        }
        List<SeatResDTO> result = seatRepository
                .findByBusIdAndIsActiveTrueOrderByRowNumAscColNumAsc(busId)
                .stream().map(busMapper::toSeatRes).toList();
        redisTemplate.opsForValue().set(key, result, TTL_2H);
        return result;
    }

    @Override
    @Transactional
    public List<SeatResDTO> initOrReplaceSeats(Long busId, List<SeatReqDTO> seatsReq) {
        Bus bus = requireBus(busId);

        // Validate unique seat numbers in request
        Set<String> numbers = new HashSet<>();
        for (SeatReqDTO dto : seatsReq) {
            if (!numbers.add(dto.getSeatNumber())) {
                throw new BusinessException(ErrorCode.SEAT_NUMBER_DUPLICATE);
            }
        }

        // Replace-all via orphanRemoval
        bus.getSeats().clear();

        for (SeatReqDTO dto : seatsReq) {
            Seat seat = new Seat();
            seat.setBus(bus);
            seat.setSeatNumber(dto.getSeatNumber());
            seat.setSeatType(dto.getSeatType() != null ? dto.getSeatType() : SeatType.STANDARD);
            seat.setRowNum(dto.getRowNum());
            seat.setColNum(dto.getColNum());
            seat.setPriceMultiplier(dto.getPriceMultiplier() != null ? dto.getPriceMultiplier() : BigDecimal.ONE);
            seat.setIsActive(true);
            bus.getSeats().add(seat);
        }

        busRepository.save(bus);
        evictSeatsCache(busId);

        return bus.getSeats().stream().map(busMapper::toSeatRes).toList();
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private Bus requireBus(Long id) {
        return busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUS_NOT_FOUND));
    }

    private void mapBusRequest(BusReqDTO req, Bus bus, Company company, BusType busType) {
        bus.setCompany(company);
        bus.setBusType(busType);
        bus.setLicensePlate(req.getLicensePlate());
        bus.setBusNumber(req.getBusNumber());
        bus.setName(req.getName());
        bus.setStatus(req.getStatus() != null ? req.getStatus() : BusStatus.ACTIVE);
    }

    /**
     * Auto-generates Seat entities from SeatLayout JSONB data.
     * Expected format: {"seats": [{"row": 1, "col": 1, "type": "standard"}, ...]}
     */
    @SuppressWarnings("unchecked")
    private void generateSeatsFromLayout(Bus bus, SeatLayout layout) {
        Object seatsObj = layout.getLayoutData().get("seats");
        if (!(seatsObj instanceof List)) {
            throw new BusinessException(ErrorCode.INVALID_SEAT_LAYOUT);
        }
        List<Map<String, Object>> seatsData = (List<Map<String, Object>>) seatsObj;

        bus.getSeats().clear();
        List<Seat> generated = new ArrayList<>();

        for (Map<String, Object> seatDef : seatsData) {
            int row = ((Number) seatDef.getOrDefault("row", 1)).intValue();
            int col = ((Number) seatDef.getOrDefault("col", 1)).intValue();
            String typeStr = (String) seatDef.getOrDefault("type", "standard");

            Seat seat = new Seat();
            seat.setBus(bus);
            seat.setSeatNumber(row + String.valueOf((char) ('A' + col - 1))); // e.g., "1A", "2B"
            seat.setSeatType(parseSeatType(typeStr));
            seat.setRowNum(row);
            seat.setColNum(col);
            seat.setPriceMultiplier(BigDecimal.ONE);
            seat.setIsActive(true);
            generated.add(seat);
        }
        bus.getSeats().addAll(generated);
    }

    private SeatType parseSeatType(String type) {
        return switch (type.toLowerCase()) {
            case "vip" -> SeatType.VIP;
            case "sleeper" -> SeatType.SLEEPER;
            case "extra" -> SeatType.EXTRA;
            case "bed" -> SeatType.BED;
            default -> SeatType.STANDARD;
        };
    }

    private void evictSeatsCache(Long busId) {
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_BUS_SEATS + busId + KEY_SEATS_SUFFIX);
    }
}
