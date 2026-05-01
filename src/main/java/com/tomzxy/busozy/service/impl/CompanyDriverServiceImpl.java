package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.CompanyReqDTO;
import com.tomzxy.busozy.dto.request.DriverReqDTO;
import com.tomzxy.busozy.dto.response.CompanyResDTO;
import com.tomzxy.busozy.dto.response.DriverResDTO;
import com.tomzxy.busozy.entity.Company;
import com.tomzxy.busozy.entity.Driver;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ConflictException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.CompanyDriverMapper;
import com.tomzxy.busozy.repository.BusRepository;
import com.tomzxy.busozy.repository.CompanyRepository;
import com.tomzxy.busozy.repository.DriverRepository;
import com.tomzxy.busozy.service.interfaces.CompanyDriverService;
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
public class CompanyDriverServiceImpl implements CompanyDriverService {

    private static final Logger log = LoggerFactory.getLogger(CompanyDriverServiceImpl.class);

    private final BusRepository busRepository;
    private final CompanyRepository companyRepository;
    private final DriverRepository driverRepository;
    private final CompanyDriverMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    // Redis keys: busozy:{env}:company:*
    // private static final String KEY_COMPANY_LIST = "company:list";
    private static final String KEY_COMPANY = "company:";
    private static final String KEY_DRIVERS_SUFFIX = ":drivers";

    private static final Duration TTL_24H = Duration.ofHours(24);
    private static final Duration TTL_4H = Duration.ofHours(4);
    private static final Duration TTL_1H = Duration.ofHours(1);

    // ─────────────────────────────────────────────
    // COMPANY – PUBLIC
    // ─────────────────────────────────────────────

    @Override
    public Page<CompanyResDTO> getCompanies(String keyword, Pageable pageable) {
        // Cache only full list with no keyword (page 0, no filter)
        if ((keyword == null || keyword.isBlank()) && pageable.getPageNumber() == 0) {
            // For simplicity, paginated results are NOT cached — cache individual company
            // entries instead
        }
        return companyRepository.searchActiveCompanies(keyword, pageable)
                .map(mapper::toCompanyRes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompanyResDTO getCompanyById(Long id) {
        String key = redisConfig.keyPrefix() + KEY_COMPANY + id;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof CompanyResDTO dto) {
            log.debug("Cache hit: company {}", id);
            return dto;
        }
        Company company = requireActiveCompany(id);
        CompanyResDTO dto = mapper.toCompanyRes(company);
        redisTemplate.opsForValue().set(key, dto, TTL_4H);
        return dto;
    }

    // ─────────────────────────────────────────────
    // COMPANY – ADMIN
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public CompanyResDTO createCompany(CompanyReqDTO req) {
        if (req.getTaxCode() != null && companyRepository.existsByTaxCode(req.getTaxCode())) {
            throw new ConflictException(ErrorCode.TAX_CODE_EXISTS);
        }
        Company company = new Company();
        mapCompanyRequest(req, company);
        companyRepository.save(company);

        // evictCompanyListCache();
        log.info("Company created: id={}", company.getId());
        return mapper.toCompanyRes(company);
    }

    @Override
    @Transactional
    public CompanyResDTO updateCompany(Long id, CompanyReqDTO req) {
        Company company = requireActiveCompany(id);

        // Tax code unique check (exclude current)
        if (req.getTaxCode() != null && !req.getTaxCode().equals(company.getTaxCode())
                && companyRepository.existsByTaxCode(req.getTaxCode())) {
            throw new ConflictException(ErrorCode.TAX_CODE_EXISTS);
        }

        mapCompanyRequest(req, company);
        companyRepository.save(company);

        evictCompanyCaches(id);
        return mapper.toCompanyRes(company);
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {
        Company company = requireActiveCompany(id);

        // Critical business rule: cannot delete if company has active buses (Phase 5+)
        if (busRepository.existsByCompanyIdAndIsActiveTrue(id)) {
            throw new BusinessException(ErrorCode.COMPANY_HAS_ACTIVE_BUSES);
        }

        company.setDeletedAt(OffsetDateTime.now());
        company.setIsActive(false);
        companyRepository.save(company);

        evictCompanyCaches(id);
        log.info("Company soft-deleted: id={}", id);
    }

    // ─────────────────────────────────────────────
    // DRIVER – ADMIN
    // ─────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<DriverResDTO> getDriversByCompany(Long companyId) {
        requireActiveCompany(companyId);
        String key = redisConfig.keyPrefix() + KEY_COMPANY + companyId + KEY_DRIVERS_SUFFIX;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?> list) {
            log.debug("Cache hit: drivers of company {}", companyId);
            return (List<DriverResDTO>) list;
        }
        List<DriverResDTO> result = driverRepository.findByCompanyIdOrderByFullNameAsc(companyId)
                .stream().map(mapper::toDriverRes).toList();
        redisTemplate.opsForValue().set(key, result, TTL_1H);
        return result;
    }

