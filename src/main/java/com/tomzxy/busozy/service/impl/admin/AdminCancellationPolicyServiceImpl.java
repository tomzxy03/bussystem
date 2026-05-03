package com.tomzxy.busozy.service.impl.admin;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.dto.request.CancellationPolicyReqDTO;
import com.tomzxy.busozy.dto.response.CancellationPolicyResDTO;
import com.tomzxy.busozy.entity.CancellationPolicy;
import com.tomzxy.busozy.entity.Company;
import com.tomzxy.busozy.entity.Route;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.repository.CancellationPolicyRepository;
import com.tomzxy.busozy.repository.CompanyRepository;
import com.tomzxy.busozy.repository.RouteRepository;
import com.tomzxy.busozy.service.interfaces.admin.AdminCancellationPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCancellationPolicyServiceImpl implements AdminCancellationPolicyService {

    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final CompanyRepository companyRepository;
    private final RouteRepository routeRepository;

    @Override
    public Page<CancellationPolicyResDTO> getPolicies(Long companyId, Long routeId, Boolean active, Pageable pageable) {
        return cancellationPolicyRepository.searchPolicies(companyId, routeId, active, pageable)
                .map(this::toRes);
    }

    @Override
    @Transactional
    public CancellationPolicyResDTO createPolicy(CancellationPolicyReqDTO req) {
        CancellationPolicy policy = new CancellationPolicy();
        applyReq(policy, req);
        cancellationPolicyRepository.save(policy);
        return toRes(policy);
    }

    @Override
    @Transactional
    public CancellationPolicyResDTO updatePolicy(Long id, CancellationPolicyReqDTO req) {
        CancellationPolicy policy = cancellationPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REFUND_POLICY_NOT_FOUND));
        applyReq(policy, req);
        cancellationPolicyRepository.save(policy);
        return toRes(policy);
    }

    private void applyReq(CancellationPolicy policy, CancellationPolicyReqDTO req) {
        Company company = null;
        Route route = null;

        if (req.getCompanyId() != null) {
            company = companyRepository.findById(req.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND));
        }
        if (req.getRouteId() != null) {
            route = routeRepository.findById(req.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND));
        }
        if (company != null && route != null && !route.getCompany().getId().equals(company.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Route không thuộc company đã chọn");
        }

        policy.setCompany(company);
        policy.setRoute(route);
        policy.setHoursBeforeDeparture(req.getHoursBeforeDeparture());
        policy.setRefundPercentage(req.getRefundPercentage().setScale(2, java.math.RoundingMode.HALF_UP));
        policy.setIsActive(req.getIsActive() == null ? Boolean.TRUE : req.getIsActive());
    }

    private CancellationPolicyResDTO toRes(CancellationPolicy policy) {
        return new CancellationPolicyResDTO(
                policy.getId(),
                policy.getCompany() != null ? policy.getCompany().getId() : null,
                policy.getCompany() != null ? policy.getCompany().getName() : null,
                policy.getRoute() != null ? policy.getRoute().getId() : null,
                policy.getRoute() != null ? policy.getRoute().getName() : null,
                policy.getHoursBeforeDeparture(),
                policy.getRefundPercentage(),
                policy.getIsActive(),
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }
}
