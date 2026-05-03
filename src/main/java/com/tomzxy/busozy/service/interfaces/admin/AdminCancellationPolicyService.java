package com.tomzxy.busozy.service.interfaces.admin;

import com.tomzxy.busozy.dto.request.CancellationPolicyReqDTO;
import com.tomzxy.busozy.dto.response.CancellationPolicyResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCancellationPolicyService {

    Page<CancellationPolicyResDTO> getPolicies(Long companyId, Long routeId, Boolean active, Pageable pageable);

    CancellationPolicyResDTO createPolicy(CancellationPolicyReqDTO req);

    CancellationPolicyResDTO updatePolicy(Long id, CancellationPolicyReqDTO req);
}