    @Override
    @Transactional
    public DriverResDTO createDriver(DriverReqDTO req) {
        Company company = requireActiveCompany(req.getCompanyId());

        // Unique per company checks
        if (driverRepository.existsByCompanyIdAndLicenseNumber(req.getCompanyId(), req.getLicenseNumber())) {
            throw new ConflictException(ErrorCode.LICENSE_EXISTS_IN_COMPANY);
        }
        if (driverRepository.existsByCompanyIdAndPhone(req.getCompanyId(), req.getPhone())) {
            throw new ConflictException(ErrorCode.PHONE_EXISTS_IN_COMPANY);
        }

        Driver driver = new Driver();
        mapDriverRequest(req, driver, company);
        driverRepository.save(driver);

        evictDriversCache(req.getCompanyId());
        log.info("Driver created: id={}, company={}", driver.getId(), req.getCompanyId());
        return mapper.toDriverRes(driver);
    }

    @Override
    @Transactional
    public DriverResDTO updateDriver(Long id, DriverReqDTO req) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DRIVER_NOT_FOUND));

        Company company = requireActiveCompany(req.getCompanyId());

        // Unique per company checks (exclude current driver)
        if (!req.getLicenseNumber().equals(driver.getLicenseNumber())
                && driverRepository.existsByCompanyIdAndLicenseNumber(req.getCompanyId(), req.getLicenseNumber())) {
            throw new ConflictException(ErrorCode.LICENSE_EXISTS_IN_COMPANY);
        }
        if (!req.getPhone().equals(driver.getPhone())
                && driverRepository.existsByCompanyIdAndPhone(req.getCompanyId(), req.getPhone())) {
            throw new ConflictException(ErrorCode.PHONE_EXISTS_IN_COMPANY);
        }

        Long oldCompanyId = driver.getCompany().getId();
        mapDriverRequest(req, driver, company);
        driverRepository.save(driver);

        evictDriversCache(oldCompanyId);
        if (!oldCompanyId.equals(req.getCompanyId())) {
            evictDriversCache(req.getCompanyId());
        }
        return mapper.toDriverRes(driver);
    }

    @Override
    @Transactional
    public DriverResDTO updateDriverStatus(Long id, UserStatus status) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DRIVER_NOT_FOUND));
        driver.setStatus(status);
        driverRepository.save(driver);
        evictDriversCache(driver.getCompany().getId());
        return mapper.toDriverRes(driver);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private Company requireActiveCompany(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND));
        if (!Boolean.TRUE.equals(company.getIsActive())) {
            throw new BusinessException(ErrorCode.COMPANY_NOT_ACTIVE);
        }
        return company;
    }

    private void mapCompanyRequest(CompanyReqDTO req, Company company) {
        company.setName(req.getName());
        company.setTaxCode(req.getTaxCode());
        company.setPhone(req.getPhone());
        company.setAddress(req.getAddress());
        company.setIsActive(true);
    }

    private void mapDriverRequest(DriverReqDTO req, Driver driver, Company company) {
        driver.setCompany(company);
        driver.setFullName(req.getFullName());
        driver.setPhone(req.getPhone());
        driver.setLicenseNumber(req.getLicenseNumber());
        driver.setAvatarUrl(req.getAvatarUrl());
        driver.setDateOfBirth(req.getDateOfBirth());
        driver.setAddress(req.getAddress());
        if (driver.getStatus() == null) {
            driver.setStatus(UserStatus.ACTIVE);
        }
    }

    // private void evictCompanyListCache() {
    // redisTemplate.delete(redisConfig.keyPrefix() + KEY_COMPANY_LIST);
    // }

    private void evictCompanyCaches(Long companyId) {
        // evictCompanyListCache();
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_COMPANY + companyId);
    }

    private void evictDriversCache(Long companyId) {
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_COMPANY + companyId + KEY_DRIVERS_SUFFIX);
    }
}
